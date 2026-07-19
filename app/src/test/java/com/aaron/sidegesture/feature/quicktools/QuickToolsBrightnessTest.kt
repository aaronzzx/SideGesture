package com.aaron.sidegesture.feature.quicktools

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickToolsBrightnessTest {

    @Test
    fun android15SystemUiPositionsMapToObservedRawValues() {
        val range = QuickToolsBrightnessRange(minimum = 1, maximum = 255)

        assertEquals(1, QuickToolsBrightnessMapping.ratioToRaw(0f, range, sdkInt = 35))
        assertEquals(6, QuickToolsBrightnessMapping.ratioToRaw(0.25f, range, sdkInt = 35))
        assertEquals(22, QuickToolsBrightnessMapping.ratioToRaw(0.5f, range, sdkInt = 35))
        assertEquals(68, QuickToolsBrightnessMapping.ratioToRaw(0.75f, range, sdkInt = 35))
        assertEquals(255, QuickToolsBrightnessMapping.ratioToRaw(1f, range, sdkInt = 35))
    }

    @Test
    fun rawValuesRoundTripThroughPerceptualMapping() {
        val range = QuickToolsBrightnessRange(minimum = 1, maximum = 255)

        listOf(1, 6, 22, 68, 128, 255).forEach { rawValue ->
            val ratio = QuickToolsBrightnessMapping.rawToRatio(rawValue, range, sdkInt = 35)
            val restoredRawValue = QuickToolsBrightnessMapping.ratioToRaw(
                ratio,
                range,
                sdkInt = 35
            )

            assertTrue(kotlin.math.abs(restoredRawValue - rawValue) <= 1)
        }
    }

    @Test
    fun preAndroid9SystemUiUsesLinearMapping() {
        val range = QuickToolsBrightnessRange(minimum = 10, maximum = 255)

        assertEquals(133, QuickToolsBrightnessMapping.ratioToRaw(0.5f, range, sdkInt = 27))
        assertEquals(
            0.5f,
            QuickToolsBrightnessMapping.rawToRatio(133, range, sdkInt = 27),
            0.005f
        )
    }

    @Test
    fun modernAospRangeUsesBrightnessSynchronizerBounds() {
        assertEquals(
            QuickToolsBrightnessRange(minimum = 1, maximum = 255),
            QuickToolsBrightnessMapping.resolveRange(
                sdkInt = 35,
                configuredMinimum = 10,
                configuredMaximum = 255
            )
        )
    }

    @Test
    fun legacyAndExtendedRangesKeepConfiguredBounds() {
        assertEquals(
            QuickToolsBrightnessRange(minimum = 10, maximum = 255),
            QuickToolsBrightnessMapping.resolveRange(
                sdkInt = 28,
                configuredMinimum = 10,
                configuredMaximum = 255
            )
        )
        assertEquals(
            QuickToolsBrightnessRange(minimum = 2, maximum = 2047),
            QuickToolsBrightnessMapping.resolveRange(
                sdkInt = 35,
                configuredMinimum = 2,
                configuredMaximum = 2047
            )
        )
    }

    @Test
    fun observationOnlyRunsWhileControllerIsStarted() {
        val gateway = FakeBrightnessGateway()
        val controller = QuickToolsBrightnessController(gateway)

        controller.start()
        controller.start()
        assertEquals(1, gateway.observationStarts)
        assertTrue(gateway.hasObserver)

        gateway.updateRatioExternally(0.75f)
        assertEquals(0.75f, controller.displayedRatio, 0.001f)

        controller.stop()
        assertEquals(1, gateway.observationStops)
        assertFalse(gateway.hasObserver)

        gateway.updateRatioExternally(0.25f)
        assertEquals(0.75f, controller.displayedRatio, 0.001f)

        controller.start()
        assertEquals(2, gateway.observationStarts)
        assertEquals(0.25f, controller.displayedRatio, 0.001f)
    }

    @Test
    fun missingPermissionDoesNotOptimisticallyUpdateBrightness() = runBlocking {
        val gateway = FakeBrightnessGateway(
            writeCapability = QuickToolsBrightnessWriteCapability.None
        )
        val controller = QuickToolsBrightnessController(gateway)
        controller.start()

        val ratioResult = controller.setRatio(0.9f)
        val autoResult = controller.toggleAuto()

        assertSame(QuickToolsOperationResult.NeedsWriteSettingsOrShizuku, ratioResult)
        assertSame(QuickToolsOperationResult.NeedsWriteSettingsOrShizuku, autoResult)
        assertEquals(0, gateway.requestedRatios.size)
        assertEquals(0, gateway.toggleRequests)
        assertEquals(0.5f, controller.displayedRatio, 0.001f)
        assertFalse(controller.snapshot.autoEnabled)
        assertNull(controller.pendingRatio)
    }

    @Test
    fun autoToggleWaitsForObservedSystemReadback() = runBlocking {
        val gateway = FakeBrightnessGateway(autoToggleCompletesImmediately = false)
        val controller = QuickToolsBrightnessController(gateway)
        controller.start()

        val result = controller.toggleAuto()

        assertSame(QuickToolsOperationResult.PendingSystemSync, result)
        assertFalse(controller.snapshot.autoEnabled)

        gateway.updateAutoExternally(true)
        assertTrue(controller.snapshot.autoEnabled)
    }

    @Test
    fun rapidWritesSkipQueuedStaleValuesAndKeepLatestPendingRatio() = runBlocking {
        val firstWriteGate = CompletableDeferred<Unit>()
        val gateway = FakeBrightnessGateway(firstWriteGate = firstWriteGate)
        val controller = QuickToolsBrightnessController(gateway)
        controller.start()

        val first = async(start = CoroutineStart.UNDISPATCHED) {
            controller.setRatio(0.25f)
        }
        val stale = async(start = CoroutineStart.UNDISPATCHED) {
            controller.setRatio(0.5f)
        }
        val latest = async(start = CoroutineStart.UNDISPATCHED) {
            controller.setRatio(0.75f)
        }

        assertEquals(0.75f, controller.displayedRatio, 0.001f)
        firstWriteGate.complete(Unit)

        assertSame(QuickToolsOperationResult.Success, first.await())
        assertSame(QuickToolsOperationResult.Superseded, stale.await())
        assertSame(QuickToolsOperationResult.Success, latest.await())
        assertEquals(listOf(0.25f, 0.75f), gateway.requestedRatios)
        assertEquals(0.75f, controller.displayedRatio, 0.001f)
        assertNull(controller.pendingRatio)
    }

    private class FakeBrightnessGateway(
        writeCapability: QuickToolsBrightnessWriteCapability =
            QuickToolsBrightnessWriteCapability.WriteSettings,
        private val firstWriteGate: CompletableDeferred<Unit>? = null,
        private val autoToggleCompletesImmediately: Boolean = true
    ) : QuickToolsBrightnessGateway {

        private val range = QuickToolsBrightnessRange(minimum = 1, maximum = 255)
        private var observer: (() -> Unit)? = null
        private var currentSnapshot = snapshot(
            ratio = 0.5f,
            writeCapability = writeCapability
        )

        val requestedRatios = mutableListOf<Float>()
        var toggleRequests = 0
            private set
        var observationStarts = 0
            private set
        var observationStops = 0
            private set
        val hasObserver: Boolean
            get() = observer != null

        override fun readSnapshot(): QuickToolsBrightnessSnapshot = currentSnapshot

        override fun observeChanges(onChanged: () -> Unit): AutoCloseable {
            observationStarts++
            observer = onChanged
            return AutoCloseable {
                observationStops++
                observer = null
            }
        }

        override suspend fun setRatio(ratio: Float): QuickToolsBrightnessOperation {
            requestedRatios += ratio
            if (requestedRatios.size == 1) {
                firstWriteGate?.await()
            }
            currentSnapshot = snapshot(
                ratio = ratio,
                writeCapability = currentSnapshot.writeCapability
            )
            return QuickToolsBrightnessOperation(
                result = QuickToolsOperationResult.Success,
                snapshot = currentSnapshot
            )
        }

        override suspend fun toggleAuto(): QuickToolsBrightnessOperation {
            toggleRequests++
            if (autoToggleCompletesImmediately) {
                currentSnapshot = currentSnapshot.copy(
                    autoEnabled = !currentSnapshot.autoEnabled
                )
            }
            return QuickToolsBrightnessOperation(
                result = if (autoToggleCompletesImmediately) {
                    QuickToolsOperationResult.Success
                } else {
                    QuickToolsOperationResult.PendingSystemSync
                },
                snapshot = currentSnapshot
            )
        }

        fun updateRatioExternally(ratio: Float) {
            currentSnapshot = snapshot(
                ratio = ratio,
                writeCapability = currentSnapshot.writeCapability
            )
            observer?.invoke()
        }

        fun updateAutoExternally(enabled: Boolean) {
            currentSnapshot = currentSnapshot.copy(autoEnabled = enabled)
            observer?.invoke()
        }

        private fun snapshot(
            ratio: Float,
            writeCapability: QuickToolsBrightnessWriteCapability
        ): QuickToolsBrightnessSnapshot {
            return QuickToolsBrightnessSnapshot(
                rawValue = QuickToolsBrightnessMapping.ratioToRaw(
                    ratio,
                    range,
                    sdkInt = 35
                ),
                ratio = ratio,
                autoEnabled = false,
                range = range,
                writeCapability = writeCapability
            )
        }
    }
}
