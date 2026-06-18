package com.aaron.sidegesture.ui.update

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.GithubRelease
import com.aaron.sidegesture.ui.update.UpdateViewModel.UiEvent
import com.aaron.sidegesture.ui.update.UpdateViewModel.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.update.ApkInstaller
import com.aaron.sidegesture.utils.update.UpdateChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * 应用内检查更新（GitHub Releases）。
 *
 * - 启动静默检查：失败 / 已最新都不打扰用户
 * - 手动检查：已最新 / 失败均 toast
 * - 发现新版且未被忽略 → 弹底部卡片，可下载安装 / 忽略此版本
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/18
 */
class UpdateViewModel : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    /** 启动时静默检查：失败或已是最新都不弹窗、不提示。 */
    fun checkOnLaunch() {
        viewModelScope.launch {
            if (uiState.showDialog || uiState.downloading) return@launch
            val release = UpdateChecker.fetchLatestRelease() ?: return@launch
            if (!UpdateChecker.isRemoteNewer(release.tagName, BuildConfig.VERSION_NAME)) return@launch
            val ignored = DataStoreHolder.initialSettings.data.first().ignoredUpdateVersion
            if (release.tagName == ignored) return@launch
            showRelease(release)
        }
    }

    /** 手动检查：已最新 toast 提示，失败 toast 提示。 */
    fun checkManually() {
        viewModelScope.launch {
            if (uiState.checking || uiState.downloading) return@launch
            updateUiState { it.copy(checking = true) }
            val release = UpdateChecker.fetchLatestRelease()
            updateUiState { it.copy(checking = false) }
            if (release == null) {
                toast(R.string.update_check_failed)
                return@launch
            }
            if (UpdateChecker.isRemoteNewer(release.tagName, BuildConfig.VERSION_NAME)) {
                showRelease(release)
            } else {
                toast(R.string.update_already_latest)
            }
        }
    }

    private fun showRelease(release: GithubRelease) {
        val asset = UpdateChecker.pickApkAsset(release)
        updateUiState {
            it.copy(
                showDialog = true,
                version = release.tagName,
                notes = release.body.ifBlank { release.name },
                releaseUrl = release.htmlUrl.ifBlank { UpdateChecker.RELEASES_PAGE_URL },
                apkUrl = asset?.browserDownloadUrl.orEmpty(),
                apkSize = asset?.size ?: 0L,
                downloading = false,
                progress = 0
            )
        }
    }

    fun onConfirmUpdate() {
        if (uiState.downloading) return
        val context = App.getContext()
        if (!ApkInstaller.canInstall(context)) {
            toast(R.string.update_install_permission_hint)
            ApkInstaller.gotoUnknownSourceSetting(context)
            return
        }
        val url = uiState.apkUrl
        if (url.isBlank()) {
            toast(R.string.update_download_failed)
            return
        }
        val updateDir = File(context.externalCacheDir, "update")
        val destFile = File(updateDir, apkFileNameOf(uiState.version))
        // 清理旧版本残留，只保留本次目标文件
        ApkInstaller.clearOutdatedApks(updateDir, destFile.name)
        // 缓存命中（同版本已完整下载）：跳过下载直接安装
        if (ApkInstaller.isDownloaded(destFile, uiState.apkSize)) {
            updateUiState { it.copy(showDialog = false) }
            ApkInstaller.installApk(context, destFile)
            return
        }
        viewModelScope.launch {
            updateUiState { it.copy(downloading = true, progress = 0) }
            val success = ApkInstaller.download(url, destFile) { percent ->
                updateUiState { it.copy(progress = percent) }
            }
            if (success) {
                updateUiState { it.copy(showDialog = false, downloading = false, progress = 0) }
                ApkInstaller.installApk(context, destFile)
            } else {
                updateUiState { it.copy(downloading = false, progress = 0) }
                toast(R.string.update_download_failed)
            }
        }
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

    /** 下载中禁止关闭，避免任务被中断后状态不一致。 */
    fun dismiss() {
        if (uiState.downloading) return
        updateUiState { it.copy(showDialog = false) }
    }

    /** 按版本号命名，做版本隔离，避免不同版本同名 asset 复用到错误缓存。 */
    private fun apkFileNameOf(version: String): String {
        val safe = version.trim().ifBlank { "update" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "$safe.apk"
    }

    data class UiState(
        val showDialog: Boolean = false,
        val version: String = "",
        val notes: String = "",
        val releaseUrl: String = "",
        val apkUrl: String = "",
        val apkSize: Long = 0L,
        val checking: Boolean = false,
        val downloading: Boolean = false,
        val progress: Int = 0
    )

    sealed interface UiEvent
}
