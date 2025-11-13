package com.aaron.sidegesture.ui.screen.gesturesettings

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.VibrationEffects
import com.aaron.sidegesture.entity.Vibrations
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
                    it.copy(
                        slideTriggerDistance = uiState.slideTriggerDistance.toInt(),
                        longPressTriggerDelayMs = uiState.longPressTriggerDelayMs,
                        longSlideTriggerImmediately = uiState.longSlideTriggerImmediately,
                        longSlideTriggerDistance = uiState.longSlideTriggerDistance.toInt(),
                        longSlideTriggerDelayMs = uiState.longSlideTriggerDelayMs,
                        isCustomVibration = uiState.isCustomVibration,
                        vibrations = uiState.vibrations,
                        isPreciseSlideType = uiState.isPreciseSlideTypeEnabled
                    )
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

    fun onPreciseSlideTypeChange(value: Boolean) {
        updateUiState {
            it.copy(isPreciseSlideTypeEnabled = value)
        }
        saveSettings()
    }

    fun onCustomVibrationMsChange(value: Float) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(customVibrationMs = value.toLong()))
        }
    }

    fun onSlideTriggerDistanceChange(value: Float) {
        updateUiState {
            it.copy(slideTriggerDistance = value)
        }
    }

    fun onLongPressTriggerDelayMsChange(value: Float) {
        updateUiState {
            it.copy(longPressTriggerDelayMs = value.toLong())
        }
    }

    fun onLongSlideTriggerDistanceChange(value: Float) {
        updateUiState {
            it.copy(longSlideTriggerDistance = value)
        }
    }

    fun onLongSlideTriggerDelayMsChange(value: Float) {
        updateUiState {
            it.copy(longSlideTriggerDelayMs = value.toLong())
        }
    }

    fun onLongSlideTriggerImmediatelyChange(value: Boolean) {
        updateUiState {
            it.copy(longSlideTriggerImmediately = value)
        }
        saveSettings()
    }

    fun onVibrateForSlide(value: Boolean) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(slideEnabled = value))
        }
        saveSettings()
    }

    fun onVibrateForLongSlide(value: Boolean) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(longSlideEnabled = value))
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
            DataStoreHolder
                .gestureSettings
                .data
                .collectLatest { item ->
                    updateUiState {
                        it.copy(
                            slideTriggerDistance = item.slideTriggerDistance.toFloat(),
                            longPressTriggerDelayMs = item.longPressTriggerDelayMs,
                            longSlideTriggerImmediately = item.longSlideTriggerImmediately,
                            longSlideTriggerDistance = item.longSlideTriggerDistance.toFloat(),
                            longSlideTriggerDelayMs = item.longSlideTriggerDelayMs,
                            isCustomVibration = item.isCustomVibration,
                            vibrations = item.vibrations,
                            isPreciseSlideTypeEnabled = item.isPreciseSlideType
                        )
                    }
                }
        }
    }

    data class UiState(
        val slideTriggerDistance: Float = 0f,
        val longPressTriggerDelayMs: Long = 0L,
        val longSlideTriggerImmediately: Boolean = true,
        val longSlideTriggerDistance: Float = 0f,
        val longSlideTriggerDelayMs: Long = 0L,
        val canShowPredefinedVibration: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        val isCustomVibration: Boolean = false,
        val vibrations: Vibrations = Vibrations(),
        val showPredefinedVibrationDropdown: Boolean = false,
        val isPreciseSlideTypeEnabled: Boolean = false
    )

    sealed interface UiEvent {

        data object ScrollToBottom : UiEvent
    }
}