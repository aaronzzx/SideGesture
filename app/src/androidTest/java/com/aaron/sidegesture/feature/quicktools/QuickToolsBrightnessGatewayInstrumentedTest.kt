package com.aaron.sidegesture.feature.quicktools

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.ktx.canWriteSystemSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class QuickToolsBrightnessGatewayInstrumentedTest {

    @Test
    fun brightnessWriteUsesPerceptualMappingAndObservedReadback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("WRITE_SETTINGS app-op is required", context.canWriteSystemSettings())
        val resolver = context.contentResolver
        val originalRawValue = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        val originalMode = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        )
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        val gateway = QuickToolsExecutor.brightnessGateway(context)
        val changeLatch = CountDownLatch(1)
        val observation = gateway.observeChanges { changeLatch.countDown() }

        try {
            val operation = runBlocking { gateway.setRatio(0.5f) }
            assertTrue(
                operation.result == QuickToolsOperationResult.Success ||
                    operation.result == QuickToolsOperationResult.PendingSystemSync
            )
            assertTrue(changeLatch.await(2, TimeUnit.SECONDS))

            val snapshot = gateway.readSnapshot()
            val expectedRawValue = QuickToolsBrightnessMapping.ratioToRaw(
                ratio = 0.5f,
                range = snapshot.range,
                sdkInt = android.os.Build.VERSION.SDK_INT
            )
            assertTrue(abs(snapshot.rawValue - expectedRawValue) <= 1)
            assertEquals(0.5f, snapshot.ratio, 0.02f)
        } finally {
            observation.close()
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                originalRawValue
            )
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                originalMode
            )
        }
    }

    @Test
    fun observerStopsReceivingChangesAfterClose() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("WRITE_SETTINGS app-op is required", context.canWriteSystemSettings())
        val resolver = context.contentResolver
        val originalRawValue = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        val originalMode = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        )
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        val gateway = QuickToolsExecutor.brightnessGateway(context)
        val callbackCount = AtomicInteger(0)
        val observation = gateway.observeChanges { callbackCount.incrementAndGet() }
        observation.close()

        try {
            val range = gateway.readSnapshot().range
            val targetRawValue = if (originalRawValue == range.minimum) {
                range.maximum
            } else {
                range.minimum
            }
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                targetRawValue
            )
            Thread.sleep(300)

            assertEquals(0, callbackCount.get())
        } finally {
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                originalRawValue
            )
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                originalMode
            )
        }
    }

    @Test
    fun autoToggleUsesObservedSystemModeReadback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("WRITE_SETTINGS app-op is required", context.canWriteSystemSettings())
        val resolver = context.contentResolver
        val originalMode = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        )
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        val gateway = QuickToolsExecutor.brightnessGateway(context)
        val changeLatch = CountDownLatch(1)
        val observation = gateway.observeChanges { changeLatch.countDown() }

        try {
            val operation = runBlocking { gateway.toggleAuto() }
            assertTrue(
                operation.result == QuickToolsOperationResult.Success ||
                    operation.result == QuickToolsOperationResult.PendingSystemSync
            )
            assertTrue(changeLatch.await(2, TimeUnit.SECONDS))
            assertTrue(gateway.readSnapshot().autoEnabled)
        } finally {
            observation.close()
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                originalMode
            )
        }
    }
}
