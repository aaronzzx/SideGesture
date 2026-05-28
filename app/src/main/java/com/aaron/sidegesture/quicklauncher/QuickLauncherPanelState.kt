package com.aaron.sidegesture.quicklauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.Position

class QuickLauncherPanelState {

    var visible: Boolean by mutableStateOf(false)
        private set

    var items: List<Action> by mutableStateOf(emptyList())
        private set

    var fingerAnchor: Offset by mutableStateOf(Offset.Unspecified)
        private set

    var triggerEdge: Position by mutableStateOf(Position.Left)
        private set

    var interactionTick: Int by mutableIntStateOf(0)
        private set

    fun show(items: List<Action>, anchor: Offset, edge: Position) {
        this.items = items
        this.fingerAnchor = anchor
        this.triggerEdge = edge
        visible = true
        markInteraction()
    }

    fun hide() {
        visible = false
    }

    fun markInteraction() {
        interactionTick++
    }
}

@Composable
fun rememberQuickLauncherPanelState(): QuickLauncherPanelState {
    return remember { QuickLauncherPanelState() }
}
