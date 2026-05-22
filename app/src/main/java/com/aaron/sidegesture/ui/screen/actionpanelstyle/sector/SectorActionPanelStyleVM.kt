package com.aaron.sidegesture.ui.screen.actionpanelstyle.sector

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.SectorStyle
import com.aaron.sidegesture.ui.screen.actionpanelstyle.sector.SectorActionPanelStyleVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionpanelstyle.sector.SectorActionPanelStyleVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * @author OpenAI
 * @since 2026/5/22
 */
class SectorActionPanelStyleVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun onItemSizeChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(itemSize = value.roundToInt()))
        }
    }

    fun onInitialRadiusRatioChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(initialRadiusRatio = value.roundToStep(0.05f)))
        }
    }

    fun onItemSpacingRatioChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(itemSpacingRatio = value.roundToStep(0.02f)))
        }
    }

    fun saveSettings() {
        val payload = JsonHelper.encodeToString(uiState.style)
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                it.copy(
                    actionPanelStyles = it.actionPanelStyles.updateStyle(ActionPanelStyles.TYPE_SECTOR, payload)
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
                    val actionPanelStyles = advancedSettings.actionPanelStyles
                    val payload = when (actionPanelStyles.type) {
                        ActionPanelStyles.TYPE_ARC -> actionPanelStyles.payloadOf(ActionPanelStyles.TYPE_ARC)
                        else -> actionPanelStyles.payloadOf(ActionPanelStyles.TYPE_SECTOR)
                    }
                    updateUiState {
                        val style = runCatching {
                            when (actionPanelStyles.type) {
                                ActionPanelStyles.TYPE_ARC -> {
                                    val arcStyle = if (payload.isEmpty()) {
                                        ArcStyle()
                                    } else {
                                        JsonHelper.decodeFromString<ArcStyle>(payload)
                                    }
                                    SectorStyle(itemSize = arcStyle.itemSize)
                                }

                                else -> {
                                    if (payload.isEmpty()) {
                                        SectorStyle()
                                    } else {
                                        JsonHelper.decodeFromString<SectorStyle>(payload)
                                    }
                                }
                            }
                        }.getOrDefault(SectorStyle())
                        it.copy(style = style)
                    }
                }
        }
    }

    data class UiState(
        val style: SectorStyle = SectorStyle()
    )

    sealed interface UiEvent
}

private fun Float.roundToStep(step: Float): Float {
    return (this / step).roundToInt() * step
}
