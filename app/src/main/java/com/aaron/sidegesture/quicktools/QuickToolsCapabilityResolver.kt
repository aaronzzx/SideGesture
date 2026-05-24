package com.aaron.sidegesture.quicktools

import android.content.Context
import com.aaron.sidegesture.ktx.canWriteSystemSettings
import com.aaron.sidegesture.ktx.isNotificationListenerEnabled
import com.aaron.sidegesture.shizuku.ShizukuShellManager

object QuickToolsCapabilityResolver {

    fun resolve(context: Context): QuickToolsCapabilityState {
        val shizukuStatus = ShizukuShellManager.currentStatus()
        return QuickToolsCapabilityState(
            canWriteSystemSettings = context.canWriteSystemSettings(),
            shizukuReady = shizukuStatus.permissionGranted,
            notificationListenerEnabled = context.isNotificationListenerEnabled(
                QuickToolsNotificationListenerService::class.java
            )
        )
    }
}

data class QuickToolsCapabilityState(
    val canWriteSystemSettings: Boolean,
    val shizukuReady: Boolean,
    val notificationListenerEnabled: Boolean
)
