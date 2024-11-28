package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonLength
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonStart
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.fraction
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/28
 */
class GestureButtonSettingsVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    private val gestureButtonSettings = savedStateHandle.toRoute<GestureButtonSettings>()

    val colorPickerDialog = ColorPickerDialog()

    init {
        loadData()
    }

    fun onGestureButtonWidthChange(width: Float) {
        updateUiState {
            it.copy(gestureButton = it.gestureButton?.copy(width = width.toInt()))
        }
        saveSettings()
    }

    fun onGestureButtonLengthChange(fraction: Float) {
        updateUiState {
            it.copy(
                gestureButton = it.gestureButton?.let { b ->
                    val start = b.start
                    val end = b.end
                    val newEnd = start + fraction
                    if (newEnd > MaxGestureButtonLength) {
                        val residue = newEnd - MaxGestureButtonLength
                        val newStart = end - b.fraction - residue
                        return@let b.copy(start = newStart, end = newEnd)
                    }
                    b.copy(start = start, end = newEnd)
                }
            )
        }
    }

    fun onGestureButtonLocationChange(value: Float) {
        updateUiState {
            it.copy(
                gestureButton = it.gestureButton?.let { b ->
                    val startValue = MaxGestureButtonStart - value
                    val fraction = b.fraction
                    val end = startValue + fraction
                    if (end >= MaxGestureButtonLength) {
                        return@let b
                    }
                    b.copy(start = startValue, end = end)
                }
            )
        }
    }

    fun onGestureButtonAlignChange(value: Boolean) {
        updateUiState {
            it.copy(alignRegion = value)
        }
    }

    fun saveSettings() {
        val curButton = uiState.gestureButton ?: return
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.updateData {
                val list = it.toMutableList()
                for (index in list.indices) {
                    val button = list[index]
                    if (button.id == curButton.id) {
                        if (button.position == curButton.position) {
                            list[index] = curButton
                        } else if (uiState.alignRegion) {
                            list[index] = button.copy(
                                width = curButton.width,
                                start = curButton.start,
                                end = curButton.end,
                                color = curButton.color
                            )
                        }
                    }
                }
                list
            }
        }
    }

    inner class ColorPickerDialog {

        fun show(show: Boolean) {
            updateUiState {
                val color = it.gestureButton?.let { b -> Color(b.color) } ?: it.colorPickerDialog.second
                it.copy(
                    colorPickerDialog = it.colorPickerDialog.copy(first = show, second = color)
                )
            }
        }

        fun onColorChange(color: Color) {
            updateUiState {
                it.copy(colorPickerDialog = it.colorPickerDialog.copy(second = color))
            }
        }

        fun confirm() {
            updateUiState {
                val pickedColor = it.colorPickerDialog.second
                it.copy(gestureButton = it.gestureButton?.copy(color = pickedColor.toArgb()))
            }
            saveSettings()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.data.collectLatest { items ->
                val gestureButtonSettings = gestureButtonSettings
                val buttonId = gestureButtonSettings.buttonId
                val position = gestureButtonSettings.position
                val filtered = items.filter { it.id == buttonId }
                updateUiState {
                    it.copy(gestureButton = filtered.find { b -> b.position == position })
                }
            }
        }
    }

    data class UiState(
        val gestureButton: GestureButton? = null,
        val alignRegion: Boolean = true,
        val colorPickerDialog: Pair<Boolean, Color> = Pair(false, Color.Red.copy(alpha = 0.1f))
    )

    sealed interface UiEvent
}