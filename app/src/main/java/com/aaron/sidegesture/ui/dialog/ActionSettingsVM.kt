package com.aaron.sidegesture.ui.dialog

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.ui.dialog.ActionSettingsVM.UiEvent
import com.aaron.sidegesture.ui.dialog.ActionSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author DS-Z
 * @since 2025/6/30
 */
class ActionSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun saveSettings() {
        viewModelScope.launchWithLoading {
            DataStoreHolder.actionSettings.updateData {
                uiState.actionSettings
            }
        }
    }

    fun onMoveScreenRateChange(rate: Float) {
        updateUiState {
            it.copy(
                actionSettings = it.actionSettings.copy(
                    moveScreen = it.actionSettings.moveScreen.copy(rate = rate)
                )
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.actionSettings.data.collectLatest { actionSettings ->
                updateUiState {
                    it.copy(actionSettings = actionSettings)
                }
            }
        }
    }

    data class UiState(
        val actionSettings: ActionSettings = ActionSettings()
    )

    sealed interface UiEvent
}