package com.aaron.sidegesture.ui.screen.gesturesettings

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
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

    fun updatePredefinedVibration(effect: Int) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(predefinedEffect = effect))
        }
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
            it.copy(pressTriggerDistance = value)
        }
    }

    fun onLongPressTriggerDistanceChange(value: Float) {
        updateUiState {
            it.copy(longPressTriggerDistance = value)
        }
    }

    fun onLongPressTriggerDelayMsChange(value: Float) {
        updateUiState {
            it.copy(longPressTriggerDelayMs = value.toLong())
        }
    }

    fun onLongPressTriggerImmediatelyChange(value: Boolean) {
        updateUiState {
            it.copy(longPressTriggerImmediately = value)
        }
    }

    fun onVibrateImmediatelyChange(value: Boolean) {
        updateUiState {
            it.copy(vibrations = it.vibrations.copy(vibrateImmediately = value))
        }
    }

    fun onCustomVibrationChange(value: Boolean) {
        updateUiState {
            it.copy(isCustomVibration = value)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureSettings.data.collectLatest { item ->
                updateUiState {
                    it.copy(
                        pressTriggerDistance = item.pressTriggerDistance.toFloat(),
                        longPressTriggerImmediately = item.longPressTriggerImmediately,
                        longPressTriggerDistance = item.longPressTriggerDistance.toFloat(),
                        longPressTriggerDelayMs = item.longPressTriggerDelayMs,
                        isCustomVibration = item.isCustomVibration,
                        vibrations = item.vibrations
                    )
                }
            }
        }
    }

    data class UiState(
        val pressTriggerDistance: Float = 0f,
        val longPressTriggerImmediately: Boolean = true,
        val longPressTriggerDistance: Float = 0f,
        val longPressTriggerDelayMs: Long = 0L,
        val isCustomVibration: Boolean = false,
        val vibrations: Vibrations = Vibrations(),
        val showPredefinedVibrationDropdown: Boolean = false
    )

    sealed interface UiEvent
}