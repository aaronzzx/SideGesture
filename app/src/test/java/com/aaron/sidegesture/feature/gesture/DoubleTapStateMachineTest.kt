package com.aaron.sidegesture.feature.gesture

import com.aaron.sidegesture.feature.gesture.DoubleTapStateMachine.DownResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleTapStateMachineTest {

    private val stateMachine = DoubleTapStateMachine<String>(
        timeoutMillis = 300L,
        doubleTapSlop = 20f
    )

    @Test
    fun noPendingTapDoesNotMatchSecondDown() {
        val result = stateMachine.onDown("left|1", 10f, 20f, 100L)

        assertEquals(DownResolution.NoPending, result.resolution)
        assertNull(result.expiredSingleTap)
    }

    @Test
    fun sameButtonWithinTimeoutAndSlopDispatchesOnlyDoubleTap() {
        val token = stateMachine.begin(
            buttonKey = "left|1",
            downX = 10f,
            downY = 20f,
            upTimeMillis = 100L,
            singleTap = "single",
            doubleTap = "double"
        )

        val down = stateMachine.onDown("left|1", 22f, 26f, 250L)

        assertEquals(DownResolution.Matched, down.resolution)
        assertNull(stateMachine.consumeTimeout(token))
        assertEquals("double", stateMachine.completeSecondTap(280L))
        assertNull(stateMachine.completeSecondTap(281L))
        assertFalse(stateMachine.hasPending)
    }

    @Test
    fun timeoutDispatchesSingleTapOnlyOnce() {
        val token = stateMachine.begin(
            buttonKey = "left|1",
            downX = 10f,
            downY = 20f,
            upTimeMillis = 100L,
            singleTap = "single",
            doubleTap = "double"
        )

        assertEquals("single", stateMachine.consumeTimeout(token))
        assertNull(stateMachine.consumeTimeout(token))
        assertFalse(stateMachine.hasPending)
    }

    @Test
    fun crossButtonSecondDownCancelsOldCandidate() {
        val token = stateMachine.begin(
            buttonKey = "left|1",
            downX = 10f,
            downY = 20f,
            upTimeMillis = 100L,
            singleTap = "single",
            doubleTap = "double"
        )

        val down = stateMachine.onDown("right|1", 10f, 20f, 200L)

        assertEquals(DownResolution.Rejected, down.resolution)
        assertNull(stateMachine.consumeTimeout(token))
        assertFalse(stateMachine.hasPending)
    }

    @Test
    fun secondDownOutsideDoubleTapSlopCancelsOldCandidate() {
        stateMachine.begin(
            buttonKey = "left|1",
            downX = 10f,
            downY = 20f,
            upTimeMillis = 100L,
            singleTap = "single",
            doubleTap = "double"
        )

        val down = stateMachine.onDown("left|1", 31f, 20f, 200L)

        assertEquals(DownResolution.Rejected, down.resolution)
        assertFalse(stateMachine.hasPending)
    }

    @Test
    fun lateSecondDownReturnsExpiredSingleTap() {
        stateMachine.begin(
            buttonKey = "left|1",
            downX = 10f,
            downY = 20f,
            upTimeMillis = 100L,
            singleTap = "single",
            doubleTap = "double"
        )

        val down = stateMachine.onDown("left|1", 10f, 20f, 401L)

        assertEquals(DownResolution.Expired, down.resolution)
        assertEquals("single", down.expiredSingleTap)
        assertFalse(stateMachine.hasPending)
    }

    @Test
    fun secondTapFinishingAfterDeadlineDoesNotDispatchDoubleTap() {
        stateMachine.begin(
            buttonKey = "left|1",
            downX = 10f,
            downY = 20f,
            upTimeMillis = 100L,
            singleTap = "single",
            doubleTap = "double"
        )
        assertEquals(
            DownResolution.Matched,
            stateMachine.onDown("left|1", 10f, 20f, 350L).resolution
        )

        assertNull(stateMachine.completeSecondTap(401L))
        assertFalse(stateMachine.hasPending)
    }

    @Test
    fun cancelClearsPendingTapAndInvalidatesTimeout() {
        val token = stateMachine.begin(
            buttonKey = "left|1",
            downX = 10f,
            downY = 20f,
            upTimeMillis = 100L,
            singleTap = "single",
            doubleTap = "double"
        )
        assertTrue(stateMachine.hasPending)

        stateMachine.cancel()

        assertNull(stateMachine.consumeTimeout(token))
        assertFalse(stateMachine.hasPending)
    }
}
