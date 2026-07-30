package com.aaron.sidegesture.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Typography
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.ui.theme.generator.AppTheme
import com.aaron.sidegesture.ui.theme.generator.AppTypography
import com.aaron.sidegesture.ui.widget.MyTextButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val THEMED_ITEM_TAG = "themed-item"

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
}
