package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.DayNightMode
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
@Serializable
@Keep
data class AdvancedSettings(
    val excludeApps: List<String> = emptyList(),
    val animationStyles: AnimationStyles = AnimationStyles(),
    val actionPanelStyles: ActionPanelStyles = ActionPanelStyles(),
    val fitSoftKeyboard: Boolean = true,
    val hideLandscape: Boolean = false,
    val hideQuickPanel: Boolean = false,
    val hideScreenLock: Boolean = false,
    val hideHomeScreen: Boolean = false,
    val excludeFromRecents: Boolean = false,
    val dynamicColor: Boolean = false,
    val dayNightMode: DayNightMode = DayNightMode.Auto,
    val clipApps: Map<String, Float> = emptyMap()
)
