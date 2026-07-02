package com.aaron.sidegesture.feature.update

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 前台下载 Service（主进程，dataSync）。
 *
 * - `onStartCommand` 内即时 `startForeground`（5s 限制）。
 * - **单飞**：同版本已在下载则忽略本次 start，避免同 `.part` 竞争。
 * - 实现 `onTimeout()`：Android 14+/15 dataSync 超时回调里收尾自杀，避免被系统强杀。
 * - 真正下载委托 [ApkInstaller.download]（断点续传），进度写 [DownloadController] + 通知。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/19
 */
class DownloadService : Service() {

    companion object {
        private const val EXTRA_VERSION = "extra_version"
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_SIZE = "extra_size"

        /** 通知节流：进度变化达到该步长（或到 100%）才刷新通知。 */
        private const val NOTIFY_STEP = 2

        fun start(context: Context, version: String, url: String, size: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_VERSION, version)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_SIZE, size)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var lastStartId = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId

        // 5s 内必须 startForeground
        startForegroundCompat(DownloadController.flow.value.progress)

        // 全局互斥：只要已在下载，忽略任何新 start（含不同版本），
        // 避免两个下载协程互删 .part / 覆盖内存状态 / 旧任务停掉新任务
        if (job?.isActive == true) {
            return START_NOT_STICKY
        }

        val version = intent?.getStringExtra(EXTRA_VERSION).orEmpty()
        val url = intent?.getStringExtra(EXTRA_URL).orEmpty()
        val size = intent?.getLongExtra(EXTRA_SIZE, 0L) ?: 0L
        if (url.isBlank() || version.isBlank()) {
            stopSelfResult(lastStartId)
            return START_NOT_STICKY
        }

        DownloadController.onStart(version)
        // 去重：开始下载即取消「发现新版」通知
        UpdateNotifications.cancelNewVersion(this)

        job = scope.launch {
            val context = this@DownloadService
            val dir = ApkInstaller.updateDir(context)
            val dest = ApkInstaller.apkFile(context, version)
            ApkInstaller.clearOutdatedApks(dir, dest.name)
            var lastNotified = -1
            val success = ApkInstaller.download(url, dest, size) { percent ->
                DownloadController.onProgress(percent)
                if (percent >= 100 || percent - lastNotified >= NOTIFY_STEP) {
                    lastNotified = percent
                    UpdateNotifications.notifyDownloadProgress(context, percent)
                }
            }
            if (success) {
                DownloadController.onFinish()
                stopForegroundCompat()
                UpdateNotifications.showDownloadDone(context, version)
            } else {
                DownloadController.onFailed(version)
                stopForegroundCompat()
                UpdateNotifications.showDownloadFailed(context)
            }
            // 只在「最后一次 start」对应的下载结束时停服务，避免被忽略的新 start 误停
            stopSelfResult(lastStartId)
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        handleTimeout()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(startId: Int, fgsType: Int) {
        handleTimeout()
    }

    private fun handleTimeout() {
        job?.cancel()
        DownloadController.reset()
        stopForegroundCompat()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun startForegroundCompat(progress: Int) {
        val notification = UpdateNotifications.buildDownloadNotification(this, progress)
        ServiceCompat.startForeground(
            this,
            UpdateNotifications.NOTIFICATION_ID_DOWNLOAD,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}
