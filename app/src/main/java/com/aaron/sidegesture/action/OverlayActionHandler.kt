package com.aaron.sidegesture.action

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

interface OverlayActionHandler : ActionHandler, OverlayDismissAware {

    val touchEnabled: Flow<Boolean>

    @Composable
    fun Content()
}
