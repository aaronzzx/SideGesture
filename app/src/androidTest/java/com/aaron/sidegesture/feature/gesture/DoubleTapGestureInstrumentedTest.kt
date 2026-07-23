package com.aaron.sidegesture.feature.gesture

import android.view.ViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.ktx.rootSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoubleTapGestureInstrumentedTest {

    @Test
    fun allFourEdgesUseTheSameDoubleTapStateMachine() = runBlocking {
        Position.entries.forEach { position ->
            withState(
                buttons = listOf(button(position)),
                gestureSettings = gestureSettings()
            ) { state, dispatched ->
                val offset = tapOffset(position)

                assertEquals(Action.NONE, tap(state, offset))
                delay(40L)
                assertEquals(Action.NONE, tap(state, offset))

                assertEquals(listOf("double-${position.name}"), dispatched.map { it.value })
            }
        }
    }

    @Test
    fun unconfiguredDoubleTapKeepsSingleTapImmediate() = runBlocking {
        val position = Position.Left
        val singleOnlyButton = button(position).let { button ->
            button.copy(
                slideActions = button.slideActions.copy(doubleClick = emptyList())
            )
        }
        withState(
            buttons = listOf(singleOnlyButton),
            gestureSettings = gestureSettings()
        ) { state, dispatched ->
            assertEquals(Action("single-Left"), tap(state, tapOffset(position)))
            delay(ViewConfiguration.getDoubleTapTimeout().toLong() + 50L)
            assertTrue(dispatched.isEmpty())
        }
    }

    @Test
    fun timeoutDispatchesSingleTapOnce() = runBlocking {
        val position = Position.Bottom
        withState(
            buttons = listOf(button(position)),
            gestureSettings = gestureSettings()
        ) { state, dispatched ->
            assertEquals(Action.NONE, tap(state, tapOffset(position)))

            delay(ViewConfiguration.getDoubleTapTimeout().toLong() + 80L)

            assertEquals(listOf("single-Bottom"), dispatched.map { it.value })
        }
    }

    @Test
    fun crossButtonCancelsOldCandidateAndTreatsNewButtonAsFirstTap() = runBlocking {
        val buttons = listOf(button(Position.Left), button(Position.Right))
        withState(
            buttons = buttons,
            gestureSettings = gestureSettings()
        ) { state, dispatched ->
            assertEquals(Action.NONE, tap(state, tapOffset(Position.Left)))
            delay(40L)
            assertEquals(Action.NONE, tap(state, tapOffset(Position.Right)))

            delay(ViewConfiguration.getDoubleTapTimeout().toLong() + 80L)

            assertEquals(listOf("single-Right"), dispatched.map { it.value })
        }
    }

    @Test
    fun movementCancelAndReleasePreventDeferredSingleTap() = runBlocking {
        val position = Position.Left
        val settings = gestureSettings()
        withState(listOf(button(position)), settings) { state, dispatched ->
            val offset = tapOffset(position)
            assertEquals(Action.NONE, tap(state, offset))
            withContext(Dispatchers.Main.immediate) {
                state.onDragStart(offset, imePadding = 0)
                state.onDrag(Offset(80f, 0f))
                state.onDragEnd()
            }
            delay(ViewConfiguration.getDoubleTapTimeout().toLong() + 80L)
            assertTrue(dispatched.isEmpty())

            assertEquals(Action.NONE, tap(state, offset))
            withContext(Dispatchers.Main.immediate) {
                state.onDragCancel()
            }
            delay(ViewConfiguration.getDoubleTapTimeout().toLong() + 80L)
            assertTrue(dispatched.isEmpty())

            assertEquals(Action.NONE, tap(state, offset))
            withContext(Dispatchers.Main.immediate) {
                state.release()
            }
            delay(ViewConfiguration.getDoubleTapTimeout().toLong() + 80L)
            assertTrue(dispatched.isEmpty())
        }
    }

    @Test
    fun longPressCancelsPendingDoubleTap() = runBlocking {
        val position = Position.Left
        val longPressAction = Action("long-press")
        val configuredButton = button(position).let { button ->
            button.copy(
                slideActions = button.slideActions.copy(center2 = listOf(longPressAction))
            )
        }
        val settings = gestureSettings().copy(
            longPressTriggerDelayMs = 30L
        )
        withState(listOf(configuredButton), settings) { state, dispatched ->
            val longPresses = mutableListOf<Action>()
            state.onLongPress = { action ->
                longPresses += action
                state.cancel()
            }
            val offset = tapOffset(position)
            assertEquals(Action.NONE, tap(state, offset))

            withContext(Dispatchers.Main.immediate) {
                state.onDragStart(offset, imePadding = 0)
            }
            delay(80L)
            delay(ViewConfiguration.getDoubleTapTimeout().toLong() + 50L)

            assertEquals(listOf(longPressAction), longPresses)
            assertTrue(dispatched.isEmpty())
        }
    }

    @Test
    fun pendingTapKeepsOriginalActionsAcrossConfigurationChanges() = runBlocking {
        val position = Position.Left
        val originalButton = button(position)
        withState(
            buttons = listOf(originalButton),
            gestureSettings = gestureSettings()
        ) { state, dispatched ->
            val offset = tapOffset(position)
            assertEquals(Action.NONE, tap(state, offset))

            state.updateConfiguration(
                buttons = listOf(
                    originalButton.copy(
                        slideActions = originalButton.slideActions.copy(
                            click = listOf(Action("new-single")),
                            doubleClick = listOf(Action("new-double"))
                        )
                    )
                ),
                gestureSettings = gestureSettings()
            )
            delay(40L)
            assertEquals(Action.NONE, tap(state, offset))

            assertEquals(listOf("double-Left"), dispatched.map { it.value })
        }
    }

    private suspend fun tap(state: SideGestureState, offset: Offset): Action {
        return withContext(Dispatchers.Main.immediate) {
            state.onDragStart(offset, imePadding = 0)
            state.onDragEnd()
        }
    }

    private suspend fun withState(
        buttons: List<GestureButton>,
        gestureSettings: GestureSettings,
        block: suspend (SideGestureState, MutableList<Action>) -> Unit
    ) {
        val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
        val state = withContext(Dispatchers.Main.immediate) {
            SideGestureState(scope, buttons, gestureSettings)
        }
        val dispatched = mutableListOf<Action>()
        state.onTapDispatch = { action, _, _ -> dispatched += action }
        try {
            block(state, dispatched)
        } finally {
            withContext(Dispatchers.Main.immediate) {
                state.release()
            }
            scope.cancel()
        }
    }

    private fun gestureSettings(): GestureSettings {
        return GestureSettings(
            longPressTriggerDelayMs = 1_000L,
            vibrations = Vibrations(slideEnabled = false)
        )
    }

    private fun button(position: Position): GestureButton {
        return GestureButton(
            id = position.name,
            position = position,
            enabled = true,
            start = 0f,
            end = 1f,
            width = 120,
            slideActions = GestureActions(
                click = listOf(Action("single-${position.name}")),
                doubleClick = listOf(Action("double-${position.name}"))
            )
        )
    }

    private fun tapOffset(position: Position): Offset {
        val size = rootSize
        return when (position) {
            Position.Left -> Offset(1f, size.height / 2f)
            Position.Right -> Offset(size.width - 1f, size.height / 2f)
            Position.Bottom -> Offset(size.width / 2f, size.height - 1f)
            Position.Top -> Offset(size.width / 2f, 1f)
        }
    }
}
