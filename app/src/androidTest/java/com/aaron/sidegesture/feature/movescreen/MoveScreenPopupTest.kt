package com.aaron.sidegesture.feature.movescreen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.MoveScreenData
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.DoubleTap
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.Tap
import com.aaron.sidegesture.ui.theme.generator.AppTheme
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoveScreenPopupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun crosshairUsesRootBoundsAndSupportsRehover() {
        verifyPopupFlow()
    }

    private fun verifyPopupFlow() {
        val hoverGate = Channel<Unit>(Channel.UNLIMITED)
        val screenSize = IntSize(1080, 1920)
        lateinit var state: MoveScreenState
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            state = remember {
                MoveScreenState(
                    actionSettings = ActionSettings.MoveScreen(
                        rate = 2f,
                        hoverDelayMs = 600L,
                        radius = 20,
                        style = ActionSettings.MoveScreen.Style.Crosshair,
                        popupEnabled = true
                    ),
                    coroutineScope = scope,
                    screenSizeProvider = { screenSize },
                    hoverDelay = { hoverGate.receive() }
                ).also {
                    it.onDragStart(Offset(540f, 900f))
                }
            }
            AppTheme(darkTheme = false, dynamicColor = false) {
                CrosshairScreen(state, Modifier.fillMaxSize())
            }
        }
        composeTestRule.runOnIdle {
            state.onDrag(Offset(10f, 0f))
            hoverGate.trySend(Unit).getOrThrow()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            state.showMoveScreenActionPopup
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tapNode = composeTestRule.onNodeWithText(context.getString(R.string.tap))
        val doubleTapNode = composeTestRule.onNodeWithText(context.getString(R.string.double_tap))
        val longPressNode = composeTestRule.onNodeWithText(context.getString(R.string.long_press))
        tapNode.assertIsDisplayed()
        doubleTapNode.assertIsDisplayed()
        longPressNode.assertIsDisplayed()
        val firstTarget = state.displayFingerOnScreen
        val firstAnchor = state.popupAnchor
        val doubleTapCenter = doubleTapNode.fetchSemanticsNode().boundsInRoot.center

        composeTestRule.runOnIdle {
            state.onDrag(doubleTapCenter - state.finger)
        }

        assertEquals(DoubleTap, state.pendingAction)
        assertEquals(firstTarget, state.displayFingerOnScreen)

        val itemBounds = listOf(tapNode, doubleTapNode, longPressNode).map {
            it.fetchSemanticsNode().boundsInRoot
        }
        val popupBounds = Rect(
            left = itemBounds.minOf { it.left },
            top = itemBounds.minOf { it.top },
            right = itemBounds.maxOf { it.right },
            bottom = itemBounds.maxOf { it.bottom }
        )
        val outside = Offset(popupBounds.right + 150f, popupBounds.center.y)
        composeTestRule.runOnIdle {
            state.onDrag(outside - state.finger)
        }

        assertFalse(state.showMoveScreenActionPopup)
        assertEquals(MoveScreenPhase.HoverPending, state.phase)
        assertEquals(firstTarget, state.displayFingerOnScreen)

        composeTestRule.runOnIdle {
            state.onDrag(Offset(5f, 0f))
        }
        val resumedTarget = firstTarget + Offset(15f, 0f)
        assertEquals(resumedTarget, state.displayFingerOnScreen)
        composeTestRule.runOnIdle {
            hoverGate.trySend(Unit).getOrThrow()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            state.showMoveScreenActionPopup && state.popupAnchor != firstAnchor
        }

        assertNotEquals(firstAnchor, state.popupAnchor)
        assertEquals(resumedTarget, state.displayFingerOnScreen)
        val result = JsonHelper.decodeFromString<MoveScreenData>(state.done().data)
        assertEquals(Tap, result.action)
        assertEquals(resumedTarget.x.toInt(), result.x)
        assertEquals(resumedTarget.y.toInt(), result.y)
        val rehoverDoubleTapNode = composeTestRule.onNodeWithText(
            context.getString(R.string.double_tap)
        )
        rehoverDoubleTapNode.assertIsDisplayed()
        val rehoverDoubleTapCenter = rehoverDoubleTapNode.fetchSemanticsNode().boundsInRoot.center
        assertNotEquals(doubleTapCenter, rehoverDoubleTapCenter)
        composeTestRule.runOnIdle {
            state.onDrag(rehoverDoubleTapCenter - state.finger)
        }

        assertEquals(DoubleTap, state.pendingAction)
        assertTrue(state.showMoveScreenActionPopup)
    }
}
