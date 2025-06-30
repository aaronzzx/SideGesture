package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.ui.widget.ActionPanelState

/**
 * @author DS-Z
 * @since 2025/6/30
 */

fun ActionPanelState.TriggerType.isMiniWindow(): Boolean {
    return when (this) {
        ActionPanelState.TriggerType.Press -> true
        ActionPanelState.TriggerType.LongPress -> false
    }
}