package com.aaron.sidegesture.ui.update

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.update.UpdateViewModel.UiEvent
import com.aaron.sidegesture.ui.update.UpdateViewModel.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.update.ApkInstaller
import com.aaron.sidegesture.utils.update.DownloadController
import com.aaron.sidegesture.utils.update.DownloadController.DownloadStatus
import com.aaron.sidegesture.utils.update.DownloadService
import com.aaron.sidegesture.utils.update.UpdateChecker
import com.aaron.sidegesture.utils.update.UpdateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 更新交互的唯一收口（主进程）。
 *
 * 合成三处状态决定弹窗：
 * 1. UpdateState 跨进程缓存（检查结果）
 * 2. DownloadController 主进程内存（下载进行中 / 失败）
 * 3. apk 文件态（ApkInstaller.isDownloaded，「已下完」唯一判据，进程重启不丢）
 *
 * 优先级：下载中 > 下载失败 > 已下完 > 有新版未下 > 已最新。
 * 下载委托 DownloadService（前台全局互斥），本 VM 不直接下载。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/18
 */
class UpdateViewModel : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    // 会话级：用户在「已下完」弹窗点关闭后，切回前台不再反复弹安装提示（启动/通知入口仍会浮）
    private var installPromptDismissed = false

    init {
        // 下载进度 / 缓存变化时只刷新内容；但下载「刚结束」（完成/失败）这个状态转换要主动浮出入口，
        // 否则点了「后台下载」又停留在 App 时，下完/失败既无弹窗也（拒权限时）无通知 = 完全无感知
        viewModelScope.launch {
            var prevStatus: DownloadStatus? = null
            DownloadController.flow.collect { download ->
                val justEnded = prevStatus == DownloadStatus.DOWNLOADING &&
                        download.status != DownloadStatus.DOWNLOADING
                prevStatus = download.status
                recompute(if (justEnded) OpenMode.DownloadEnded else OpenMode.None)
            }
        }
        viewModelScope.launch {
            DataStoreHolder.updateState.data.collect { recompute(OpenMode.None) }
        }
    }

    /** App 启动：开启自动检查、缓存不新鲜(>1h)且已过退避点时懒触发检查，随后按状态决定是否自动弹窗。 */
    fun checkOnLaunch() {
        viewModelScope.launch {
            val autoCheck = DataStoreHolder.advancedSettings.data.first().autoCheckUpdate
            val cache = DataStoreHolder.updateState.data.first()
            val now = System.currentTimeMillis()
            if (autoCheck &&
                now - cache.lastCheckSuccessTime >= LAUNCH_FRESH_MS &&
                now >= cache.nextRetryTime
            ) {
                UpdateRepository.checkAndCache(force = false)
            }
            recompute(OpenMode.Auto)
        }
    }

    /** 启动入口 / 通知点击（onCreate、onNewIntent）：按状态决定弹窗（含有新版自动弹）。 */
    fun onEntry() {
        viewModelScope.launch {
            recompute(OpenMode.Auto)
        }
    }

    /** 回到前台（onResume）：只浮出下载相关态（进行中/已下完/失败），不重复弹「有新版」。 */
    fun onForeground() {
        viewModelScope.launch {
            recompute(OpenMode.Resume)
        }
    }

    /** 关于页手动检查：强制绕过 TTL，失败弹提示弹窗，成功（含已最新）都弹更新窗。 */
    fun checkManually() {
        viewModelScope.launch {
            if (uiState.checking) return@launch
            updateUiState { it.copy(checking = true) }
            toast(R.string.update_checking)
            val result = UpdateRepository.checkAndCache(force = true)
            updateUiState { it.copy(checking = false) }
            when (result) {
                is UpdateRepository.CheckResult.Failed -> showCheckFailed(CheckFailedReason.Generic)
                is UpdateRepository.CheckResult.RateLimited -> showCheckFailed(CheckFailedReason.RateLimited)
                is UpdateRepository.CheckResult.NoApk -> showCheckFailed(CheckFailedReason.NoApk)
                else -> {
                    // 主动检查到结果：清掉残留的下载失败态，避免显示成「下载失败」
                    if (DownloadController.flow.value.status == DownloadStatus.FAILED) {
                        DownloadController.reset()
                    }
                    recompute(OpenMode.Force)
                }
            }
        }
    }

    private fun showCheckFailed(reason: CheckFailedReason) {
        updateUiState { it.copy(showCheckFailedDialog = true, checkFailedReason = reason) }
    }

    fun dismissCheckFailedDialog() {
        updateUiState { it.copy(showCheckFailedDialog = false) }
    }

    /** 首启请求通知权限：仅当「自动检查已开 + 未授权 + 没主动问过」时弹说明（只主动问一次）。 */
    fun evaluateNotificationPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        viewModelScope.launch {
            val initial = DataStoreHolder.initialSettings.data.first()
            if (initial.notificationPermissionRequested) return@launch
            val autoCheck = DataStoreHolder.advancedSettings.data.first().autoCheckUpdate
            if (!autoCheck) return@launch
            // 无论已授权还是即将弹说明，都标记「已主动问过」，确保只问一次
            DataStoreHolder.initialSettings.updateData { it.copy(notificationPermissionRequested = true) }
            val granted = ContextCompat.checkSelfPermission(
                App.getContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                updateUiState { it.copy(showNotificationRationale = true) }
            }
        }
    }

    fun dismissNotificationRationale() {
        updateUiState { it.copy(showNotificationRationale = false) }
    }

    /** [立即更新] / [重试]：委托前台 DownloadService 下载，弹窗保持显示进度。 */
    fun onConfirmUpdate() {
        val context = App.getContext()
        if (!ApkInstaller.canInstall(context)) {
            toast(R.string.update_install_permission_hint)
            ApkInstaller.gotoUnknownSourceSetting(context)
            return
        }
        if (uiState.apkUrl.isBlank()) {
            toast(R.string.update_download_failed)
            return
        }
        installPromptDismissed = false
        DownloadService.start(context, uiState.version, uiState.apkUrl, uiState.apkSize)
    }

    /** [点击安装]：文件态校验通过则安装，丢失/损坏则降级重下。 */
    fun onInstall() {
        val context = App.getContext()
        val file = ApkInstaller.apkFile(context, uiState.version)
        if (!ApkInstaller.isDownloaded(file, uiState.apkSize)) {
            toast(R.string.update_download_failed)
            onConfirmUpdate()
            return
        }
        if (!ApkInstaller.canInstall(context)) {
            toast(R.string.update_install_permission_hint)
            ApkInstaller.gotoUnknownSourceSetting(context)
            return
        }
        updateUiState { it.copy(showDialog = false) }
        if (!ApkInstaller.installApk(context, file)) {
            toast(R.string.update_download_failed)
        }
    }

    /** [转后台]：仅关闭弹窗，下载在 Service / 通知里继续。 */
    fun onMoveToBackground() {
        updateUiState { it.copy(showDialog = false) }
    }

    fun onIgnoreVersion() {
        val version = uiState.version
        viewModelScope.launch {
            DataStoreHolder.initialSettings.updateData {
                it.copy(ignoredUpdateVersion = version)
            }
            updateUiState { it.copy(showDialog = false) }
        }
    }

    /** 关闭弹窗：下载中=转后台（下载不中断）；失败态消费掉、已下完态记关闭，避免切回前台反复浮出。 */
    fun dismiss() {
        when (uiState.phase) {
            UpdatePhase.Failed -> DownloadController.reset()
            UpdatePhase.Downloaded -> installPromptDismissed = true
            else -> Unit
        }
        updateUiState { it.copy(showDialog = false) }
    }

    private suspend fun recompute(openMode: OpenMode) {
        val context = App.getContext()
        val cache = DataStoreHolder.updateState.data.first()
        val ignored = DataStoreHolder.initialSettings.data.first().ignoredUpdateVersion
        val download = DownloadController.flow.value

        val downloaded = ApkInstaller.isDownloaded(
            ApkInstaller.apkFile(context, cache.latestVersion),
            cache.apkSize
        )
        val isNewer = cache.latestVersion.isNotBlank() &&
                UpdateChecker.isRemoteNewer(cache.latestVersion, BuildConfig.VERSION_NAME)

        val phase = when {
            download.status == DownloadStatus.DOWNLOADING -> UpdatePhase.Downloading
            download.status == DownloadStatus.FAILED -> UpdatePhase.Failed
            downloaded && isNewer -> UpdatePhase.Downloaded
            isNewer -> UpdatePhase.NewVersion
            else -> UpdatePhase.UpToDate
        }

        // 下载中显示「实际正在下载的版本」，避免后台复查改缓存后进行中下载从弹窗错位/消失
        val version = if (phase == UpdatePhase.Downloading) download.version else cache.latestVersion

        val show = when (openMode) {
            OpenMode.None -> uiState.showDialog
            OpenMode.Force -> true
            OpenMode.Auto -> when (phase) {
                UpdatePhase.Downloading, UpdatePhase.Downloaded, UpdatePhase.Failed -> true
                UpdatePhase.NewVersion -> cache.latestVersion != ignored
                UpdatePhase.UpToDate -> uiState.showDialog
            }
            OpenMode.Resume -> when (phase) {
                // 切回前台：已下完浮一次安装入口（关过则不再反复），失败浮（关闭即消费）；
                // 下载中/有新版/已最新不在此主动弹，避免反复打扰
                UpdatePhase.Downloaded -> !installPromptDismissed
                UpdatePhase.Failed -> true
                else -> uiState.showDialog
            }
            OpenMode.DownloadEnded -> when (phase) {
                // 下载刚结束：浮出已下完/失败入口（停留 App 也能看到），其余保持
                UpdatePhase.Downloaded, UpdatePhase.Failed -> true
                else -> uiState.showDialog
            }
        }

        updateUiState {
            it.copy(
                showDialog = show,
                phase = phase,
                version = version,
                notes = cache.notes,
                apkUrl = cache.apkUrl,
                apkSize = cache.apkSize,
                progress = download.progress
            )
        }
    }

    private enum class OpenMode {
        /** 仅刷新内容，不改变弹窗显隐。 */
        None,

        /** 按状态自动决定是否弹（含有新版，受忽略版本约束）。 */
        Auto,

        /** 切回前台：只浮下载相关态，不重复弹有新版。 */
        Resume,

        /** 下载刚结束（完成/失败）：浮出安装/重试入口，即使用户停留在 App。 */
        DownloadEnded,

        /** 强制弹（手动检查）。 */
        Force
    }

    enum class UpdatePhase {
        Downloading,
        Downloaded,
        NewVersion,
        UpToDate,
        Failed
    }

    /** 手动检查失败的原因，决定失败弹窗文案。 */
    enum class CheckFailedReason {
        Generic,
        RateLimited,
        NoApk
    }

    data class UiState(
        val showDialog: Boolean = false,
        val phase: UpdatePhase = UpdatePhase.NewVersion,
        val version: String = "",
        val notes: String = "",
        val apkUrl: String = "",
        val apkSize: Long = 0L,
        val progress: Int = 0,
        val checking: Boolean = false,
        val showCheckFailedDialog: Boolean = false,
        val checkFailedReason: CheckFailedReason = CheckFailedReason.Generic,
        val showNotificationRationale: Boolean = false
    )

    sealed interface UiEvent

    private companion object {
        /** 启动缓存新鲜窗口：1h 内成功检查过则不再发网络请求。 */
        const val LAUNCH_FRESH_MS = 60 * 60 * 1000L
    }
}
