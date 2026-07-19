package com.aaron.sidegesture.feature.quicktools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.Position

class QuickToolsControlCenterState(
    brightnessGateway: QuickToolsBrightnessGateway
) {

    private val brightnessController = QuickToolsBrightnessController(brightnessGateway)

    var visible: Boolean by mutableStateOf(false)
        private set

    var fingerAnchor: Offset by mutableStateOf(Offset.Unspecified)
        private set

    var triggerEdge: Position by mutableStateOf(Position.Left)
        private set

    var refreshTick: Int by mutableIntStateOf(0)
        private set

    val brightnessRatio: Float
        get() = brightnessController.displayedRatio

    val brightnessAutoEnabled: Boolean
        get() = brightnessController.snapshot.autoEnabled

    val brightnessCanWrite: Boolean
        get() = brightnessController.snapshot.writeCapability.canWrite

    fun show(anchor: Offset, edge: Position) {
        fingerAnchor = anchor
        triggerEdge = edge
        brightnessController.start()
        visible = true
        refresh()
    }

    fun hide() {
        brightnessController.stop()
        visible = false
    }

    suspend fun setBrightnessRatio(ratio: Float): QuickToolsOperationResult {
        return brightnessController.setRatio(ratio)
    }

    suspend fun toggleBrightnessAuto(): QuickToolsOperationResult {
        return brightnessController.toggleAuto()
    }

    fun refresh() {
        refreshTick++
    }
}

@Composable
fun rememberQuickToolsControlCenterState(
    service: SideGestureService
): QuickToolsControlCenterState {
    return remember(service) {
        QuickToolsControlCenterState(QuickToolsExecutor.brightnessGateway(service))
    }
}
