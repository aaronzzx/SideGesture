package com.aaron.sidegesture.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val RANGE_TAG = "slider-display-range"

@RunWith(AndroidJUnit4::class)
class SliderDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun singleSliderUpdatesTextBeforePointerUpAndFinishesOnce() {
        var value by mutableStateOf(0.25f)
        var changeCount = 0
        var finishedCount = 0

        composeTestRule.setContent {
            SliderTestTheme {
                MyTextSlider(
                    value = value,
                    onValueChange = {
                        value = it
                        changeCount++
                    },
                    onValueChangeFinished = { finishedCount++ },
                    text = "单值滑块",
                    sliderValueHint = "小" to "大",
                    valueFormatter = { formatSliderDecimal(it, 2) }
                )
            }
        }

        val slider = composeTestRule.onNode(progressSliderMatcher)
        slider.performTouchInput {
            down(center)
            moveBy(Offset(width * 0.25f, 0f))
        }
        composeTestRule.waitForIdle()

        assertTrue("pointer move must call onValueChange", changeCount > 0)
        assertEquals("pointer is still down", 0, finishedCount)
        assertTrue("value must move", abs(value - 0.25f) > 0.001f)
        composeTestRule.onNodeWithText(formatSliderDecimal(value, 2)).assertIsDisplayed()
        composeTestRule.onNodeWithText("小").assertIsDisplayed()
        composeTestRule.onNodeWithText("大").assertIsDisplayed()

        slider.performTouchInput { up() }
        composeTestRule.waitForIdle()
        assertEquals(1, finishedCount)
    }

    @Test
    fun rangeSliderDragsBothThumbsAndClampsCrossover() {
        var range by mutableStateOf(0.2f..0.8f)
        var finishedCount = 0
        var touchSlopPx by mutableStateOf(0f)

        composeTestRule.setContent {
            SliderTestTheme {
                val viewConfiguration = LocalViewConfiguration.current
                SideEffect {
                    touchSlopPx = viewConfiguration.touchSlop
                }
                MyTextRangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    onValueChangeFinished = { finishedCount++ },
                    text = "区间滑块",
                    sliderValueHint = "起点" to "终点",
                    valueFormatter = ::formatSliderPercentageRange,
                    sliderModifier = Modifier.testTag(RANGE_TAG)
                )
            }
        }

        composeTestRule.waitForIdle()
        assertTrue(touchSlopPx > 0f)
        assertEquals(0.2f, range.start, 1e-6f)
        assertEquals(0.8f, range.endInclusive, 1e-6f)

        val rangeSlider = composeTestRule.onNodeWithTag(RANGE_TAG)
        val rangeRect = rangeSlider.fetchSemanticsNode().boundsInRoot
        val thumbNodes = composeTestRule
            .onAllNodes(progressSliderMatcher)
            .fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.center.x }
        val origin = Offset(rangeRect.left, rangeRect.top)
        val startCenter = thumbNodes.first().boundsInRoot.center - origin
        val endCenter = thumbNodes.last().boundsInRoot.center - origin
        val minX = 1f
        val maxX = rangeRect.width - 1f

        rangeSlider.performTouchInput {
            down(startCenter)
            moveBy(Offset(-(touchSlopPx + 1f), 0f))
            moveTo(Offset(minX, startCenter.y))
        }
        composeTestRule.waitForIdle()
        assertEquals(0, finishedCount)
        assertTrue(range.start <= range.endInclusive)
        assertTrue(range.start >= 0f && range.endInclusive <= 1f)
        assertEquals(0f, range.start, 1e-6f)
        assertEquals("0% – 80%", formatSliderPercentageRange(range))
        composeTestRule.onNodeWithText("0% – 80%").assertIsDisplayed()

        rangeSlider.performTouchInput { up() }
        composeTestRule.waitForIdle()
        assertEquals(1, finishedCount)

        rangeSlider.performTouchInput {
            down(endCenter)
            moveBy(Offset(touchSlopPx + 1f, 0f))
            moveTo(Offset(maxX, endCenter.y))
        }
        composeTestRule.waitForIdle()
        assertEquals(1, finishedCount)
        assertTrue(range.start <= range.endInclusive)
        assertTrue(range.start >= 0f && range.endInclusive <= 1f)
        assertEquals(1f, range.endInclusive, 1e-6f)
        assertEquals("0% – 100%", formatSliderPercentageRange(range))
        composeTestRule.onNodeWithText("0% – 100%").assertIsDisplayed()

        rangeSlider.performTouchInput { up() }
        composeTestRule.waitForIdle()
        assertEquals(2, finishedCount)

        val currentRangeRect = rangeSlider.fetchSemanticsNode().boundsInRoot
        val currentOrigin = Offset(currentRangeRect.left, currentRangeRect.top)
        val currentStartThumb = composeTestRule
            .onAllNodes(progressSliderMatcher)
            .fetchSemanticsNodes()
            .sortedBy { it.boundsInRoot.center.x }
            .first()
        val currentStartCenter = currentStartThumb.boundsInRoot.center - currentOrigin
        rangeSlider.performTouchInput {
            down(currentStartCenter)
            moveBy(Offset(touchSlopPx + 1f, 0f))
            moveTo(Offset(maxX, currentStartCenter.y))
        }
        composeTestRule.waitForIdle()
        assertEquals(2, finishedCount)
        assertTrue(range.start <= range.endInclusive)
        assertTrue(range.start >= 0f && range.endInclusive <= 1f)
        assertEquals(1f, range.start, 1e-6f)
        assertEquals("100% – 100%", formatSliderPercentageRange(range))
        composeTestRule.onNodeWithText("100% – 100%").assertIsDisplayed()

        rangeSlider.performTouchInput { up() }
        composeTestRule.waitForIdle()
        assertEquals(3, finishedCount)
        composeTestRule.onNodeWithText("起点").assertIsDisplayed()
        composeTestRule.onNodeWithText("终点").assertIsDisplayed()
    }

    @Test
    fun longTitlesEllipsizeAndValueStaysSingleLineWithHint() {
        val title = "这是一个非常非常长的 Slider 标题，用于验证单行省略行为"
        val valueText = "0.50 – 1.00"
        composeTestRule.setContent {
            SliderTestTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                    Box(modifier = Modifier.width(240.dp)) {
                        MyTextRangeSlider(
                            value = 0.5f..1f,
                            onValueChange = {},
                            text = title,
                            sliderValueHint = "0" to "1",
                            valueFormatter = { valueText }
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val titleLayout = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it.invoke(titleLayout)
        }
        assertEquals(1, titleLayout.single().lineCount)
        assertTrue(titleLayout.single().isLineEllipsized(0))

        val valueLayout = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(valueText).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it.invoke(valueLayout)
        }
        assertEquals(1, valueLayout.single().lineCount)
        val titleBounds = composeTestRule.onNodeWithText(title).getUnclippedBoundsInRoot()
        val valueBounds = composeTestRule.onNodeWithText(valueText).getUnclippedBoundsInRoot()
        assertTrue(titleBounds.right <= valueBounds.left)
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    private val progressSliderMatcher = SemanticsMatcher("slider with SetProgress action") { node ->
        node.config.contains(SemanticsActions.SetProgress)
    }
}

@Composable
private fun SliderTestTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
