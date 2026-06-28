package com.aaron.sidegesture.utils.update

import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.entity.global.UpdateState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.first

/**
 * 检查更新逻辑（进程无关）：拉取 GitHub release → 写入跨进程缓存 [UpdateState]。
 *
 * 主进程启动 / 关于页手动检查 / `:service` 后台 ticker 都走这里，结果统一进缓存，
 * 由各自的呈现层（[com.aaron.sidegesture.ui.update.UpdateVM] / 通知）读取。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/19
 */
object UpdateRepository {

    /** 后台懒触发间隔：距上次「成功」检查超过 24h 才需要再查。 */
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    /** 软限流：距上次「尝试」不足 60s 且非强制，跳过本次（仅防瞬时重复请求，存在双进程窗口已裁定接受）。 */
    private const val ATTEMPT_TTL_MS = 60 * 1000L

    /** 失败/限流退避：网络不通或被限流时，至少再等 2h，避免每个 ticker 都空打 GitHub。 */
    private const val FAILED_RETRY_BACKOFF_MS = 2 * 60 * 60 * 1000L

    sealed interface CheckResult {
        /** 命中软 TTL，未发起请求。 */
        data object Skipped : CheckResult

        /** 请求失败（网络/解析），缓存的版本信息未更新。 */
        data object Failed : CheckResult

        /** 被 GitHub 限流（403 额度耗尽或 429）；[resetEpochSeconds] 为恢复时间（0 未知）。 */
        data class RateLimited(val resetEpochSeconds: Long) : CheckResult

        /** 远端有新版但暂无可下载 APK（可能正在上传）；不写成功缓存以便尽快重试。 */
        data class NoApk(val version: String) : CheckResult

        /** 检查成功，远端比当前新。 */
        data class NewVersion(val state: UpdateState) : CheckResult

        /** 检查成功，当前已是最新。 */
        data class UpToDate(val state: UpdateState) : CheckResult
    }

    /** 后台懒触发判据：距上次成功超过 24h 且已过退避点（[UpdateState.nextRetryTime]）。 */
    suspend fun shouldCheck(): Boolean {
        val state = DataStoreHolder.updateState.data.first()
        val now = System.currentTimeMillis()
        if (now - state.lastCheckSuccessTime < CHECK_INTERVAL_MS) return false
        return now >= state.nextRetryTime
    }

    /**
     * 拉取最新 release 并写入缓存。
     *
     * @param force 为 true 时绕过软 TTL（手动检查），仍只读、零副作用。
     */
    suspend fun checkAndCache(force: Boolean): CheckResult {
        val current = DataStoreHolder.updateState.data.first()
        val now = System.currentTimeMillis()
        if (!force && now - current.lastCheckAttemptTime < ATTEMPT_TTL_MS) {
            return CheckResult.Skipped
        }
        DataStoreHolder.updateState.updateData { it.copy(lastCheckAttemptTime = now) }

        val release = when (val result = UpdateChecker.fetchLatestRelease()) {
            is UpdateChecker.FetchResult.Success -> result.release
            is UpdateChecker.FetchResult.RateLimited -> {
                // 网络不通/限流：退避 2h
                setNextRetry(now + FAILED_RETRY_BACKOFF_MS)
                return CheckResult.RateLimited(result.resetEpochSeconds)
            }
            UpdateChecker.FetchResult.Failed -> {
                setNextRetry(now + FAILED_RETRY_BACKOFF_MS)
                return CheckResult.Failed
            }
        }
        val asset = UpdateChecker.pickApkAsset(release)
        val isNewer = UpdateChecker.isRemoteNewer(release.tagName, BuildConfig.VERSION_NAME)
        val hasApk = asset != null && asset.size > 0

        // 有新版但暂无可下载 APK：不写成功缓存（lastCheckSuccessTime 不更新），也不返回 NewVersion，
        // 避免误发通知/弹「立即更新」后点击必失败。网络是通的、APK 多为正在上传，
        // 故不长退避（nextRetryTime 清 0），靠 ticker 自然节奏（约 30min）较快拿到补传的包
        if (isNewer && !hasApk) {
            setNextRetry(0L)
            return CheckResult.NoApk(release.tagName)
        }

        val newState = DataStoreHolder.updateState.updateData {
            it.copy(
                latestVersion = release.tagName,
                notes = release.body.ifBlank { release.name },
                apkUrl = asset?.browserDownloadUrl.orEmpty(),
                apkSize = asset?.size ?: 0L,
                lastCheckSuccessTime = System.currentTimeMillis(),
                nextRetryTime = 0L
            )
        }
        return if (isNewer) {
            CheckResult.NewVersion(newState)
        } else {
            CheckResult.UpToDate(newState)
        }
    }

    private suspend fun setNextRetry(time: Long) {
        DataStoreHolder.updateState.updateData { it.copy(nextRetryTime = time) }
    }
}
