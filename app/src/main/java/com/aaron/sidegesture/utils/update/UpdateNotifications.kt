package com.aaron.sidegesture.utils.update

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aaron.sidegesture.MainActivity
import com.aaron.sidegesture.R

/**
 * 更新相关通知统一出口：两个 channel + 统一 PendingIntent（指向 [MainActivity]）。
 *
 * - `update` channel：`:service` 后台「发现新版」提醒。
 * - `download` channel：前台下载进度 / 完成 / 失败（静默 ongoing）。
 *
 * 所有通知点击都拉起 [MainActivity] 让 ViewModel 读状态决定弹窗，不依赖 intent 标记。
 * 无 `POST_NOTIFICATIONS` 权限时静默跳过 notify（功能照常，仅无通知）。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/19
 */
object UpdateNotifications {

    private const val CHANNEL_UPDATE = "update"
    private const val CHANNEL_DOWNLOAD = "download"

    const val NOTIFICATION_ID_NEW_VERSION = 1001
    const val NOTIFICATION_ID_DOWNLOAD = 1002

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_UPDATE) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_UPDATE,
                    context.getString(R.string.update_channel_update),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        if (nm.getNotificationChannel(CHANNEL_DOWNLOAD) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DOWNLOAD,
                    context.getString(R.string.update_channel_download),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** `:service` 后台发现新版时调用。 */
    fun showNewVersion(context: Context, version: String) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(context.getString(R.string.update_notification_new_title))
            .setContentText(context.getString(R.string.update_notification_new_content, UpdateChecker.displayVersion(version)))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notifyIfPermitted(context, NOTIFICATION_ID_NEW_VERSION, notification)
    }

    /** 用户开始下载时去重，取消「发现新版」通知。 */
    fun cancelNewVersion(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_NEW_VERSION)
    }

    /** 前台下载通知，供 startForeground 与进度刷新复用。 */
    fun buildDownloadNotification(context: Context, progress: Int): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(context.getString(R.string.update_notification_downloading_title))
            .setContentText("$progress%")
            .setProgress(100, progress.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun notifyDownloadProgress(context: Context, progress: Int) {
        notifyIfPermitted(context, NOTIFICATION_ID_DOWNLOAD, buildDownloadNotification(context, progress))
    }

    fun showDownloadDone(context: Context, version: String) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(context.getString(R.string.update_notification_done_title))
            .setContentText(context.getString(R.string.update_notification_done_content, UpdateChecker.displayVersion(version)))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifyIfPermitted(context, NOTIFICATION_ID_DOWNLOAD, notification)
    }

    fun showDownloadFailed(context: Context) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(context.getString(R.string.update_notification_failed_title))
            .setContentText(context.getString(R.string.update_notification_failed_content))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifyIfPermitted(context, NOTIFICATION_ID_DOWNLOAD, notification)
    }

    private fun notifyIfPermitted(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

}
