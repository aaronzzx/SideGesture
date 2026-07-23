package com.aaron.sidegesture.feature.movescreen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.aaron.sidegesture.entity.MoveScreenData
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.DoubleTap
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.LongPress
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.Tap
import com.aaron.sidegesture.utils.JsonHelper
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveScreenStateTest {

    private val states = mutableListOf<MoveScreenState>()
    private val hoverDelays = mutableListOf<ManualHoverDelay>()

    @After
    fun tearDown() {
        states.forEach { it.onDragCancel() }
        hoverDelays.forEach { it.resumeAll() }
    }

    @Test
    fun hoverCapturesLatestTargetAndFreezesItWhileSelecting() {
        val hoverDelay = ManualHoverDelay()
        val state = createState(hoverDelay)
        state.onDragStart(Offset(100f, 200f))

        state.onDrag(Offset(10f, 0f))
        state.onDrag(Offset(5f, 0f))

        assertEquals(MoveScreenPhase.HoverPending, state.phase)
        hoverDelay.resumeNext()

        assertEquals(MoveScreenPhase.Selecting, state.phase)
        assertEquals(Offset(145f, 200f), state.displayFingerOnScreen)
        assertEquals(Offset(115f, 200f), state.popupAnchor)
        state.updateActionPopupBounds(Rect(50f, 100f, 200f, 300f))

        state.onDrag(Offset(10f, 10f))

        assertEquals(Offset(145f, 200f), state.displayFingerOnScreen)
        assertEquals(Offset(125f, 210f), state.finger)
    }

    @Test
    fun leavingPopupRestoresWithoutJumpAndCanHoverAgain() {
        val hoverDelay = ManualHoverDelay()
        val state = createState(hoverDelay)
        state.onDragStart(Offset(100f, 200f))
        state.onDrag(Offset(10f, 0f))
        hoverDelay.resumeNext()
        state.updateActionPopupBounds(Rect(50f, 100f, 200f, 300f))
        val firstTarget = state.displayFingerOnScreen

        state.onDrag(Offset(100f, 0f))

        assertEquals(MoveScreenPhase.HoverPending, state.phase)
        assertFalse(state.showMoveScreenActionPopup)
        assertNull(state.pendingAction)
        assertEquals(firstTarget, state.displayFingerOnScreen)

        state.onDrag(Offset(5f, 0f))

        val resumedTarget = firstTarget + Offset(15f, 0f)
        assertEquals(resumedTarget, state.displayFingerOnScreen)
        hoverDelay.resumeNext()

        assertEquals(MoveScreenPhase.Selecting, state.phase)
        assertEquals(resumedTarget, state.displayFingerOnScreen)
        assertEquals(Offset(215f, 200f), state.popupAnchor)
    }

    @Test
    fun latePopupBoundsCloseAfterPointerAlreadyMovedOutside() {
        val hoverDelay = ManualHoverDelay()
        val state = createState(hoverDelay)
        state.onDragStart(Offset(100f, 200f))
        state.onDrag(Offset(10f, 0f))
        hoverDelay.resumeNext()
        val frozenTarget = state.displayFingerOnScreen

        state.onDrag(Offset(200f, 0f))
        assertTrue(state.showMoveScreenActionPopup)

        state.updateActionPopupBounds(Rect(50f, 100f, 200f, 300f))

        assertEquals(MoveScreenPhase.HoverPending, state.phase)
        assertFalse(state.showMoveScreenActionPopup)
        assertEquals(frozenTarget, state.displayFingerOnScreen)
    }

    @Test
    fun lateActionBoundsSelectPointerMoveThatArrivedBeforeLayout() {
        val hoverDelay = ManualHoverDelay()
        var selectionFeedbackCount = 0
        val state = createState(hoverDelay) { selectionFeedbackCount++ }
        state.onDragStart(Offset(100f, 200f))
        state.onDrag(Offset(10f, 0f))
        hoverDelay.resumeNext()

        state.onDrag(Offset(40f, -75f))
        assertNull(state.pendingAction)

        state.updateActionPopupBounds(Rect(50f, 100f, 250f, 300f))
        state.updateActionBounds(Tap, Rect(100f, 100f, 200f, 150f))

        assertEquals(Tap, state.pendingAction)
        assertEquals(1, selectionFeedbackCount)
    }

    @Test
    fun menuSelectionVibratesOnChangeAndDoneAlwaysHasAnAction() {
        val hoverDelay = ManualHoverDelay()
        var selectionFeedbackCount = 0
        val state = createState(hoverDelay) { selectionFeedbackCount++ }
        state.onDragStart(Offset(100f, 200f))
        state.onDrag(Offset(10f, 0f))
        hoverDelay.resumeNext()
        state.updateActionPopupBounds(Rect(50f, 100f, 250f, 300f))

        val fallback = decodeMoveScreenData(state)

        assertEquals(Tap, fallback.action)
        assertEquals(130, fallback.x)
        assertEquals(200, fallback.y)

        state.updateActionBounds(Tap, Rect(100f, 100f, 200f, 150f))
        state.updateActionBounds(DoubleTap, Rect(100f, 150f, 200f, 200f))
        state.updateActionBounds(LongPress, Rect(100f, 200f, 200f, 250f))

        state.onDrag(Offset(40f, -75f))
        assertEquals(Tap, state.pendingAction)
        assertEquals(1, selectionFeedbackCount)

        state.onDrag(Offset.Zero)
        assertEquals(Tap, state.pendingAction)
        assertEquals(1, selectionFeedbackCount)

        state.onDrag(Offset(0f, 50f))
        assertEquals(DoubleTap, state.pendingAction)
        assertEquals(2, selectionFeedbackCount)
        assertEquals(DoubleTap, decodeMoveScreenData(state).action)

        state.onDrag(Offset(0f, 50f))
        assertEquals(LongPress, state.pendingAction)
        assertEquals(3, selectionFeedbackCount)
        assertEquals(LongPress, decodeMoveScreenData(state).action)
    }

    @Test
    fun leavingSelectedActionInsidePopupClearsSelectionAndFallsBackToTap() {
        val hoverDelay = ManualHoverDelay()
        val state = createState(hoverDelay)
        state.onDragStart(Offset(100f, 200f))
        state.onDrag(Offset(10f, 0f))
        hoverDelay.resumeNext()
        state.updateActionPopupBounds(Rect(50f, 100f, 250f, 300f))
        state.updateActionBounds(DoubleTap, Rect(100f, 150f, 200f, 200f))

        state.onDrag(Offset(40f, -25f))
        assertEquals(DoubleTap, state.pendingAction)

        state.onDrag(Offset(75f, 75f))

        assertNull(state.pendingAction)
        assertEquals(Tap, decodeMoveScreenData(state).action)
    }

    @Test
    fun staleHoverAndCancelCannotOpenPopup() {
        val hoverDelay = ManualHoverDelay()
        val state = createState(hoverDelay)
        state.onDragStart(Offset(100f, 200f))
        state.onDrag(Offset(10f, 0f))
        state.onDrag(Offset(30f, 0f))

        assertEquals(2, hoverDelay.pendingCount)
        hoverDelay.resumeNext()

        assertEquals(MoveScreenPhase.HoverPending, state.phase)
        assertFalse(state.showMoveScreenActionPopup)

        hoverDelay.resumeNext()
        assertTrue(state.showMoveScreenActionPopup)

        state.onDragCancel()

        assertEquals(MoveScreenPhase.Following, state.phase)
        assertFalse(state.visible)
        assertFalse(state.showMoveScreenActionPopup)
        assertNull(state.pendingAction)

        state.onDragStart(Offset(300f, 400f))
        state.onDrag(Offset(5f, 0f))
        state.onDragCancel()
        hoverDelay.resumeNext()

        assertEquals(MoveScreenPhase.Following, state.phase)
        assertFalse(state.showMoveScreenActionPopup)
    }

    @Test
    fun disabledPopupAndOutOfBoundsTargetNeverArmHover() {
        val disabledDelay = ManualHoverDelay()
        val disabledState = createState(disabledDelay, popupEnabled = false)
        disabledState.onDragStart(Offset(100f, 200f))
        disabledState.onDrag(Offset(10f, 0f))

        assertEquals(MoveScreenPhase.Following, disabledState.phase)
        assertEquals(0, disabledDelay.pendingCount)

        val outOfBoundsDelay = ManualHoverDelay()
        val outOfBoundsState = createState(outOfBoundsDelay)
        outOfBoundsState.onDragStart(Offset(990f, 200f))
        outOfBoundsState.onDrag(Offset(10f, 0f))

        assertEquals(MoveScreenPhase.Following, outOfBoundsState.phase)
        assertEquals(0, outOfBoundsDelay.pendingCount)

        val boundaryDelay = ManualHoverDelay()
        val boundaryState = createState(boundaryDelay)
        boundaryState.onDragStart(Offset(970f, 200f))
        boundaryState.onDrag(Offset(10f, 0f))

        assertEquals(MoveScreenPhase.HoverPending, boundaryState.phase)
        assertEquals(1, boundaryDelay.pendingCount)
    }

    @Test
    fun fastMoveAccelerationKeepsSlowMovementPreciseAndCapsFastMovement() {
        val slowClock = ManualNanoTime()
        val slowState = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = slowClock::read
        )
        slowState.onDragStart(Offset(100f, 200f))

        slowClock.advanceMillis(100)
        slowState.onDrag(Offset(10f, 0f))

        assertEquals(Offset(130f, 200f), slowState.displayFingerOnScreen)

        val mediumClock = ManualNanoTime()
        val mediumState = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = mediumClock::read
        )
        mediumState.onDragStart(Offset(100f, 200f))

        mediumClock.advanceMillis(10)
        mediumState.onDrag(Offset(10f, 0f))

        assertTrue(mediumState.displayFingerOnScreen.x > 130f)
        assertTrue(mediumState.displayFingerOnScreen.x < 170f)

        val fastClock = ManualNanoTime()
        val fastState = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = fastClock::read
        )
        fastState.onDragStart(Offset(100f, 200f))

        fastClock.advanceMillis(1)
        fastState.onDrag(Offset(10f, 0f))

        assertEquals(Offset(170f, 200f), fastState.displayFingerOnScreen)
    }

    @Test
    fun fastMoveAccelerationRestoresBaseRateAsSoonAsMovementSlows() {
        val clock = ManualNanoTime()
        val state = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = clock::read
        )
        state.onDragStart(Offset(100f, 200f))
        clock.advanceMillis(1)
        state.onDrag(Offset(10f, 0f))

        clock.advanceMillis(100)
        state.onDrag(Offset(10f, 0f))

        assertEquals(Offset(200f, 200f), state.displayFingerOnScreen)
    }

    @Test
    fun fastMoveAccelerationRestoresBaseRateImmediatelyWhenDirectionReverses() {
        val clock = ManualNanoTime()
        val state = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = clock::read
        )
        state.onDragStart(Offset(100f, 200f))
        clock.advanceMillis(1)
        state.onDrag(Offset(10f, 0f))

        clock.advanceMillis(1)
        state.onDrag(Offset(-10f, 0f))

        assertEquals(Offset(140f, 200f), state.displayFingerOnScreen)
    }

    @Test
    fun leavingPopupRestartsFastMoveAccelerationCurve() {
        val hoverDelay = ManualHoverDelay()
        val resumedClock = ManualNanoTime()
        val resumedState = createState(
            hoverDelay = hoverDelay,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = resumedClock::read
        )
        resumedState.onDragStart(Offset(100f, 200f))
        resumedClock.advanceMillis(1)
        resumedState.onDrag(Offset(10f, 0f))
        hoverDelay.resumeNext()
        resumedState.updateActionPopupBounds(Rect(0f, 0f, 300f, 400f))
        val frozenTarget = resumedState.displayFingerOnScreen

        resumedClock.advanceMillis(1)
        resumedState.onDrag(Offset(300f, 0f))
        resumedClock.advanceMillis(10)
        resumedState.onDrag(Offset(10f, 0f))
        val resumedDelta = resumedState.displayFingerOnScreen.x - frozenTarget.x

        val freshClock = ManualNanoTime()
        val freshState = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = freshClock::read
        )
        freshState.onDragStart(Offset(500f, 600f))
        freshClock.advanceMillis(10)
        freshState.onDrag(Offset(10f, 0f))
        val freshDelta = freshState.displayFingerOnScreen.x - 500f

        assertEquals(freshDelta, resumedDelta, 0.001f)
    }

    @Test
    fun disabledAndResetFastMoveAccelerationUseBaseRate() {
        val disabledClock = ManualNanoTime()
        val disabledState = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            nanoTimeProvider = disabledClock::read
        )
        disabledState.onDragStart(Offset(100f, 200f))
        disabledClock.advanceMillis(1)
        disabledState.onDrag(Offset(10f, 0f))

        assertEquals(Offset(130f, 200f), disabledState.displayFingerOnScreen)

        val resetClock = ManualNanoTime()
        val resetState = createState(
            hoverDelay = ManualHoverDelay(),
            popupEnabled = false,
            fastMoveAccelerationEnabled = true,
            nanoTimeProvider = resetClock::read
        )
        resetState.onDragStart(Offset(100f, 200f))
        resetClock.advanceMillis(1)
        resetState.onDrag(Offset(10f, 0f))
        resetState.onDragCancel()
        resetState.onDragStart(Offset(500f, 600f))

        resetClock.advanceMillis(100)
        resetState.onDrag(Offset(10f, 0f))

        assertEquals(Offset(530f, 600f), resetState.displayFingerOnScreen)
    }

    private fun createState(
        hoverDelay: ManualHoverDelay,
        popupEnabled: Boolean = true,
        fastMoveAccelerationEnabled: Boolean = false,
        nanoTimeProvider: () -> Long = System::nanoTime,
        onActionSelected: () -> Unit = {}
    ): MoveScreenState {
        hoverDelays += hoverDelay
        return MoveScreenState(
            actionSettings = ActionSettings.MoveScreen(
                rate = 2f,
                fastMoveAccelerationEnabled = fastMoveAccelerationEnabled,
                hoverDelayMs = 600L,
                radius = 20,
                style = ActionSettings.MoveScreen.Style.Crosshair,
                popupEnabled = popupEnabled
            ),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            screenSizeProvider = { IntSize(1000, 2000) },
            hoverDelay = hoverDelay::await,
            nanoTimeProvider = nanoTimeProvider,
            onActionSelected = onActionSelected
        ).also(states::add)
    }

    private fun decodeMoveScreenData(state: MoveScreenState): MoveScreenData {
        return JsonHelper.decodeFromString(state.done().data)
    }

    private class ManualHoverDelay {

        private val continuations = ArrayDeque<Continuation<Unit>>()

        val pendingCount: Int get() = continuations.size

        suspend fun await(delayMs: Long) {
            check(delayMs >= 0L)
            suspendCoroutine { continuations.addLast(it) }
        }

        fun resumeNext() {
            continuations.removeFirst().resume(Unit)
        }

        fun resumeAll() {
            while (continuations.isNotEmpty()) {
                resumeNext()
            }
        }
    }

    private class ManualNanoTime {

        private var now = 0L

        fun read(): Long = now

        fun advanceMillis(millis: Long) {
            now += millis * 1_000_000L
        }
    }
}
