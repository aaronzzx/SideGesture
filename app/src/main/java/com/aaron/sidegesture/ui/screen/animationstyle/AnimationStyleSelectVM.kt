package com.aaron.sidegesture.ui.screen.animationstyle

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.CapsuleStyle
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ui.screen.animationstyle.AnimationStyleSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.animationstyle.AnimationStyleSelectVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.JsonHelper
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
            ),
            AnimationStyleItem(
                type = AnimationStyles.TYPE_CAPSULE,
                nameRes = R.string.animation_style_capsule,
                descriptionRes = R.string.animation_style_capsule_desc,
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
                    animationStyles = advancedSettings.animationStyles.selectType(type)
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
                    val animationStyles = advancedSettings.animationStyles
                    val waveStyle = runCatching {
                        val payload = animationStyles.payloadOf(AnimationStyles.TYPE_WAVE)
                        if (payload.isEmpty()) WaveStyle() else JsonHelper.decodeFromString<WaveStyle>(payload)
                    }.getOrDefault(WaveStyle())
                    val capsuleStyle = runCatching {
                        val payload = animationStyles.payloadOf(AnimationStyles.TYPE_CAPSULE)
                        if (payload.isEmpty()) CapsuleStyle() else JsonHelper.decodeFromString<CapsuleStyle>(payload)
                    }.getOrDefault(CapsuleStyle())
                    updateUiState {
                        it.copy(
                            currentType = advancedSettings.animationStyles.type,
                            waveStyle = waveStyle,
                            capsuleStyle = capsuleStyle
                        )
                    }
                }
        }
    }

    data class UiState(
        val currentType: Int = AnimationStyles.TYPE_WAVE,
        val items: List<AnimationStyleItem> = emptyList(),
        val waveStyle: WaveStyle = WaveStyle(),
        val capsuleStyle: CapsuleStyle = CapsuleStyle()
    )

    data class AnimationStyleItem(
        val type: Int,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        val hasSettings: Boolean
    )

    sealed interface UiEvent
}
