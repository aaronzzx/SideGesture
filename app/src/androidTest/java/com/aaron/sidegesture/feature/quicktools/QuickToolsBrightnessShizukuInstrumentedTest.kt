package com.aaron.sidegesture.feature.quicktools

import android.os.Build
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.ktx.canWriteSystemSettings
import com.aaron.sidegesture.platform.shizuku.ShizukuShellManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class QuickToolsBrightnessShizukuInstrumentedTest {

    @Test
    fun externalChangesRefreshOnlyWhileObservedAndRestartReadsLatestValue() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("WRITE_SETTINGS must stay denied for this test", context.canWriteSystemSettings())
        val gateway = QuickToolsExecutor.brightnessGateway(context)
        val originalSnapshot = gateway.readSnapshot()
        assertEquals(
            QuickToolsBrightnessWriteCapability.Shizuku,
            originalSnapshot.writeCapability
        )
        val controller = QuickToolsBrightnessController(gateway)

        try {
            if (gateway.readSnapshot().autoEnabled) {
                runBlocking { gateway.toggleAuto() }
            }
            controller.start()
            val firstRatio = 0.25f
            val firstRawValue = QuickToolsBrightnessMapping.ratioToRaw(
                ratio = firstRatio,
                range = originalSnapshot.range,
                sdkInt = Build.VERSION.SDK_INT
            )
            val firstWrite = runBlocking {
                ShizukuShellManager.execute(
                    "settings put system ${Settings.System.SCREEN_BRIGHTNESS} $firstRawValue"
                )
            }
            assertTrue(firstWrite.isSuccess)
            val firstDeadline = System.currentTimeMillis() + 3_000L
            while (
                abs(controller.displayedRatio - firstRatio) > 0.02f &&
                System.currentTimeMillis() < firstDeadline
            ) {
                Thread.sleep(20)
            }
            assertEquals(firstRatio, controller.displayedRatio, 0.02f)

            controller.stop()
            val stoppedRatio = controller.displayedRatio
            val secondRatio = 0.75f
            val secondRawValue = QuickToolsBrightnessMapping.ratioToRaw(
                ratio = secondRatio,
                range = originalSnapshot.range,
                sdkInt = Build.VERSION.SDK_INT
            )
            val secondWrite = runBlocking {
                ShizukuShellManager.execute(
                    "settings put system ${Settings.System.SCREEN_BRIGHTNESS} $secondRawValue"
                )
            }
            assertTrue(secondWrite.isSuccess)
            Thread.sleep(300)
            assertEquals(stoppedRatio, controller.displayedRatio, 0.001f)

            controller.start()
            assertEquals(secondRatio, controller.displayedRatio, 0.02f)
        } finally {
            controller.stop()
            if (gateway.readSnapshot().autoEnabled) {
                runBlocking { gateway.toggleAuto() }
            }
            val originalRatio = QuickToolsBrightnessMapping.rawToRatio(
                rawValue = originalSnapshot.rawValue,
                range = originalSnapshot.range,
                sdkInt = Build.VERSION.SDK_INT
            )
            runBlocking { gateway.setRatio(originalRatio) }
            if (originalSnapshot.autoEnabled && !gateway.readSnapshot().autoEnabled) {
                runBlocking { gateway.toggleAuto() }
            }
        }
    }

    @Test
    fun shizukuWritesBrightnessAndAutoModeWithoutWriteSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("WRITE_SETTINGS must stay denied for this test", context.canWriteSystemSettings())
        val gateway = QuickToolsExecutor.brightnessGateway(context)
        val originalSnapshot = gateway.readSnapshot()
        assertEquals(
            QuickToolsBrightnessWriteCapability.Shizuku,
            originalSnapshot.writeCapability
        )

        try {
            if (gateway.readSnapshot().autoEnabled) {
                val manualOperation = runBlocking { gateway.toggleAuto() }
                assertTrue(
                    manualOperation.result == QuickToolsOperationResult.Success ||
                        manualOperation.result == QuickToolsOperationResult.PendingSystemSync
                )
                assertFalse(gateway.readSnapshot().autoEnabled)
            }

            val currentRatio = gateway.readSnapshot().ratio
            val targetRatio = if (currentRatio < 0.5f) 0.75f else 0.25f
            val changeLatch = CountDownLatch(1)
            val observation = gateway.observeChanges { changeLatch.countDown() }
            try {
                val brightnessOperation = runBlocking { gateway.setRatio(targetRatio) }
                assertTrue(
                    brightnessOperation.result == QuickToolsOperationResult.Success ||
                        brightnessOperation.result == QuickToolsOperationResult.PendingSystemSync
                )
                assertTrue(changeLatch.await(3, TimeUnit.SECONDS))

                val changedSnapshot = gateway.readSnapshot()
                val expectedRawValue = QuickToolsBrightnessMapping.ratioToRaw(
                    ratio = targetRatio,
                    range = changedSnapshot.range,
                    sdkInt = Build.VERSION.SDK_INT
                )
                assertTrue(abs(changedSnapshot.rawValue - expectedRawValue) <= 1)
                assertEquals(targetRatio, changedSnapshot.ratio, 0.02f)
            } finally {
                observation.close()
            }

            val autoOperation = runBlocking { gateway.toggleAuto() }
            assertTrue(
                autoOperation.result == QuickToolsOperationResult.Success ||
                    autoOperation.result == QuickToolsOperationResult.PendingSystemSync
            )
            assertTrue(gateway.readSnapshot().autoEnabled)

            val manualOperation = runBlocking { gateway.toggleAuto() }
            assertTrue(
                manualOperation.result == QuickToolsOperationResult.Success ||
                    manualOperation.result == QuickToolsOperationResult.PendingSystemSync
            )
            assertFalse(gateway.readSnapshot().autoEnabled)
        } finally {
            if (gateway.readSnapshot().autoEnabled) {
                runBlocking { gateway.toggleAuto() }
            }
            val originalRatio = QuickToolsBrightnessMapping.rawToRatio(
                rawValue = originalSnapshot.rawValue,
                range = originalSnapshot.range,
                sdkInt = Build.VERSION.SDK_INT
            )
            runBlocking { gateway.setRatio(originalRatio) }
            if (originalSnapshot.autoEnabled && !gateway.readSnapshot().autoEnabled) {
                runBlocking { gateway.toggleAuto() }
            }
        }
    }
}
