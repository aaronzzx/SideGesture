package com.aaron.sidegesture.ui.screen.animationstyle.capsule

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.CapsuleStyle
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ui.screen.animationstyle.capsule.CapsuleStyleVM.UiEvent
import com.aaron.sidegesture.ui.screen.animationstyle.capsule.CapsuleStyleVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.reflect.KCallable

/**
 * @author OpenAI
 * @since 2026/5/20
 */
class CapsuleStyleVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    val colorPickerDialog = ColorPickerDialog()

    init {
        loadData()
    }

    fun onCustomIconExpandedChange(isExpanded: Boolean) {
        updateUiState {
            it.copy(isCustomIconExpanded = isExpanded)
        }
        sendUiEvent(UiEvent.ScrollToBottom)
    }

    fun onStrokeWidthChange(width: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(strokeWidth = width.toInt()))
        }
    }

    fun onThicknessChange(value: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(thickness = value.toInt()))
        }
    }

    fun onMaxLengthChange(value: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(maxLength = value.toInt()))
        }
    }

    fun onCornerRadiusChange(value: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(cornerRadius = value.toInt()))
        }
    }

    fun onIconScaleChange(value: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(iconScale = value))
        }
    }

    fun onIconTypeChange(iconType: Int) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(iconType = iconType))
        }
        saveSettings()
    }

    fun saveSettings() {
        val payload = JsonHelper.encodeToString(uiState.animationStyle)
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                it.copy(
                    animationStyles = it.animationStyles.updateStyle(AnimationStyles.TYPE_CAPSULE, payload)
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder
                .advancedSettings
                .data
                .take(1)
                .collectLatest { advancedSettings ->
                    val payload = advancedSettings.animationStyles.payloadOf(AnimationStyles.TYPE_CAPSULE)
                    updateUiState {
                        val animationStyle = runCatching {
                            if (payload.isEmpty()) {
                                CapsuleStyle()
                            } else {
                                JsonHelper.decodeFromString<CapsuleStyle>(payload)
                            }
                        }.getOrDefault(CapsuleStyle())
                        it.copy(
                            animationStyle = animationStyle,
                            isCustomIconExpanded = it.isCustomIconExpanded ||
                                animationStyle.iconType != WaveStyle.ICON_TYPE_ARROW
                        )
                    }
                }
        }
    }

    inner class ColorPickerDialog {

        private var belongsTo: KCallable<Any>? = null

        fun show(
            show: Boolean,
            color: Int = 0,
            belongsTo: KCallable<Any>? = null
        ) {
            this.belongsTo = belongsTo
            updateUiState {
                it.copy(colorPickerDialog = Pair(show, color))
            }
        }

        fun onColorChange(color: Int) {
            updateUiState {
                it.copy(colorPickerDialog = it.colorPickerDialog.copy(second = color))
            }
        }

        fun confirm() {
            updateUiState {
                val pickedColor = it.colorPickerDialog.second
                it.copy(
                    colorPickerDialog = it.colorPickerDialog.copy(first = false),
                    animationStyle = it.animationStyle.copy(
                        backgroundColor = when (belongsTo == it.animationStyle::backgroundColor) {
                            true -> pickedColor
                            else -> it.animationStyle.backgroundColor
                        },
                        strokeColor = when (belongsTo == it.animationStyle::strokeColor) {
                            true -> pickedColor
                            else -> it.animationStyle.strokeColor
                        },
                        iconColor = when (belongsTo == it.animationStyle::iconColor) {
                            true -> pickedColor
                            else -> it.animationStyle.iconColor
                        }
                    )
                )
            }
            belongsTo = null
            saveSettings()
        }
    }

    data class UiState(
        val animationStyle: CapsuleStyle = CapsuleStyle(),
        val isCustomIconExpanded: Boolean = false,
        val colorPickerDialog: Pair<Boolean, Int> = Pair(false, 0)
    )

    sealed interface UiEvent {

        data object ScrollToBottom : UiEvent
    }
}
