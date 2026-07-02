package com.aaron.sidegesture.feature.quicktools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.entity.Position

class QuickToolsControlCenterState {

    var visible: Boolean by mutableStateOf(false)
        private set

    var fingerAnchor: Offset by mutableStateOf(Offset.Unspecified)
        private set

    var triggerEdge: Position by mutableStateOf(Position.Left)
        private set

    var refreshTick: Int by mutableIntStateOf(0)
        private set

    fun show(anchor: Offset, edge: Position) {
        fingerAnchor = anchor
        triggerEdge = edge
        visible = true
        refresh()
    }

    fun hide() {
        visible = false
    }

    fun refresh() {
        refreshTick++
    }
}

@Composable
fun rememberQuickToolsControlCenterState(): QuickToolsControlCenterState {
    return remember { QuickToolsControlCenterState() }
}
