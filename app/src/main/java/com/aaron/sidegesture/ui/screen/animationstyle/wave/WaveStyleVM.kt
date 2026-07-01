package com.aaron.sidegesture.ui.screen.animationstyle.wave

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ui.screen.animationstyle.wave.WaveStyleVM.UiEvent
import com.aaron.sidegesture.ui.screen.animationstyle.wave.WaveStyleVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.reflect.KCallable

/**
 * @author DS-Z
 * @since 2025/11/4
 */
class WaveStyleVM : BaseComposeVM<UiState, UiEvent>() {

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

    fun onWidthChange(width: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(width = width.toInt()))
        }
    }

    fun onLengthHalfRatioChange(ratio: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(bezierLengthHalfRatio = ratio))
        }
    }

    fun onSafeBoundsChange(value: Boolean) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(safeBounds = value))
        }
        saveSettings()
    }

    fun onTransformEnabledChange(value: Boolean) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(transformEnabled = value))
        }
        saveSettings()
    }

    fun onIconScaleChange(scale: Float) {
        updateUiState {
            it.copy(animationStyle = it.animationStyle.copy(iconScale = scale))
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
                    animationStyles = it.animationStyles.updateStyle(AnimationStyles.TYPE_WAVE, payload)
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
                    val payload = advancedSettings.animationStyles.payloadOf(AnimationStyles.TYPE_WAVE)
                    updateUiState {
                        val waveStyle = runCatching {
                            if (payload.isEmpty()) WaveStyle() else JsonHelper.decodeFromString<WaveStyle>(payload)
                        }.getOrDefault(WaveStyle())
                        it.copy(
                            animationStyle = waveStyle,
                            isCustomIconExpanded = it.isCustomIconExpanded || waveStyle.iconType != WaveStyle.ICON_TYPE_ARROW
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
        val animationStyle: WaveStyle = WaveStyle(),
        val isCustomIconExpanded: Boolean = false,
        val colorPickerDialog: Pair<Boolean, Int> = Pair(false, 0)
    )

    sealed interface UiEvent {

        data object ScrollToBottom : UiEvent
    }
}
