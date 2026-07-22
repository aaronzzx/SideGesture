package com.aaron.sidegesture.feature.gesture

import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.ktx.rootSize
import com.aaron.sidegesture.ktx.updateGestureButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopGestureInstrumentedTest {

    @Test
    fun topMapsDownLeftAndRightDirectionsAsBottomMirror() = runBlocking {
        assertGesture(Offset(0f, 120f), TriggerDirection.Center, "center")
        assertGesture(Offset(-120f, 0f), TriggerDirection.Up2, "left")
        assertGesture(Offset(120f, 0f), TriggerDirection.Down2, "right")
        assertGesture(Offset(-120f, 120f), TriggerDirection.Up, "left-down")
        assertGesture(Offset(120f, 120f), TriggerDirection.Down, "right-down")
    }

    @Test
    fun reverseUpwardMovementCannotTriggerTopAction() = runBlocking {
        val (direction, action) = performGesture(Offset(0f, -120f))

        assertEquals(Action.NONE, action)
        assertEquals(TriggerDirection.Down2, direction)
    }

    @Test
    fun topWindowLayoutParamsStayAtYZero() {
        val screen = rootSize
        val button = GestureButton(
            id = "top-window",
            position = Position.Top,
            enabled = true,
            start = 0.2f,
            end = 0.8f,
            width = 48,
            slideActions = GestureActions(),
            longSlideActions = GestureActions()
        )
        val layoutParams = WindowManager.LayoutParams()

        layoutParams.updateGestureButton(button)

        assertEquals((screen.width * 0.2f).toInt(), layoutParams.x)
        assertEquals(0, layoutParams.y)
        assertEquals((screen.width * 0.6f).toInt(), layoutParams.width)
        assertEquals(48, layoutParams.height)
    }

    private suspend fun assertGesture(
        drag: Offset,
        expectedDirection: TriggerDirection,
        expectedAction: String
    ) {
        val (direction, action) = performGesture(drag)
        assertEquals(expectedDirection, direction)
        assertEquals(Action(expectedAction), action)
    }

    private suspend fun performGesture(drag: Offset): Pair<TriggerDirection, Action> {
        val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
        val button = GestureButton(
            id = "top",
            position = Position.Top,
            enabled = true,
            start = 0f,
            end = 1f,
            width = 120,
            slideActions = GestureActions(
                center = listOf(Action("center")),
                up = listOf(Action("left-down")),
                down = listOf(Action("right-down")),
                up2 = listOf(Action("left")),
                down2 = listOf(Action("right"))
            )
        )
        val settings = GestureSettings(
            slideTriggerDistance = 30,
            vibrations = Vibrations(slideEnabled = false)
        )
        val state = withContext(Dispatchers.Main.immediate) {
            SideGestureState(scope, listOf(button), settings)
        }
        return try {
            withContext(Dispatchers.Main.immediate) {
                val origin = Offset(rootSize.width / 2f, 1f)
                state.onDragStart(origin, imePadding = 0)
                state.onDrag(drag)
                state.triggerDirection to state.onDragEnd()
            }
        } finally {
            withContext(Dispatchers.Main.immediate) { state.release() }
            scope.cancel()
        }
    }
}
