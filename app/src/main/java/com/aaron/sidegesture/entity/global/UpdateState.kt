package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * 更新检查结果缓存（跨进程共享，MultiProcessDataStore）。
 *
 * 只存「检查结果」，不存任何下载状态/进度/路径：
 * - `:service` 进程后台检查后写入，主进程读出并合成弹窗状态。
 * - 「下载完成」不在这里记录，统一由 apk 文件存在 + size 匹配判定（进程重启不丢）。
 *
 * 放在 entity 包，由 proguard 的 `-keep class com.aaron.sidegesture.entity.** { *; }` 自动覆盖。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/19
 */
@Serializable
@Keep
data class UpdateState(
    val latestVersion: String = "",
    val notes: String = "",
    val apkUrl: String = "",
    // GitHub asset.size，用于缓存命中 + .part 续传校验
    val apkSize: Long = 0L,
    // 最近一次检查成功的时间，用于懒触发 + 缓存新鲜度
    val lastCheckSuccessTime: Long = 0L,
    // 最近一次检查尝试的时间（含失败），用于 check 软 TTL 限流
    val lastCheckAttemptTime: Long = 0L,
    // 下次最早重试时间（退避到期点）：真失败/限流退 2h，NoApk 不退避(0)，成功清 0
    val nextRetryTime: Long = 0L
)
