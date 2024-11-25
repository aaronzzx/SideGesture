package com.aaron.sidegesture.ui.screen.advancedsettings

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class AdvancedSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.data.collectLatest { item ->
                updateUiState {
                    it.copy(advancedSettings = item)
                }
            }
        }
    }

    data class UiState(
        val advancedSettings: AdvancedSettings = AdvancedSettings()
    )

    sealed interface UiEvent
}