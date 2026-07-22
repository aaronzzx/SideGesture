package com.aaron.sidegesture.ui.screen.quicklauncher

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.entity.global.QuickLauncherSettings
import com.aaron.sidegesture.ktx.LocalNavController
import com.aaron.sidegesture.utils.DataStoreHolder
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickLauncherSettingsScreenTest {

    private companion object {
        const val UI_TIMEOUT_MILLIS = 5_000L
        const val UI_POLL_INTERVAL_MILLIS = 50L
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun pageShowsAllControlsAndPersistsSettings() {
        val originalActionSettings = runBlocking {
            DataStoreHolder.actionSettings.data.first()
        }
        val expected = QuickLauncherSettings(
            rows = 3,
            columns = 5,
            iconSizeDp = 50,
            textSizeSp = 14
        )
        val vm = QuickLauncherSettingsVM()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        try {
            scenario.onActivity { activity ->
                activity.setContent {
                    MaterialTheme {
                        val navController = rememberNavController()
                        CompositionLocalProvider(LocalNavController provides navController) {
                            QuickLauncherSettingsScreen(onBack = {}, vm = vm)
                        }
                    }
                }
            }
            waitForText("快速启动器设置")
            waitForText("行数")
            waitForText("列数")
            waitForText("图标大小")
            waitForText("文字大小")
            waitUntil("settings load") {
                enabledSliderCount() == 4
            }

            scenario.onActivity {
                vm.onRowsChange(expected.rows.toFloat())
                vm.onColumnsChange(expected.columns.toFloat())
                vm.onIconSizeChange(expected.iconSizeDp.toFloat())
                vm.onTextSizeChange(expected.textSizeSp.toFloat())
                vm.saveSettings()
            }

            val saved = runBlocking {
                withTimeout(UI_TIMEOUT_MILLIS) {
                    DataStoreHolder.actionSettings.data.first {
                        it.quickLauncher == expected
                    }
                }
            }
            assertEquals(expected, saved.quickLauncher)
            captureScreenshot("quick-launcher-settings.png")
        } finally {
            runBlocking {
                DataStoreHolder.actionSettings.updateData { originalActionSettings }
            }
            scenario.close()
        }
    }

    private fun waitForText(value: String) {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            val roots = accessibilityRoots()
            if (roots.any { findText(it, value) }) return
            SystemClock.sleep(UI_POLL_INTERVAL_MILLIS)
        }
        error(
            "Timed out waiting for text '$value'\n" +
                accessibilityRoots().joinToString(separator = "\n--- window ---\n", transform = ::dumpTree)
        )
    }

    private fun waitUntil(description: String, predicate: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            if (predicate()) return
            SystemClock.sleep(UI_POLL_INTERVAL_MILLIS)
        }
        error("Timed out waiting for $description")
    }

    private fun findText(node: AccessibilityNodeInfo, value: String): Boolean {
        if (node.text?.toString() == value) return true
        repeat(node.childCount) { index ->
            val child = node.getChild(index) ?: return@repeat
            if (findText(child, value)) return true
        }
        return false
    }

    private fun enabledSliderCount(): Int {
        instrumentation.waitForIdleSync()
        return accessibilityRoots().sumOf(::countEnabledSliders)
    }

    private fun countEnabledSliders(node: AccessibilityNodeInfo): Int {
        var count = if (node.className == "android.widget.SeekBar" && node.isEnabled) 1 else 0
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { count += countEnabledSliders(it) }
        }
        return count
    }

    private fun accessibilityRoots(): List<AccessibilityNodeInfo> {
        val windowRoots = instrumentation.uiAutomation.windows.mapNotNull { it.root }
        return windowRoots.ifEmpty {
            listOfNotNull(instrumentation.uiAutomation.rootInActiveWindow)
        }
    }

    private fun dumpTree(root: AccessibilityNodeInfo): String {
        val lines = mutableListOf<String>()

        fun appendNode(node: AccessibilityNodeInfo, depth: Int) {
            lines += buildString {
                repeat(depth) { append("  ") }
                append("class=").append(node.className)
                append(" text=").append(node.text)
                append(" description=").append(node.contentDescription)
                append(" enabled=").append(node.isEnabled)
            }
            repeat(node.childCount) { index ->
                node.getChild(index)?.let { appendNode(it, depth + 1) }
            }
        }

        appendNode(root, 0)
        return lines.joinToString(separator = "\n")
    }

    private fun captureScreenshot(fileName: String) {
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = File(instrumentation.targetContext.cacheDir, fileName)
        try {
            FileOutputStream(file).use { output ->
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            screenshot.recycle()
        }
    }
}
