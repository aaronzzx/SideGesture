package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiState

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class GestureButtonSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    data class UiState(val value: Int = 0)

    sealed interface UiEvent
}