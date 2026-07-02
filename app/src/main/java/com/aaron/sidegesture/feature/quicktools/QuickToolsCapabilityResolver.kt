package com.aaron.sidegesture.feature.quicktools

import android.content.Context
import com.aaron.sidegesture.ktx.canWriteSystemSettings
import com.aaron.sidegesture.ktx.isNotificationListenerEnabled
import com.aaron.sidegesture.platform.shizuku.ShizukuShellManager

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
