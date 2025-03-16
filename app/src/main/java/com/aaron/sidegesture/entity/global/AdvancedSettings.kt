package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ActionPanelStyles
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.AnimationStyles
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ClipApps
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.DayNightMode
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.DynamicColor
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ExcludeApps
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ExcludeFromRecents
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.FitSoftKeyboard
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideHomeScreen
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideLandscape
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideQuickPanel
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideScreenLock
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideTemporary
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
    // packageName
    val excludeApps: List<String> = ExcludeApps,
    val animationStyles: AnimationStyles = AnimationStyles,
    val actionPanelStyles: ActionPanelStyles = ActionPanelStyles,
    val fitSoftKeyboard: Boolean = FitSoftKeyboard,
    val hideLandscape: Boolean = HideLandscape,
    val hideQuickPanel: Boolean = HideQuickPanel,
    val hideScreenLock: Boolean = HideScreenLock,
    val hideHomeScreen: Boolean = HideHomeScreen,
    val hideTemporary: Boolean = HideTemporary,
    val excludeFromRecents: Boolean = ExcludeFromRecents,
    val dynamicColor: Boolean = DynamicColor,
    val dayNightMode: DayNightMode = DayNightMode,
    // qualifiedName
    val clipApps: Map<String, Float> = ClipApps
)
