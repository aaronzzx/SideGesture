package com.aaron.sidegesture.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areNavigationBarsVisible
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.aaron.compose.ktx.onClick
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.componentShapes
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.motion
import java.util.Locale
import kotlin.math.roundToInt

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@Composable
fun MyColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .padding(MaterialTheme.dimensions.layout.screenPadding)
            .padding(bottom = MaterialTheme.dimensions.layout.scrollBottomPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}
@Composable
fun MySection(
    modifier: Modifier = Modifier,
    title: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .padding(bottom = MaterialTheme.dimensions.layout.sectionTitleSpacing)
                    .padding(horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding),
                text = title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun MyExpandableColumn(
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MaterialTheme.dimensions.listItem.singleLineMinHeight)
                    .onClick {
                        onExpandedChange(!expanded)
                    }
                    .padding(
                        horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding,
                        vertical = MaterialTheme.dimensions.layout.contentVerticalPaddingWithSection
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.listItem.contentGap)
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 0f else -90f,
                    label = "ArrowDropDownRotation"
                )
                Icon(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotation
                    },
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = title
                )
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    content()
                }
            }
        }
    }
}

@Composable
fun MyTextSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sliderValueHint: Pair<String, String>? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueFormatter: (Float) -> String
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.dimensions.listItem.singleLineMinHeight)
            .padding(vertical = MaterialTheme.dimensions.layout.contentVerticalPaddingWithSection),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.listItem.iconTextGap)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding)
                .fillMaxWidth(),
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (sliderValueHint != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding)
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart),
                    text = sliderValueHint.first,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    text = sliderValueHint.second,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
        MySlider(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.dimensions.slider.horizontalPadding)
                .height(MaterialTheme.dimensions.slider.containerHeight),
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            valueFormatter = valueFormatter
        )
    }
}

@Composable
fun MyTextRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sliderValueHint: Pair<String, String>? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueFormatter: (Float) -> String,
    sliderModifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MaterialTheme.dimensions.listItem.singleLineMinHeight)
            .padding(vertical = MaterialTheme.dimensions.layout.contentVerticalPaddingWithSection),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.listItem.iconTextGap)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding)
                .fillMaxWidth(),
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (sliderValueHint != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding)
                    .fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart),
                    text = sliderValueHint.first,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    text = sliderValueHint.second,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
        MyRangeSlider(
            modifier = sliderModifier
                .padding(horizontal = MaterialTheme.dimensions.slider.horizontalPadding)
                .height(MaterialTheme.dimensions.slider.containerHeight),
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            valueFormatter = valueFormatter
        )
    }
}

fun formatSliderInteger(value: Float, suffix: String = ""): String =
    String.format(Locale.ROOT, "%.0f%s", value, suffix)

fun formatSliderDecimal(value: Float, decimals: Int, suffix: String = ""): String =
    String.format(Locale.ROOT, "%.${decimals}f%s", value, suffix)

