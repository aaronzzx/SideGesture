package com.aaron.sidegesture.feature.quicklauncher

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.utils.JsonHelper
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class QuickLauncherPanelTest {

    private companion object {
        const val UI_TIMEOUT_MILLIS = 5_000L
        const val UI_POLL_INTERVAL_MILLIS = 50L
        const val TAP_HOLD_MILLIS = 60L
        const val SWIPE_STEPS = 16
        const val SWIPE_STEP_DURATION_MILLIS = 16L
        const val PAGER_SETTLE_MILLIS = 500L
        const val EMPTY_AREA_X_OFFSET = 180
        const val EMPTY_AREA_Y_OFFSET = 60
        const val BACKGROUND_EDGE_OFFSET = 12
        const val PANEL_WIDTH_DP = 260f
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun horizontalSwipeChangesPageWithoutLaunchingAndNextShowResetsPage() {
        val items = (1..17).map(::createAppAction)
        val launches = CopyOnWriteArrayList<Action>()
        val state = QuickLauncherPanelState().apply {
            show(items, Offset(120f, 320f), Position.Left)
        }
        val scenario = launchPanel(state) { action, _ -> launches += action }
        try {
            waitForPageDescription("第 1 页，共 2 页")
            val firstItemBounds = waitForTextBounds("应用 1")
            val fourthItemBounds = waitForTextBounds("应用 4")
            val swipeStart = Point(fourthItemBounds.centerX(), fourthItemBounds.centerY())
            val swipeEnd = Point(firstItemBounds.centerX(), firstItemBounds.centerY())

            injectSwipe(
                start = swipeStart,
                end = swipeEnd,
                onHalfway = { captureScreenshot("quick-launcher-page-transition.png") }
            )
            waitForPageDescription("第 2 页，共 2 页")

            waitForTextBounds("应用 17")
            captureScreenshot("quick-launcher-page-2.png")
            assertTrue(launches.isEmpty())
            scenario.onActivity { assertTrue(state.visible) }

            injectSwipe(start = swipeStart, end = swipeEnd)
            SystemClock.sleep(PAGER_SETTLE_MILLIS)
            assertTrue(hasContentDescription("第 2 页，共 2 页"))

            injectSwipe(start = swipeEnd, end = swipeStart)
            waitForPageDescription("第 1 页，共 2 页")

            injectSwipe(start = swipeStart, end = swipeEnd)
            waitForPageDescription("第 2 页，共 2 页")
            assertTrue(launches.isEmpty())

            scenario.onActivity {
                state.hide()
                state.show(items, Offset(120f, 320f), Position.Left)
            }
            waitForPageDescription("第 1 页，共 2 页")

            waitForTextBounds("应用 1")
            assertTrue(launches.isEmpty())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun pagerViewportReachesPanelEdgesWhileItemsKeepContentPadding() {
        val state = QuickLauncherPanelState().apply {
            show((1..17).map(::createAppAction), Offset(120f, 320f), Position.Left)
        }
        val scenario = launchPanel(state) { _, _ -> }
        try {
            val pagerBounds = waitForNodeBounds("horizontal pager") { node ->
                node.isScrollable
            }
            val indicatorBounds = waitForPageDescription("第 1 页，共 2 页")
            val firstItemBounds = waitForTextBounds("应用 1")
            val expectedPanelWidth = (
                PANEL_WIDTH_DP * instrumentation.targetContext.resources.displayMetrics.density
            ).roundToInt()

            assertTrue(pagerBounds.width() in expectedPanelWidth - 1..expectedPanelWidth + 1)
            assertEquals(indicatorBounds.left, pagerBounds.left)
            assertEquals(indicatorBounds.right, pagerBounds.right)
            assertTrue(firstItemBounds.left > pagerBounds.left)
            assertTrue(firstItemBounds.right < pagerBounds.right)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun itemTapAndLongPressKeepExistingMiniWindowRule() {
        val action = createAppAction(index = 1, miniWindow = false)
        val launches = CopyOnWriteArrayList<Pair<Action, Boolean>>()
        val state = QuickLauncherPanelState().apply {
            show(listOf(action), Offset(120f, 320f), Position.Left)
        }
        val scenario = launchPanel(state) { launchedAction, miniWindow ->
            launches += launchedAction to miniWindow
        }
        try {
            injectTap(waitForTextBounds("应用 1"))
            waitUntil("item tap launch") { launches.size == 1 }
            assertEquals(listOf(action to false), launches)
            scenario.onActivity {
                assertFalse(state.visible)
                state.show(listOf(action), Offset(120f, 320f), Position.Left)
            }

            injectTap(
                bounds = waitForTextBounds("应用 1"),
                holdMillis = ViewConfiguration.getLongPressTimeout().toLong() + 150L
            )
            waitUntil("item long press launch") { launches.size == 2 }
            assertEquals(listOf(action to false, action to true), launches)
            scenario.onActivity { assertFalse(state.visible) }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun singlePageHidesIndicatorAndOnlyBackgroundDismisses() {
        val state = QuickLauncherPanelState().apply {
            show(listOf(createAppAction(1)), Offset(120f, 320f), Position.Left)
        }
        val scenario = launchPanel(state) { _, _ -> }
        try {
            val itemBounds = waitForTextBounds("应用 1")
            assertFalse(hasPageDescription())

            injectTap(
                Point(
                    itemBounds.centerX() + EMPTY_AREA_X_OFFSET,
                    itemBounds.centerY() + EMPTY_AREA_Y_OFFSET
                )
            )
            scenario.onActivity { assertTrue(state.visible) }
            captureScreenshot("quick-launcher-single-page.png")

            var backgroundPoint = Point()
            scenario.onActivity { activity ->
                backgroundPoint = Point(
                    activity.window.decorView.width - BACKGROUND_EDGE_OFFSET,
                    itemBounds.centerY()
                )
            }
            injectTap(backgroundPoint)
            scenario.onActivity { assertFalse(state.visible) }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun panelKeepsLeftRightAndBottomEdgeAnchors() {
        val action = createAppAction(1)
        val state = QuickLauncherPanelState().apply {
            show(listOf(action), Offset(120f, 800f), Position.Left)
        }
        val scenario = launchPanel(state) { _, _ -> }
        try {
            var screenWidth = 0
            scenario.onActivity { activity ->
                screenWidth = activity.window.decorView.width
            }
            val leftBounds = waitForTextBounds("应用 1") { bounds ->
                bounds.centerX() < screenWidth / 2
            }

            scenario.onActivity {
                state.show(listOf(action), Offset(120f, 800f), Position.Right)
            }
            val rightBounds = waitForTextBounds("应用 1") { bounds ->
                bounds.centerX() > leftBounds.centerX()
            }

            scenario.onActivity {
                state.show(listOf(action), Offset(120f, 800f), Position.Bottom)
            }
            val bottomBounds = waitForTextBounds("应用 1") { bounds ->
                bounds.centerX() < screenWidth / 2 && bounds.centerY() > leftBounds.centerY()
            }

            assertTrue(rightBounds.centerX() > leftBounds.centerX())
            assertTrue(bottomBounds.centerY() > leftBounds.centerY())
            captureScreenshot("quick-launcher-bottom-edge.png")
        } finally {
            scenario.close()
        }
    }

    private fun launchPanel(
        state: QuickLauncherPanelState,
        onLaunch: (Action, Boolean) -> Unit
    ): ActivityScenario<ComponentActivity> {
        instrumentation.uiAutomation.executeShellCommand("cmd statusbar collapse").close()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                MaterialTheme {
                    QuickLauncherPanel(
                        state = state,
                        onLaunch = onLaunch,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        instrumentation.waitForIdleSync()
        return scenario
    }

    private fun waitForPageDescription(description: String): Rect {
        return waitForNodeBounds("page description '$description'") { node ->
            node.contentDescription?.toString() == description
        }
    }

    private fun waitForTextBounds(
        text: String,
        boundsPredicate: (Rect) -> Boolean = { true }
    ): Rect {
        return waitForNodeBounds("text '$text'") { node ->
            if (node.text?.toString() != text) return@waitForNodeBounds false
            val bounds = Rect().also(node::getBoundsInScreen)
            boundsPredicate(bounds)
        }
    }

    private fun waitForNodeBounds(
        description: String,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Rect {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            val node = accessibilityRoots().firstNotNullOfOrNull { root ->
                findNode(root, predicate)
            }
            if (node != null) {
                return Rect().also(node::getBoundsInScreen)
            }
            SystemClock.sleep(UI_POLL_INTERVAL_MILLIS)
        }
        val roots = accessibilityRoots()
        val tree = if (roots.isEmpty()) {
            "<empty accessibility tree>"
        } else {
            roots.joinToString(separator = "\n--- window ---\n", transform = ::dumpAccessibilityTree)
        }
        error("Timed out waiting for $description\n$tree")
    }

    private fun accessibilityRoots(): List<AccessibilityNodeInfo> {
        val windowRoots = instrumentation.uiAutomation.windows.mapNotNull { it.root }
        return windowRoots.ifEmpty {
            listOfNotNull(instrumentation.uiAutomation.rootInActiveWindow)
        }
    }

    private fun findNode(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        repeat(node.childCount) { index ->
            val child = node.getChild(index) ?: return@repeat
            findNode(child, predicate)?.let { return it }
        }
        return null
    }

    private fun dumpAccessibilityTree(root: AccessibilityNodeInfo): String {
        val lines = mutableListOf<String>()

        fun appendNode(node: AccessibilityNodeInfo, depth: Int) {
            val bounds = Rect().also(node::getBoundsInScreen)
            lines += buildString {
                repeat(depth) { append("  ") }
                append("class=").append(node.className)
                append(" text=").append(node.text)
                append(" description=").append(node.contentDescription)
                append(" bounds=").append(bounds)
            }
            repeat(node.childCount) { index ->
                node.getChild(index)?.let { appendNode(it, depth + 1) }
            }
        }

        appendNode(root, 0)
        return lines.joinToString(separator = "\n")
    }

    private fun hasPageDescription(): Boolean {
        instrumentation.waitForIdleSync()
        return accessibilityRoots().any { root ->
            findNode(root) { node ->
                val description = node.contentDescription?.toString().orEmpty()
                description.startsWith("第 ") && description.contains("页，共")
            } != null
        }
    }

    private fun hasContentDescription(value: String): Boolean {
        instrumentation.waitForIdleSync()
        return accessibilityRoots().any { root ->
            findNode(root) { node ->
                node.contentDescription?.toString() == value
            } != null
        }
    }

    private fun injectTap(bounds: Rect, holdMillis: Long = TAP_HOLD_MILLIS) {
        injectTap(Point(bounds.centerX(), bounds.centerY()), holdMillis)
    }

    private fun injectTap(point: Point, holdMillis: Long = TAP_HOLD_MILLIS) {
        val downTime = SystemClock.uptimeMillis()
        injectMotionEvent(MotionEvent.ACTION_DOWN, downTime, point)
        SystemClock.sleep(holdMillis)
        injectMotionEvent(MotionEvent.ACTION_UP, downTime, point)
        instrumentation.waitForIdleSync()
    }

    private fun injectSwipe(
        start: Point,
        end: Point,
        onHalfway: (() -> Unit)? = null
    ) {
        val downTime = SystemClock.uptimeMillis()
        injectMotionEvent(MotionEvent.ACTION_DOWN, downTime, start)
        repeat(SWIPE_STEPS) { index ->
            SystemClock.sleep(SWIPE_STEP_DURATION_MILLIS)
            val progress = (index + 1f) / SWIPE_STEPS
            injectMotionEvent(
                action = MotionEvent.ACTION_MOVE,
                downTime = downTime,
                point = Point(
                    (start.x + (end.x - start.x) * progress).toInt(),
                    (start.y + (end.y - start.y) * progress).toInt()
                )
            )
            if (index + 1 == SWIPE_STEPS / 2) {
                onHalfway?.invoke()
            }
        }
        injectMotionEvent(MotionEvent.ACTION_UP, downTime, end)
        instrumentation.waitForIdleSync()
    }

    private fun injectMotionEvent(action: Int, downTime: Long, point: Point) {
        val event = MotionEvent.obtain(
            downTime,
            SystemClock.uptimeMillis(),
            action,
            point.x.toFloat(),
            point.y.toFloat(),
            0
        ).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        try {
            assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
        } finally {
            event.recycle()
        }
    }

    private fun waitUntil(description: String, predicate: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            if (predicate()) return
            SystemClock.sleep(UI_POLL_INTERVAL_MILLIS)
        }
        error("Timed out waiting for $description")
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

    private fun createAppAction(index: Int, miniWindow: Boolean = false): Action {
        return Action(
            value = GlobalActions.EXTRA_LAUNCH_APP,
            data = JsonHelper.encodeToString(
                AppInfo(
                    packageName = instrumentation.targetContext.packageName,
                    className = "com.aaron.sidegesture.MainActivity",
                    label = "应用 $index",
                    miniWindow = miniWindow
                )
            )
        )
    }
}
