package com.aaron.sidegesture.defaults

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.component.UDFComponentDefaults
import com.aaron.compose.component.UiBaseEvent
import com.aaron.sidegesture.ktx.LocalNavController
import com.aaron.sidegesture.utils.showToast

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/1
 */

class UDFComponentDefaultsImpl : UDFComponentDefaults() {

    @Composable
    override fun <UiState : Any, UiEvent : Any> UDFComponent(
        component: UDFComponent<UiState, UiEvent>,
        activeState: Lifecycle.State,
        onBaseEvent: suspend (baseEvent: Any) -> Boolean,
        onEvent: suspend (event: UiEvent) -> Unit,
        content: @Composable (state: UiState) -> Unit
    ) {
        val navController = LocalNavController.current
        UDFComponentImpl(
            component = component,
            activeState = activeState,
            onBaseEvent = onBaseEvent.takeIf {
                it != this.onBaseEvent
            } ?: { baseEvent ->
                when (baseEvent) {
                    is UiBaseEvent.Finish -> navController.navigateUp()
                    is UiBaseEvent.ResToast -> showToast(baseEvent.res)
                    is UiBaseEvent.StringToast -> showToast(baseEvent.text)
                }
                true
            },
            onEvent = onEvent,
            content = content
        )
    }
}