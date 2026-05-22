package com.aaron.sidegesture.ui.screen.actionpanelstyle

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.entity.normalizeActionPanelStyleType
import com.aaron.sidegesture.ui.screen.actionpanelstyle.ActionPanelStyleSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionpanelstyle.ActionPanelStyleSelectVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author OpenAI
 * @since 2026/5/22
 */
class ActionPanelStyleSelectVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState(
        items = listOf(
            ActionPanelStyleItem(
                type = ActionPanelStyles.TYPE_FOLDER,
                nameRes = R.string.action_panel_style_folder,
                descriptionRes = R.string.action_panel_style_folder_desc,
                hasSettings = true
            ),
            ActionPanelStyleItem(
                type = ActionPanelStyles.TYPE_SECTOR,
                nameRes = R.string.action_panel_style_sector,
                descriptionRes = R.string.action_panel_style_sector_desc,
                hasSettings = true
            )
        )
    )

    init {
        loadData()
    }

    fun onStyleSelected(type: Int) {
        if (uiState.currentType == type && uiState.storedType == type) {
            return
        }
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData { advancedSettings ->
                advancedSettings.copy(
                    actionPanelStyles = advancedSettings.actionPanelStyles.selectType(type)
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
                    updateUiState {
                        it.copy(
                            storedType = advancedSettings.actionPanelStyles.type,
                            currentType = normalizeActionPanelStyleType(advancedSettings.actionPanelStyles.type)
                        )
                    }
                }
        }
    }

    data class UiState(
        val storedType: Int = ActionPanelStyles.TYPE_FOLDER,
        val currentType: Int = ActionPanelStyles.TYPE_FOLDER,
        val items: List<ActionPanelStyleItem> = emptyList()
    )

    data class ActionPanelStyleItem(
        val type: Int,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
        val hasSettings: Boolean
    )

    sealed interface UiEvent
}