fun formatSliderPercentage(value: Float): String =
    formatSliderInteger(value * 100f, "%")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueFormatter: ((Float) -> String)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(thumbColor = colorScheme.primary)
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var sliderLayoutBounds by remember { mutableStateOf<SliderLayoutBounds?>(null) }
    Slider(
        modifier = modifier.onGloballyPositioned { coordinates ->
            sliderLayoutBounds = coordinates.toSliderLayoutBounds()
        },
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        colors = colors,
        valueRange = valueRange,
        thumb = { sliderState ->
            val layoutBounds = sliderLayoutBounds
            SliderValueThumb(
                interactionSource = interactionSource,
                colors = colors,
                enabled = enabled,
                colorScheme = colorScheme,
                bubbleText = valueFormatter?.invoke(sliderState.value),
                sliderLayoutBounds = layoutBounds,
                thumbCenterPx = layoutBounds?.let {
                    calculateSliderThumbCenterPx(
                        value = sliderState.value,
                        valueRange = valueRange,
                        sliderWidthPx = it.boundsInWindow.width,
                        thumbWidthPx = with(density) {
                            MaterialTheme.dimensions.slider.thumbSize.roundToPx()
                        },
                        isRtl = isRtl
                    )
                }
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(MaterialTheme.dimensions.slider.trackHeight),
                colors = colors,
                enabled = enabled,
                sliderState = sliderState,
                thumbTrackGapSize = 0.dp
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueFormatter: ((Float) -> String)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(thumbColor = colorScheme.primary)
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var sliderLayoutBounds by remember { mutableStateOf<SliderLayoutBounds?>(null) }
    RangeSlider(
        modifier = modifier.onGloballyPositioned { coordinates ->
            sliderLayoutBounds = coordinates.toSliderLayoutBounds()
        },
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        colors = colors,
        valueRange = valueRange,
        startThumb = {
            val layoutBounds = sliderLayoutBounds
            SliderValueThumb(
                interactionSource = startInteractionSource,
                colors = colors,
                enabled = enabled,
                colorScheme = colorScheme,
                bubbleText = valueFormatter?.invoke(value.start),
                sliderLayoutBounds = layoutBounds,
                thumbCenterPx = layoutBounds?.let {
                    calculateSliderThumbCenterPx(
                        value = value.start,
                        valueRange = valueRange,
                        sliderWidthPx = it.boundsInWindow.width,
                        thumbWidthPx = with(density) {
                            MaterialTheme.dimensions.slider.thumbSize.roundToPx()
                        },
                        isRtl = isRtl
                    )
                }
            )
        },
        endThumb = {
            val layoutBounds = sliderLayoutBounds
            SliderValueThumb(
                interactionSource = endInteractionSource,
                colors = colors,
                enabled = enabled,
                colorScheme = colorScheme,
                bubbleText = valueFormatter?.invoke(value.endInclusive),
                sliderLayoutBounds = layoutBounds,
                thumbCenterPx = layoutBounds?.let {
                    calculateSliderThumbCenterPx(
                        value = value.endInclusive,
                        valueRange = valueRange,
                        sliderWidthPx = it.boundsInWindow.width,
                        thumbWidthPx = with(density) {
                            MaterialTheme.dimensions.slider.thumbSize.roundToPx()
                        },
                        isRtl = isRtl
                    )
                }
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                modifier = Modifier.height(MaterialTheme.dimensions.slider.trackHeight),
                colors = colors,
                enabled = enabled,
                rangeSliderState = sliderState,
                thumbTrackGapSize = 0.dp
            )
        }
    )
}

data class SliderBubbleHorizontalPlacement(
    val bodyLeftPx: Int,
    val pointerCenterInBodyPx: Int
)

data class SliderBubblePointerHorizontalPlacement(
    val baseStartInBodyPx: Int,
    val tipInBodyPx: Int,
    val baseEndInBodyPx: Int
)

data class SliderBubbleVerticalPlacement(
    val visiblePointerHeightPx: Int,
    val pointerToThumbGapPx: Int
)

fun calculateSliderBubbleHorizontalPlacement(
    containerWidthPx: Int,
    thumbCenterPx: Int,
    bubbleWidthPx: Int
): SliderBubbleHorizontalPlacement {
    val safeContainerWidth = containerWidthPx.coerceAtLeast(0)
    val safeBubbleWidth = bubbleWidthPx.coerceIn(0, safeContainerWidth)
    val safeThumbCenter = thumbCenterPx.coerceIn(0, safeContainerWidth)
    val maxBodyLeft = safeContainerWidth - safeBubbleWidth
    val bodyLeft = (safeThumbCenter - safeBubbleWidth / 2).coerceIn(0, maxBodyLeft)
    return SliderBubbleHorizontalPlacement(
        bodyLeftPx = bodyLeft,
        pointerCenterInBodyPx = safeThumbCenter - bodyLeft
    )
}

fun calculateSliderBubblePointerHorizontalPlacement(
    bodyWidthPx: Int,
    pointerCenterInBodyPx: Int,
    pointerWidthPx: Int,
    cornerInsetPx: Int
): SliderBubblePointerHorizontalPlacement {
    val safeBodyWidth = bodyWidthPx.coerceAtLeast(0)
    val safeTip = pointerCenterInBodyPx.coerceIn(0, safeBodyWidth)
    val safeCornerInset = cornerInsetPx.coerceIn(0, safeBodyWidth / 2)
    val flatEdgeStart = safeCornerInset
    val flatEdgeEnd = safeBodyWidth - safeCornerInset
    val pointerHalfWidth = pointerWidthPx.coerceAtLeast(0) / 2
    return SliderBubblePointerHorizontalPlacement(
        baseStartInBodyPx = (safeTip - pointerHalfWidth).coerceIn(flatEdgeStart, flatEdgeEnd),
        tipInBodyPx = safeTip,
        baseEndInBodyPx = (safeTip + pointerHalfWidth).coerceIn(flatEdgeStart, flatEdgeEnd)
    )
}

fun calculateSliderBubbleVerticalPlacement(
    pointerHeightPx: Int,
    pointerOverlapPx: Int,
    bodyToThumbClearancePx: Int
): SliderBubbleVerticalPlacement {
    val safePointerHeight = pointerHeightPx.coerceAtLeast(0)
    val safePointerOverlap = pointerOverlapPx.coerceIn(0, safePointerHeight)
    val visiblePointerHeight = safePointerHeight - safePointerOverlap
    return SliderBubbleVerticalPlacement(
        visiblePointerHeightPx = visiblePointerHeight,
        pointerToThumbGapPx = (
            bodyToThumbClearancePx.coerceAtLeast(0) - visiblePointerHeight
        ).coerceAtLeast(0)
    )
}

fun calculateSliderThumbCenterPx(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    sliderWidthPx: Int,
    thumbWidthPx: Int,
    isRtl: Boolean
): Int {
    val safeSliderWidth = sliderWidthPx.coerceAtLeast(0)
    val safeThumbWidth = thumbWidthPx.coerceIn(0, safeSliderWidth)
    val fraction = if (valueRange.start == valueRange.endInclusive) {
        0f
    } else {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    }
    val logicalCenter = safeThumbWidth / 2f + (safeSliderWidth - safeThumbWidth) * fraction
    val physicalCenter = if (isRtl) safeSliderWidth - logicalCenter else logicalCenter
    return physicalCenter.roundToInt().coerceIn(0, safeSliderWidth)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderValueThumb(
    interactionSource: MutableInteractionSource,
    colors: SliderColors,
    enabled: Boolean,
    colorScheme: ColorScheme,
    bubbleText: String?,
    sliderLayoutBounds: SliderLayoutBounds?,
    thumbCenterPx: Int?
) {
    val isDragged by interactionSource.collectIsDraggedAsState()
    val thumbCenterRadius = MaterialTheme.dimensions.slider.thumbCenterRadius
    Box {
        SliderDefaults.Thumb(
            modifier = Modifier
                .requiredSize(MaterialTheme.dimensions.slider.thumbSize)
                .drawWithContent {
                    drawContent()
                    if (enabled) {
                        drawCircle(
                            color = colorScheme.onPrimary,
                            radius = thumbCenterRadius.toPx()
                        )
                    }
                },
            interactionSource = interactionSource,
            colors = colors,
            enabled = enabled
        )
        if (
            enabled &&
            isDragged &&
            bubbleText != null &&
            sliderLayoutBounds != null &&
            thumbCenterPx != null
        ) {
            SliderValueBubblePopup(
                text = bubbleText,
                sliderLayoutBounds = sliderLayoutBounds,
                thumbCenterPx = thumbCenterPx
            )
        }
    }
}
@Composable
private fun SliderValueBubblePopup(
    text: String,
    sliderLayoutBounds: SliderLayoutBounds,
    thumbCenterPx: Int
) {
    val density = LocalDensity.current
    val popupPositionProvider = remember(sliderLayoutBounds.boundsInWindow) {
        SliderValuePopupPositionProvider(sliderLayoutBounds.boundsInWindow)
    }
    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = true
        )
    ) {
        SliderValueBubble(
            modifier = Modifier.requiredWidth(
                with(density) { sliderLayoutBounds.boundsInWindow.width.toDp() }
            ),
            text = text,
            thumbCenterPx = thumbCenterPx
        )
    }
}

@Composable
private fun SliderValueBubble(
    text: String,
    thumbCenterPx: Int,
    modifier: Modifier = Modifier
) {
    val bubbleColor = MaterialTheme.colorScheme.primary
    val bubbleContentColor = MaterialTheme.colorScheme.onPrimary
    val sliderDimensions = MaterialTheme.dimensions.slider
    val sliderBubbleShape = MaterialTheme.componentShapes.sliderBubble
    val sliderBubbleResizeDurationMillis =
        MaterialTheme.motion.sliderBubbleResizeDurationMillis
    SubcomposeLayout(modifier = modifier) { constraints ->
        val bodyPlaceable = subcompose(SliderBubbleSlot.Body) {
            Box(
                modifier = Modifier
                    .testTag(SliderBubbleTestTag)
                    .background(
                        color = bubbleColor,
                        shape = sliderBubbleShape
                    )
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = sliderBubbleResizeDurationMillis
                        ),
                        alignment = Alignment.Center
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = sliderDimensions.bubbleHorizontalPadding,
                        vertical = sliderDimensions.bubbleVerticalPadding
                    ),
                    text = text,
                    color = bubbleContentColor,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }.single().measure(
            Constraints(
                maxWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight
            )
        )
        val pointerWidthPx = sliderDimensions.bubblePointerWidth.roundToPx()
        val pointerHeightPx = sliderDimensions.bubblePointerHeight.roundToPx()
        val pointerOverlapPx = sliderDimensions.bubblePointerOverlap.roundToPx()
        val layoutWidth = constraints.maxWidth
        val placement = calculateSliderBubbleHorizontalPlacement(
            containerWidthPx = layoutWidth,
            thumbCenterPx = thumbCenterPx,
            bubbleWidthPx = bodyPlaceable.width
        )
        val pointerPlacement = calculateSliderBubblePointerHorizontalPlacement(
            bodyWidthPx = bodyPlaceable.width,
            pointerCenterInBodyPx = placement.pointerCenterInBodyPx,
            pointerWidthPx = pointerWidthPx,
            cornerInsetPx = sliderDimensions.bubbleCornerInset.roundToPx()
        )
        val pointerPlaceable = subcompose(SliderBubbleSlot.Pointer) {
            Canvas(modifier = Modifier) {
                val pointerPath = Path().apply {
                    moveTo(pointerPlacement.baseStartInBodyPx.toFloat(), 0f)
                    lineTo(pointerPlacement.baseEndInBodyPx.toFloat(), 0f)
                    lineTo(pointerPlacement.tipInBodyPx.toFloat(), size.height)
                    close()
                }
                drawPath(path = pointerPath, color = bubbleColor)
            }
        }.single().measure(Constraints.fixed(bodyPlaceable.width, pointerHeightPx))
        val verticalPlacement = calculateSliderBubbleVerticalPlacement(
            pointerHeightPx = pointerPlaceable.height,
            pointerOverlapPx = pointerOverlapPx,
            bodyToThumbClearancePx = sliderDimensions.bubbleBodyToThumbClearance.roundToPx()
        )
        layout(
            layoutWidth,
            bodyPlaceable.height + verticalPlacement.visiblePointerHeightPx +
                verticalPlacement.pointerToThumbGapPx
        ) {
            bodyPlaceable.place(placement.bodyLeftPx, 0)
            pointerPlaceable.place(
                placement.bodyLeftPx,
                bodyPlaceable.height - pointerOverlapPx
            )
        }
    }
}

