package com.aaron.sidegesture.ui.screen.animationstyle

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ui.screen.animationstyle.AnimationStyleSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.animationstyle.AnimationStyleSelectVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2026/5/20
 */
class AnimationStyleSelectVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState(
        items = listOf(
            AnimationStyleItem(
                type = AnimationStyles.TYPE_WAVE,
                nameRes = R.string.animation_style_wave,
                descriptionRes = R.string.animation_style_wave_desc,
                hasSettings = true
            )
        )
    )

    init {
        loadData()
    }

    fun onStyleSelected(type: Int) {
        if (uiState.currentType == type) {
            return
        }
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData { advancedSettings ->
                advancedSettings.copy(
                    animationStyles = advancedSettings.animationStyles.copy(
                        type = type,
                        json = when (type) {
                            AnimationStyles.TYPE_WAVE -> when (advancedSettings.animationStyles.type) {
                                AnimationStyles.TYPE_WAVE -> advancedSettings.animationStyles.json
                                else -> ""
                            }
                            else -> advancedSettings.animationStyles.json
                        }
                    )
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder
                .advancedSettings
                .data
                .collectLatest { advancedSettings ->
                    val waveStyle = when (advancedSettings.animationStyles.type) {
                        AnimationStyles.TYPE_WAVE -> runCatching {
                            advancedSettings.animationStyles.value as WaveStyle
                        }.getOrDefault(WaveStyle())
                        else -> WaveStyle()
                    }
                    updateUiState {
                        it.copy(
                            currentType = advancedSettings.animationStyles.type,
                            waveStyle = waveStyle
                        )
                    }
                }
        }
    }

    data class UiState(
        val currentType: Int = AnimationStyles.TYPE_WAVE,
        val items: List<AnimationStyleItem> = emptyList(),
        val waveStyle: WaveStyle = WaveStyle()
    )

    data class AnimationStyleItem(
        val type: Int,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        val hasSettings: Boolean
    )

    sealed interface UiEvent
}
