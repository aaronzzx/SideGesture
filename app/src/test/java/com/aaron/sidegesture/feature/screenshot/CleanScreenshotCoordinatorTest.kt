package com.aaron.sidegesture.feature.screenshot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class CleanScreenshotCoordinatorTest {

    @Test
    fun unsupportedApiDoesNotTouchWindowsOrCapture() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = createCoordinator(
            events = events,
            isScreenshotSupported = { false }
        )

        val result = coordinator.capture()

        assertNull(result)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun successfulCaptureRestoresWindowsInOrder() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = createCoordinator(events)

        val result = coordinator.capture()

        assertNull(result)
        assertEquals(listOf("hide", "frame wait", "capture", "restore"), events)
    }

    @Test
    fun captureExceptionRestoresWindowsOnceAndPropagates() = runBlocking {
        val events = mutableListOf<String>()
        val expected = IllegalStateException("capture failed")
        val coordinator = createCoordinator(
            events = events,
            captureScreenshot = {
                events += "capture"
                throw expected
            }
        )

        try {
            coordinator.capture()
            fail("Expected capture exception")
        } catch (actual: IllegalStateException) {
            assertSame(expected, actual)
        }

        assertEquals(listOf("hide", "frame wait", "capture", "restore"), events)
    }

    @Test
    fun cancellationAfterHideRestoresWindowsOnceAndPropagates() = runBlocking {
        val events = mutableListOf<String>()
        val expected = CancellationException("capture cancelled")
        var propagated: CancellationException? = null
        val coordinator = createCoordinator(
            events = events,
            hiddenFrameWaiter = {
                events += "frame wait"
                awaitCancellation()
            }
        )
        val capture = async(start = CoroutineStart.UNDISPATCHED) {
            try {
                coordinator.capture()
            } catch (actual: CancellationException) {
                propagated = actual
                throw actual
            }
        }

        capture.cancel(expected)

        try {
            capture.await()
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertEquals(expected.message, actual.message)
        }

        assertEquals(expected.message, propagated?.message)
        assertEquals(listOf("hide", "frame wait", "restore"), events)
    }

    private fun createCoordinator(
        events: MutableList<String>,
        captureScreenshot: suspend () -> Nothing? = {
            events += "capture"
            null
        },
        hiddenFrameWaiter: suspend () -> Unit = {
            events += "frame wait"
        },
        isScreenshotSupported: () -> Boolean = { true }
    ) = CleanScreenshotCoordinator(
        windowVisibilityController = RecordingWindowVisibilityController(events),
        captureScreenshot = captureScreenshot,
        hiddenFrameWaiter = hiddenFrameWaiter,
        isScreenshotSupported = isScreenshotSupported,
        mainDispatcher = Dispatchers.Unconfined
    )

    private class RecordingWindowVisibilityController(
        private val events: MutableList<String>
    ) : WindowVisibilityController {

        override fun hideWindowsForScreenshot() {
            events += "hide"
        }

        override fun restoreWindowsAfterScreenshot() {
            events += "restore"
        }
    }
}
