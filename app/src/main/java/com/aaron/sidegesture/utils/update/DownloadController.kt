package com.aaron.sidegesture.utils.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 主进程内存单例，承载下载「进行中」实时状态。
 *
 * 只表达 IDLE / DOWNLOADING / FAILED —— **没有 DONE**：
 * 「已下完」一律由 apk 文件存在 + size 匹配判定（[ApkInstaller.isDownloaded]），进程重启不丢。
 * 进度只在这里，绝不进 DataStore（避免写放大）。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/19
 */
object DownloadController {

    enum class DownloadStatus { IDLE, DOWNLOADING, FAILED }

    data class DownloadUiState(
        val status: DownloadStatus = DownloadStatus.IDLE,
        val version: String = "",
        val progress: Int = 0
    )

    private val _flow = MutableStateFlow(DownloadUiState())
    val flow: StateFlow<DownloadUiState> = _flow.asStateFlow()

    fun onStart(version: String) {
        _flow.value = DownloadUiState(DownloadStatus.DOWNLOADING, version, 0)
    }

    fun onProgress(percent: Int) {
        _flow.update { it.copy(progress = percent.coerceIn(0, 100)) }
    }

    /** 下载成功结束：回 IDLE，完成态交给文件判据。 */
    fun onFinish() {
        _flow.value = DownloadUiState()
    }

    fun onFailed(version: String) {
        _flow.update { it.copy(status = DownloadStatus.FAILED, version = version) }
    }

    fun reset() {
        _flow.value = DownloadUiState()
    }
}
