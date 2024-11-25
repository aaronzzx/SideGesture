package com.aaron.sidegesture.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aaron.compose.ktx.onClick
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.DividerHeight
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MainSecondaryTextPadding
import com.aaron.sidegesture.ui.theme.MarkColorSize
import com.aaron.sidegesture.ui.theme.MinItemHeight
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
import com.aaron.sidegesture.ui.theme.RootPadding
import com.aaron.sidegesture.ui.theme.ScrollBottomPadding
import com.aaron.sidegesture.ui.theme.SectionTitlePadding

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
            .padding(RootPadding)
            .padding(bottom = ScrollBottomPadding),
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
                    .padding(bottom = SectionTitlePadding)
                    .padding(horizontal = ContentPaddingHorizontal),
                text = title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
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
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinItemHeightNoSecondary)
                    .onClick {
                        onExpandedChange(!expanded)
                    }
                    .padding(
                        horizontal = ContentPaddingHorizontal,
                        vertical = ContentPaddingVertical
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ItemPadding)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sliderValueHint: Pair<String, String>? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinItemHeightNoSecondary)
            .padding(horizontal = ContentPaddingHorizontal, vertical = ContentPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(IconTextPadding)
    ) {
        Text(
            modifier = Modifier.width(IntrinsicSize.Max),
            text = text,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
        if (sliderValueHint != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
        val interactionSource = remember { MutableInteractionSource() }
        val colors = SliderDefaults.colors()
        Slider(
            modifier = Modifier.height(30.dp),
            enabled = enabled,
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            interactionSource = interactionSource,
            colors = colors,
            valueRange = valueRange,
            thumb = {
                SliderDefaults.Thumb(
                    modifier = Modifier.requiredSize(20.dp),
                    interactionSource = interactionSource,
                    colors = colors,
                    enabled = enabled
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    modifier = Modifier.height(10.dp),
                    colors = colors,
                    enabled = enabled,
                    sliderState = sliderState,
//                    thumbTrackGapSize = 0.dp
                )
            }
        )
    }
}

@Composable
fun MyTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondaryText: String = "",
    secondaryTextColor: Color = MaterialTheme.colorScheme.secondary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let {
                val minHeight = if (secondaryText.isEmpty()) {
                    MinItemHeightNoSecondary
                } else {
                    MinItemHeight
                }
                it.heightIn(min = minHeight)
            }
            .onSingleClick {
                onClick()
            }
            .padding(horizontal = ContentPaddingHorizontal, vertical = ContentPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ItemPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(MainSecondaryTextPadding)
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
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = text)
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
    markColor: Color = Color.Unspecified
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let {
                val minHeight = if (secondaryText.isEmpty()) {
                    MinItemHeightNoSecondary
                } else {
                    MinItemHeight
                }
                it.heightIn(min = minHeight)
            }
            .onSingleClick {
                if (onTextClick != null) {
                    onTextClick()
                } else {
                    onCheckedChange(!checked)
                }
            }
            .padding(horizontal = ContentPaddingHorizontal, vertical = ContentPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ItemPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(MainSecondaryTextPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(IconTextPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (markColor.isSpecified) {
                    Box(
                        modifier = Modifier
                            .size(MarkColorSize)
                            .background(color = markColor, shape = CircleShape)
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
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
        if (onTextClick != null) {
            VerticalDivider(
                modifier = Modifier.height(DividerHeight),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 0.dp
        ) {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}