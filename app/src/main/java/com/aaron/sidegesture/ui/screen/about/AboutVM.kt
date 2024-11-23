package com.aaron.sidegesture.ui.screen.about

import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.ui.screen.about.AboutVM.UiEvent
import com.aaron.sidegesture.ui.screen.about.AboutVM.UiState

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class AboutVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    data class UiState(val value: Int = 0)

    sealed interface UiEvent
}