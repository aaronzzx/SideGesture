package com.aaron.sidegesture.ui.screen.gesturesettings

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.VibrationEffects
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class GestureSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun saveSettings() {
        viewModelScope.launch {
            val uiState = uiState
            launch {
                DataStoreHolder.gestureSettings.updateData {
                    GestureSettings(
                        slideTriggerDistance = uiState.slideTriggerDistance.toInt(),
                        longSlideTriggerImmediately = uiState.longSlideTriggerImmediately,
                        longSlideTriggerDistance = uiState.longSlideTriggerDistance.toInt(),
                        longSlideTriggerDelayMs = uiState.longSlideTriggerDelayMs,
                        isCustomVibration = uiState.isCustomVibration,
                        vibrations = uiState.vibrations
                    )
                }
            }
            launch {
                DataStoreHolder.sideGestureButtons.updateData {
                    it.toMutableList().apply {
                        forEachIndexed { index, button ->
                            val newButton = button.copy(
                                slideTriggerDistance = uiState.slideTriggerDistance.toInt(),
                                longSlideTriggerImmediately = uiState.longSlideTriggerImmediately,
                                longSlideTriggerDistance = uiState.longSlideTriggerDistance.toInt(),
                                longSlideTriggerDelayMs = uiState.longSlideTriggerDelayMs,
                                vibrations = uiState.vibrations
                            )
                            set(index, newButton)
                        }
                    }
                }
            }
            launch {
                DataStoreHolder.bottomGestureButtons.updateData {
                    it.toMutableList().apply {
                        forEachIndexed { index, button ->
                            val newButton = button.copy(
                                slideTriggerDistance = uiState.slideTriggerDistance.toInt(),
                                longSlideTriggerImmediately = uiState.longSlideTriggerImmediately,
                                longSlideTriggerDistance = uiState.longSlideTriggerDistance.toInt(),
                                longSlideTriggerDelayMs = uiState.longSlideTriggerDelayMs,
                                vibrations = uiState.vibrations
                            )
                            set(index, newButton)
                        }
                    }
                }
            }
        }
    }

    fun updatePredefinedVibration(effect: VibrationEffects) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(predefinedEffect = effect))
        }
        saveSettings()
    }

    fun showPredefinedVibrationDropdown(show: Boolean) {
        updateUiState {
            it.copy(showPredefinedVibrationDropdown = show)
        }
    }

    fun onCustomVibrationMsChange(value: Float) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(customVibrationMs = value.toLong()))
        }
    }

    fun onPressTriggerDistanceChange(value: Float) {
        updateUiState {
            it.copy(slideTriggerDistance = value)
        }
    }

    fun onLongPressTriggerDistanceChange(value: Float) {
        updateUiState {
            it.copy(longSlideTriggerDistance = value)
        }
    }

    fun onLongPressTriggerDelayMsChange(value: Float) {
        updateUiState {
            it.copy(longSlideTriggerDelayMs = value.toLong())
        }
    }

    fun onLongPressTriggerImmediatelyChange(value: Boolean) {
        updateUiState {
            it.copy(longSlideTriggerImmediately = value)
        }
        saveSettings()
    }

    fun onVibrateImmediatelyChange(value: Boolean) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(vibrateImmediately = value))
        }
        saveSettings()
    }

    fun onCustomVibrationChange(value: Boolean) {
        updateUiState {
            it.copy(isCustomVibration = value)
        }
        saveSettings()
        sendUiEvent(UiEvent.ScrollToBottom)
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureSettings.data.collectLatest { item ->
                updateUiState {
                    it.copy(
                        slideTriggerDistance = item.slideTriggerDistance.toFloat(),
                        longSlideTriggerImmediately = item.longSlideTriggerImmediately,
                        longSlideTriggerDistance = item.longSlideTriggerDistance.toFloat(),
                        longSlideTriggerDelayMs = item.longSlideTriggerDelayMs,
                        isCustomVibration = item.isCustomVibration,
                        vibrations = item.vibrations
                    )
                }
            }
        }
    }

    data class UiState(
        val slideTriggerDistance: Float = 0f,
        val longSlideTriggerImmediately: Boolean = true,
        val longSlideTriggerDistance: Float = 0f,
        val longSlideTriggerDelayMs: Long = 0L,
        val canShowPredefinedVibration: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        val isCustomVibration: Boolean = false,
        val vibrations: Vibrations = Vibrations(),
        val showPredefinedVibrationDropdown: Boolean = false
    )

    sealed interface UiEvent {

        data object ScrollToBottom : UiEvent
    }
}