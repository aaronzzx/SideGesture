package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ActionPanelAppLongPressLaunchPopup
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ActionPanelAppSwitchWindowModeDelayMs
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ActionPanelStyles
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.AnimationStyles
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.AutoCheckUpdate
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ClipApps
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ClipShortcuts
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.DayNightMode
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.DynamicColor
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ExcludeApps
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.ExcludeFromRecents
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.FitSoftKeyboard
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideGestureOnIme
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideHomeScreen
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideLandscape
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideQuickPanel
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.HideScreenLock
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults.VolumeButtonSwitchSong
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
    val volumeButtonSwitchSong: Boolean = VolumeButtonSwitchSong,
    val fitSoftKeyboard: Boolean = FitSoftKeyboard,
    val hideGestureOnIme: Boolean = HideGestureOnIme,
    val actionPanelAppLongPressLaunchPopup: Boolean = ActionPanelAppLongPressLaunchPopup,
    val actionPanelAppSwitchWindowModeDelayMs: Long = ActionPanelAppSwitchWindowModeDelayMs,
    val hideLandscape: Boolean = HideLandscape,
    val hideQuickPanel: Boolean = HideQuickPanel,
    val hideScreenLock: Boolean = HideScreenLock,
    val hideHomeScreen: Boolean = HideHomeScreen,
    val excludeFromRecents: Boolean = ExcludeFromRecents,
    val dynamicColor: Boolean = DynamicColor,
    val dayNightMode: DayNightMode = DayNightMode,
    // 后台定期检查新版本并通知（控制 :service 进程的低频检查 + 发现新版通知）
    val autoCheckUpdate: Boolean = AutoCheckUpdate,
    // qualifiedName
    val clipApps: Map<String, Float> = ClipApps,
    // qualifiedNameWithIntents
    val clipShortcuts: Map<String, Float> = ClipShortcuts
)
