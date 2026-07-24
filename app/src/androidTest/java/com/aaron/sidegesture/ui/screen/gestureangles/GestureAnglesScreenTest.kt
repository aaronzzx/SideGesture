package com.aaron.sidegesture.ui.screen.gestureangles

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.entity.GestureAngles
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAnglesVM.UiState
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GestureAnglesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun topPositionButtonReceivesTouchBelowTopBar() {
        var uiState by mutableStateOf(UiState())

        composeTestRule.setContent {
            MaterialTheme {
                GestureAnglesContent(
                    uiState = uiState,
                    onBack = {},
                    onShowResetWarningDialog = {},
                    onReset = {},
                    onSave = {},
                    onSwitchPosition = { position ->
                        uiState = uiState.copy(position = position)
                    },
                    onAngleChange = {}
                )
            }
        }

        composeTestRule
            .onNodeWithTag("gesture_angle_position_${Position.Top.name}")
            .assertIsDisplayed()
            .performTouchInput {
                down(center)
                up()
            }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("gesture_angle_content_${Position.Top.name}")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("gesture_angle_position_${Position.Top.name}")
            .assertDoesNotExist()
    }

    @Test
    fun topAngleHandleRespondsToDrag() {
        var uiState by mutableStateOf(
            UiState(
                angle = GestureAngles().top,
                position = Position.Top
            )
        )
        var changeCount = 0
        val originalP1 = uiState.angle.p1

        composeTestRule.setContent {
            MaterialTheme {
                GestureAnglesContent(
                    uiState = uiState,
                    onBack = {},
                    onShowResetWarningDialog = {},
                    onReset = {},
                    onSave = {},
                    onSwitchPosition = {},
                    onAngleChange = { angle ->
                        uiState = uiState.copy(angle = angle)
                        changeCount++
                    }
                )
            }
        }

        composeTestRule
            .onNodeWithTag("gesture_angle_canvas_${Position.Top.name}")
            .assertIsDisplayed()
            .performTouchInput {
                val radius = minOf(width, height) / 4f
                val degree = originalP1 * 180f
                val radians = Math.toRadians(degree.toDouble())
                val opposite = radius * sin(radians).toFloat()
                val neighbor = sqrt(radius.pow(2) - opposite.pow(2))
                val handle = Offset(
                    x = width / 2f - neighbor,
                    y = opposite
                )
                down(handle)
                moveTo(handle + Offset(20f, 40f))
                up()
            }
        composeTestRule.waitForIdle()

        assertTrue("top handle drag must emit an angle change", changeCount > 0)
        assertNotEquals(originalP1, uiState.angle.p1)
    }
}
