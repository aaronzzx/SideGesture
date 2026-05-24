package com.aaron.sidegesture.quicktools

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.global.QuickToolType
import com.aaron.sidegesture.ktx.SystemPanelIconType
import com.aaron.sidegesture.ktx.systemPanelIcon

fun Context.quickToolText(type: QuickToolType): String {
    return when (type) {
        QuickToolType.MediaControl -> getString(R.string.quick_tool_media_control)
        QuickToolType.Brightness -> getString(R.string.quick_tool_brightness)
        QuickToolType.Volume -> getString(R.string.quick_tool_volume)
        QuickToolType.Flashlight -> getString(R.string.quick_tool_flashlight)
        QuickToolType.Mute -> getString(R.string.quick_tool_mute)
        QuickToolType.Wifi -> getString(R.string.quick_tool_wifi)
        QuickToolType.Bluetooth -> getString(R.string.quick_tool_bluetooth)
        QuickToolType.NotificationPanel -> getString(R.string.quick_tool_notification_panel)
        QuickToolType.QuickSettingsPanel -> getString(R.string.quick_tool_quick_settings_panel)
        QuickToolType.LockScreen -> getString(R.string.quick_tool_lock_screen)
        QuickToolType.Screenshot -> getString(R.string.quick_tool_screenshot)
    }
}

@Composable
fun quickToolText(type: QuickToolType): String {
    return when (type) {
        QuickToolType.MediaControl -> stringResource(R.string.quick_tool_media_control)
        QuickToolType.Brightness -> stringResource(R.string.quick_tool_brightness)
        QuickToolType.Volume -> stringResource(R.string.quick_tool_volume)
        QuickToolType.Flashlight -> stringResource(R.string.quick_tool_flashlight)
        QuickToolType.Mute -> stringResource(R.string.quick_tool_mute)
        QuickToolType.Wifi -> stringResource(R.string.quick_tool_wifi)
        QuickToolType.Bluetooth -> stringResource(R.string.quick_tool_bluetooth)
        QuickToolType.NotificationPanel -> stringResource(R.string.quick_tool_notification_panel)
        QuickToolType.QuickSettingsPanel -> stringResource(R.string.quick_tool_quick_settings_panel)
        QuickToolType.LockScreen -> stringResource(R.string.quick_tool_lock_screen)
        QuickToolType.Screenshot -> stringResource(R.string.quick_tool_screenshot)
    }
}

fun quickToolIcon(type: QuickToolType): ImageVector {
    return when (type) {
        QuickToolType.MediaControl -> Icons.Default.MusicNote
        QuickToolType.Brightness -> Icons.Default.Brightness6
        QuickToolType.Volume -> Icons.Default.VolumeUp
        QuickToolType.Flashlight -> Icons.Default.FlashlightOn
        QuickToolType.Mute -> Icons.Default.VolumeMute
        QuickToolType.Wifi -> Icons.Default.Wifi
        QuickToolType.Bluetooth -> Icons.Default.Bluetooth
        QuickToolType.NotificationPanel -> systemPanelIcon(SystemPanelIconType.NotificationPanel)
        QuickToolType.QuickSettingsPanel -> systemPanelIcon(SystemPanelIconType.QuickSettingsPanel)
        QuickToolType.LockScreen -> Icons.Default.Lock
        QuickToolType.Screenshot -> Icons.Default.Screenshot
    }
}
