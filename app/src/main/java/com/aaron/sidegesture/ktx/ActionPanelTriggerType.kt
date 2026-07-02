package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.feature.actionpanel.ActionPanelState

/**
 * @author DS-Z
 * @since 2025/6/30
 */

fun ActionPanelState.TriggerType.isMiniWindow(longPressLaunchPopup: Boolean): Boolean {
    return when (this) {
        ActionPanelState.TriggerType.Press -> !longPressLaunchPopup
        ActionPanelState.TriggerType.LongPress -> longPressLaunchPopup
    }
}