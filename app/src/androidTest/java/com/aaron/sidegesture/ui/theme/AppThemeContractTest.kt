package com.aaron.sidegesture.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Typography
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.feature.actionpanel.actionPanelMiniWindowAnimationSpec
import com.aaron.sidegesture.feature.actionpanel.actionPanelTextShadow
import com.aaron.sidegesture.ui.createNavigationAnimationSpec
import com.aaron.sidegesture.ui.theme.generator.AppTheme
import com.aaron.sidegesture.ui.theme.generator.AppTypography
import com.aaron.sidegesture.ui.widget.MyColorDisplay
import com.aaron.sidegesture.ui.widget.MyTextButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val THEMED_ITEM_TAG = "themed-item"
private const val THEMED_COLOR_DISPLAY_TAG = "themed-color-display"

@RunWith(AndroidJUnit4::class)
class AppThemeContractTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appThemeProvidesTokensAndCommonWidgetConsumesDimensions() {
        val customDimensions = AppDimensions(
            listItem = ListItemDimensions(singleLineMinHeight = 83.dp)
        )
        val customMotion = AppMotion(navigationDurationMillis = 321)
        var capturedDimensions: AppDimensions? = null
        var capturedMotion: AppMotion? = null
        var capturedTypography: Typography? = null
        var capturedShapes: Shapes? = null

        composeTestRule.setContent {
            AppTheme(
                darkTheme = false,
                dynamicColor = false,
                dimensions = customDimensions,
                motion = customMotion
            ) {
                val dimensionsFromTheme = MaterialTheme.dimensions
                val motionFromTheme = MaterialTheme.motion
                val typographyFromTheme = MaterialTheme.typography
                val shapesFromTheme = MaterialTheme.shapes
                SideEffect {
                    capturedDimensions = dimensionsFromTheme
                    capturedMotion = motionFromTheme
                    capturedTypography = typographyFromTheme
                    capturedShapes = shapesFromTheme
                }
                Box {
                    MyTextButton(
                        onClick = {},
                        text = "主题尺寸",
                        modifier = Modifier.testTag(THEMED_ITEM_TAG)
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        assertSame(customDimensions, capturedDimensions)
        assertSame(customMotion, capturedMotion)
        assertEquals(AppTypography, capturedTypography)
        assertEquals(AppShapes, capturedShapes)
        val bounds = composeTestRule.onNodeWithTag(THEMED_ITEM_TAG).getUnclippedBoundsInRoot()
        assertTrue(
            bounds.bottom - bounds.top >= customDimensions.listItem.singleLineMinHeight
        )
    }

    @Test
    fun tokenConsumersRecomposeWhenCustomDimensionsAndMotionChange() {
        val dimensionsState = mutableStateOf(
            AppDimensions(
                colorPreview = ColorPreviewDimensions(displaySize = 31.dp),
                actionPanel = ActionPanelDimensions(
                    textShadowOffset = 2.dp,
                    textShadowBlurRadius = 3.dp
                )
            )
        )
        val motionState = mutableStateOf(
            AppMotion(
                navigationDurationMillis = 321,
                actionPanelMiniWindowResizeStiffness = 1234f
            )
        )
        var capturedNavigationDuration = 0
        var capturedMiniWindowStiffness = 0f
        var capturedShadow = androidx.compose.ui.graphics.Shadow()
        var capturedDensity: Density? = null

        composeTestRule.setContent {
            AppTheme(
                darkTheme = false,
                dynamicColor = false,
                dimensions = dimensionsState.value,
                motion = motionState.value
            ) {
                val dimensions = MaterialTheme.dimensions
                val motion = MaterialTheme.motion
                val density = LocalDensity.current
                val navigationSpec = createNavigationAnimationSpec(motion)
                val miniWindowSpec = actionPanelMiniWindowAnimationSpec(motion)
                val shadow = actionPanelTextShadow(
                    dimensions = dimensions.actionPanel,
                    density = density,
                    color = MaterialTheme.appColors.fixedBlack
                )
                SideEffect {
                    capturedNavigationDuration = navigationSpec.durationMillis
                    capturedMiniWindowStiffness = miniWindowSpec.stiffness
                    capturedShadow = shadow
                    capturedDensity = density
                }
                MyColorDisplay(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(THEMED_COLOR_DISPLAY_TAG)
                )
            }
        }

        composeTestRule.waitForIdle()

        var bounds = composeTestRule
            .onNodeWithTag(THEMED_COLOR_DISPLAY_TAG)
            .getUnclippedBoundsInRoot()
        assertTrue(kotlin.math.abs((bounds.right - bounds.left).value - 31f) < 1f)
        assertEquals(321, capturedNavigationDuration)
        assertEquals(1234f, capturedMiniWindowStiffness, 0f)
        assertEquals(2f * requireNotNull(capturedDensity).density, capturedShadow.offset.x, 0.01f)
        assertEquals(3f * requireNotNull(capturedDensity).density, capturedShadow.blurRadius, 0.01f)

        composeTestRule.runOnIdle {
            dimensionsState.value = AppDimensions(
                colorPreview = ColorPreviewDimensions(displaySize = 57.dp),
                actionPanel = ActionPanelDimensions(
                    textShadowOffset = 5.dp,
                    textShadowBlurRadius = 7.dp
                )
            )
            motionState.value = AppMotion(
                navigationDurationMillis = 654,
                actionPanelMiniWindowResizeStiffness = 4321f
            )
        }
        composeTestRule.waitForIdle()

        bounds = composeTestRule
            .onNodeWithTag(THEMED_COLOR_DISPLAY_TAG)
            .getUnclippedBoundsInRoot()
        assertTrue(kotlin.math.abs((bounds.right - bounds.left).value - 57f) < 1f)
        assertEquals(654, capturedNavigationDuration)
        assertEquals(4321f, capturedMiniWindowStiffness, 0f)
        assertEquals(5f * requireNotNull(capturedDensity).density, capturedShadow.offset.x, 0.01f)
        assertEquals(7f * requireNotNull(capturedDensity).density, capturedShadow.blurRadius, 0.01f)
    }
}
