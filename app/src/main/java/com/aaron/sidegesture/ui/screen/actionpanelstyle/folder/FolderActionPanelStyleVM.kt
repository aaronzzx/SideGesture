package com.aaron.sidegesture.ui.screen.actionpanelstyle.folder

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.entity.FolderStyle
import com.aaron.sidegesture.ui.screen.actionpanelstyle.folder.FolderActionPanelStyleVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionpanelstyle.folder.FolderActionPanelStyleVM.UiState
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
class FolderActionPanelStyleVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun onItemSizeChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(itemSize = value.roundToInt()))
        }
    }

    fun onColumnsChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(columns = value.roundToInt()))
        }
    }

    fun onRowsChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(rows = value.roundToInt()))
        }
    }

    fun onCornerRadiusChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(cornerRadius = value.roundToInt()))
        }
    }

    fun onScrollSpeedChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(scrollSpeed = value.roundToInt()))
        }
    }

    fun onScrollHotZoneHeightChange(value: Float) {
        updateUiState {
            it.copy(style = it.style.copy(scrollHotZoneHeight = value.roundToInt()))
        }
    }

    fun saveSettings() {
        val payload = JsonHelper.encodeToString(uiState.style)
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                it.copy(
                    actionPanelStyles = it.actionPanelStyles.updateStyle(ActionPanelStyles.TYPE_FOLDER, payload)
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
                    val payload = advancedSettings.actionPanelStyles.payloadOf(ActionPanelStyles.TYPE_FOLDER)
                    updateUiState {
                        it.copy(
                            style = runCatching {
                                if (payload.isEmpty()) {
                                    FolderStyle()
                                } else {
                                    JsonHelper.decodeFromString<FolderStyle>(payload)
                                }
                            }.getOrDefault(FolderStyle())
                        )
                    }
                }
        }
    }

    data class UiState(
        val style: FolderStyle = FolderStyle()
    )

    sealed interface UiEvent
}
