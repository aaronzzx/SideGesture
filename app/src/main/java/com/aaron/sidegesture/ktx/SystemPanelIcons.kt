package com.aaron.sidegesture.ktx

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

enum class SystemPanelIconType {
    NotificationPanel,
    QuickSettingsPanel
}

fun systemPanelIcon(type: SystemPanelIconType): ImageVector {
    return when (type) {
        SystemPanelIconType.NotificationPanel -> Icons.Default.Notifications
        SystemPanelIconType.QuickSettingsPanel -> Icons.Default.Widgets
    }
}
