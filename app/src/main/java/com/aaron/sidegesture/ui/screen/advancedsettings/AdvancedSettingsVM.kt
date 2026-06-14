package com.aaron.sidegesture.ui.screen.advancedsettings

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.DayNightMode
import com.aaron.sidegesture.entity.normalizeActionPanelStyleType
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class AdvancedSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun onVolumeButtonSwitchSong(volumeButtonSwitchSong: Boolean) {
        if (volumeButtonSwitchSong) {
            toast(R.string.volume_button_switch_song_warning)
        }
        updateUiState {
            it.copy(volumeButtonSwitchSong = volumeButtonSwitchSong)
        }
        saveSettings()
    }

    fun onShowAnimation(showAnimation: Boolean) {
        updateUiState {
            it.copy(showAnimation = showAnimation)
        }
        saveSettings()
    }

    fun showDayNightModeDropdownMenu(show: Boolean) {
        updateUiState {
            it.copy(showDayNightModeDropdownMenu = show)
        }
    }

    fun onFitSoftKeyboardChange(value: Boolean) {
        updateUiState {
            it.copy(fitSoftKeyboard = value)
        }
        saveSettings()
    }

    fun onActionPanelAppLongPressLaunchPopupChanged(value: Boolean) {
        updateUiState {
            it.copy(actionPanelAppLongPressLaunchPopup = value)
        }
        saveSettings()
    }

    fun onActionPanelAppSwitchWindowModeDelayMsChange(value: Float) {
        updateUiState {
            it.copy(actionPanelAppSwitchWindowModeDelayMs = value.toLong())
        }
    }

    fun onActionPanelAppSwitchWindowModeDelayMsChangeFinished() {
        saveSettings()
    }

    fun onExcludeFromRecentsChange(value: Boolean) {
        updateUiState {
            it.copy(excludeFromRecents = value)
        }
        saveSettings()
    }

    fun onHideLandscapeChange(value: Boolean) {
        updateUiState {
            it.copy(hideLandscape = value)
        }
        saveSettings()
    }

    fun onHideQuickPanelChange(value: Boolean) {
        updateUiState {
            it.copy(hideQuickPanel = value)
        }
        saveSettings()
    }

    fun onHideScreenLockChange(value: Boolean) {
        updateUiState {
            it.copy(hideScreenLock = value)
        }
        saveSettings()
    }

    fun onHideHomeScreenChange(value: Boolean) {
        updateUiState {
            it.copy(hideHomeScreen = value)
        }
        saveSettings()
    }

    fun onDynamicColorChange(value: Boolean) {
        updateUiState {
            it.copy(dynamicColor = value)
        }
        saveSettings()
    }

    fun onDayNightModeChange(dayNightMode: DayNightMode) {
        updateUiState {
            it.copy(dayNightMode = dayNightMode)
        }
        saveSettings()
    }

    private fun saveSettings() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                val uiState = uiState
                it.copy(
                    animationStyles = it.animationStyles.copy(isAnimationEnabled = uiState.showAnimation),
                    volumeButtonSwitchSong = uiState.volumeButtonSwitchSong,
                    fitSoftKeyboard = uiState.fitSoftKeyboard,
                    actionPanelAppLongPressLaunchPopup = uiState.actionPanelAppLongPressLaunchPopup,
                    actionPanelAppSwitchWindowModeDelayMs = uiState.actionPanelAppSwitchWindowModeDelayMs,
                    hideLandscape = uiState.hideLandscape,
                    hideQuickPanel = uiState.hideQuickPanel,
                    hideScreenLock = uiState.hideScreenLock,
                    hideHomeScreen = uiState.hideHomeScreen,
                    excludeFromRecents = uiState.excludeFromRecents,
                    dynamicColor = uiState.dynamicColor,
                    dayNightMode = uiState.dayNightMode
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder
                .advancedSettings
                .data
                .collectLatest { item ->
                    updateUiState {
                        it.copy(
                            showAnimation = item.animationStyles.isAnimationEnabled,
                            animationStyleType = item.animationStyles.type,
                            actionPanelStyleType = normalizeActionPanelStyleType(item.actionPanelStyles.type),
                            volumeButtonSwitchSong = item.volumeButtonSwitchSong,
                            fitSoftKeyboard = item.fitSoftKeyboard,
                            actionPanelAppLongPressLaunchPopup = item.actionPanelAppLongPressLaunchPopup,
                            actionPanelAppSwitchWindowModeDelayMs = item.actionPanelAppSwitchWindowModeDelayMs,
                            hideLandscape = item.hideLandscape,
                            hideQuickPanel = item.hideQuickPanel,
                            hideScreenLock = item.hideScreenLock,
                            hideHomeScreen = item.hideHomeScreen,
                            excludeFromRecents = item.excludeFromRecents,
                            dynamicColor = item.dynamicColor,
                            dayNightMode = item.dayNightMode
                        )
                    }
                }
        }
    }

    data class UiState(
        val volumeButtonSwitchSong: Boolean = false,
        val showAnimation: Boolean = false,
        val animationStyleType: Int = AnimationStyles.TYPE_WAVE,
        val actionPanelStyleType: Int = ActionPanelStyles.TYPE_FOLDER,
        val fitSoftKeyboard: Boolean = false,
        val actionPanelAppLongPressLaunchPopup: Boolean = false,
        val actionPanelAppSwitchWindowModeDelayMs: Long = 500L,
        val hideLandscape: Boolean = false,
        val hideQuickPanel: Boolean = false,
        val hideScreenLock: Boolean = false,
        val hideHomeScreen: Boolean = false,
        val excludeFromRecents: Boolean = false,
        val dynamicColor: Boolean = false,
        val dayNightMode: DayNightMode = DayNightMode.Auto,
        val showDynamicColorOption: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        val showDayNightModeDropdownMenu: Boolean = false
    )

    sealed interface UiEvent
}
