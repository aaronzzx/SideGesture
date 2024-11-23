package com.aaron.sidegesture.ui.screen.advancedsettings

import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsVM.UiState

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class AdvancedSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    data class UiState(val value: Int = 0)

    sealed interface UiEvent
}