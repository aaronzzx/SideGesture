package com.aaron.sidegesture.ui.screen.gesturesettings

import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettingsVM.UiState

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class GestureSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    data class UiState(val value: Int = 0)

    sealed interface UiEvent
}