private data class SliderLayoutBounds(
    val boundsInWindow: IntRect
)

private class SliderValuePopupPositionProvider(
    private val sliderBoundsInWindow: IntRect
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(
        x = sliderBoundsInWindow.left,
        y = anchorBounds.center.y - popupContentSize.height
    )
}

private fun androidx.compose.ui.layout.LayoutCoordinates.toSliderLayoutBounds(): SliderLayoutBounds {
    val position = positionInWindow()
    return SliderLayoutBounds(
        boundsInWindow = IntRect(
            left = position.x.roundToInt(),
            top = position.y.roundToInt(),
            right = (position.x + size.width).roundToInt(),
            bottom = (position.y + size.height).roundToInt()
        )
    )
}

private enum class SliderBubbleSlot {
    Body,
    Pointer
}

private const val SliderBubbleTestTag = "slider-value-bubble"

@Composable
fun MyTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondaryText: String = "",
    secondaryTextColor: Color = MaterialTheme.colorScheme.secondary,
    prefix: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding,
        vertical = MaterialTheme.dimensions.layout.contentVerticalPaddingWithSection
    )
) {
    val disabledAlpha = MaterialTheme.alpha.disabledItem
    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else disabledAlpha
            }
            .fillMaxWidth()
            .let {
                val minHeight = if (secondaryText.isEmpty()) {
                    MaterialTheme.dimensions.listItem.singleLineMinHeight
                } else {
                    MaterialTheme.dimensions.listItem.withSupportingTextMinHeight
                }
                it.heightIn(min = minHeight)
            }
            .onSingleClick(enabled = enabled) {
                onClick()
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.listItem.contentGap)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.listItem.iconTextGap)
        ) {
            prefix?.invoke()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimensions.listItem.titleSupportingTextGap
                )
            ) {
                Text(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (secondaryText.isNotEmpty()) {
                    Text(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        text = secondaryText,
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (enabled) {
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = text)
        }
    }
}

@Composable
fun MyTextSwitch(
    onCheckedChange: (Boolean) -> Unit,
    checked: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTextClick: (() -> Unit)? = null,
    secondaryText: String = "",
    secondaryTextColor: Color = MaterialTheme.colorScheme.secondary,
    mainTextMaxLines: Int = 1,
    secondaryTextMaxLines: Int = 2,
    markColor: Color = Color.Unspecified,
    mainSecondaryTextPadding: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding,
        vertical = MaterialTheme.dimensions.layout.contentVerticalPaddingWithSection
    )
) {
    val disabledAlpha = MaterialTheme.alpha.disabledItem
    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else disabledAlpha
            }
            .fillMaxWidth()
            .let {
                val minHeight = if (secondaryText.isEmpty()) {
                    MaterialTheme.dimensions.listItem.singleLineMinHeight
                } else {
                    MaterialTheme.dimensions.listItem.withSupportingTextMinHeight
                }
                it.heightIn(min = minHeight)
            }
            .onSingleClick(enabled = enabled) {
                if (onTextClick != null) {
                    onTextClick()
                } else {
                    onCheckedChange(!checked)
                }
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.listItem.contentGap)
    ) {
        val mainSecondaryPadding = when (mainSecondaryTextPadding) {
            true -> MaterialTheme.dimensions.listItem.titleSupportingTextGap
            else -> 0.dp
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(mainSecondaryPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimensions.listItem.iconTextGap
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f, false),
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = mainTextMaxLines
                )
                if (markColor.isSpecified) {
                    Box(
                        modifier = Modifier
                            .size(MaterialTheme.dimensions.listItem.markerSize)
                            .background(color = markColor, shape = CircleShape)
                    )
                }
            }
            if (secondaryText.isNotEmpty()) {
                Text(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    text = secondaryText,
                    color = secondaryTextColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = secondaryTextMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onTextClick != null) {
            VerticalDivider(
                modifier = Modifier.height(MaterialTheme.dimensions.listItem.dividerSlotHeight),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 0.dp
        ) {
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) }
) {
    val paddingBottom = when (WindowInsets.areNavigationBarsVisible) {
        true -> 0.dp
        else -> MaterialTheme.dimensions.layout.scrollBottomPadding
    }
    SnackbarHost(
        modifier = modifier.padding(bottom = paddingBottom),
        hostState = hostState,
        snackbar = snackbar
    )
}

@Composable
fun MyColorDisplay(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = MaterialTheme.dimensions.colorPreview.displaySize,
                minHeight = MaterialTheme.dimensions.colorPreview.displaySize
            )
            .background(
                color = color,
                shape = CircleShape
            )
            .border(
                width = MaterialTheme.dimensions.colorPreview.borderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
    ) {
        if (color.isUnspecified || color == Color.Transparent) {
            Icon(
                modifier = Modifier.matchParentSize(),
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
