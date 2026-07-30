package com.aaron.sidegesture.ui.screen.animationstyle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBorder
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.ui.screen.animationstyle.AnimationStyleSelectVM.AnimationStyleItem
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.TopBar

/**
 * @author aaronzzxup@gmail.com
 * @since 2026/5/20
 */
@Composable
fun AnimationStyleSelectScreen(
    onBack: () -> Unit,
    onNavToStyleConfig: (Int) -> Unit,
    vm: AnimationStyleSelectVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        androidx.compose.foundation.layout.Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.animation_style)
            )
            MyColumn(
                verticalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimensions.styleCard.listSpacing
                )
            ) {
                uiState.items.forEach { item ->
                    AnimationStyleCard(
                        item = item,
                        selected = item.type == uiState.currentType,
                        onClick = { vm.onStyleSelected(item.type) },
                        onSettingsClick = { onNavToStyleConfig(item.type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimationStyleCard(
    item: AnimationStyleItem,
    selected: Boolean,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = MaterialTheme.alpha.subtleBorder)
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = MaterialTheme.alpha.subtleContainer)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBorder(
                width = MaterialTheme.dimensions.styleCard.selectedBorderWidth,
                color = borderColor,
                shape = shape
            )
            .onSingleClick { onClick() },
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaterialTheme.dimensions.styleCard.minHeight)
                .padding(
                    horizontal = MaterialTheme.dimensions.layout.contentHorizontalPadding,
                    vertical = MaterialTheme.dimensions.layout.contentVerticalPaddingWithSection
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                MaterialTheme.dimensions.listItem.contentGap
            )
        ) {
            Box(
                modifier = Modifier.size(MaterialTheme.dimensions.styleCard.previewSize)
            ) {
                when (item.type) {
                    AnimationStyles.TYPE_WAVE -> WaveStylePreview(
                        modifier = Modifier.fillMaxSize()
                    )
                    AnimationStyles.TYPE_CAPSULE -> CapsuleStylePreview(
                        modifier = Modifier.fillMaxSize()
                    )
                    AnimationStyles.TYPE_BUBBLE -> BubbleStylePreview(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = item.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    modifier = Modifier.padding(
                        top = MaterialTheme.dimensions.styleCard.subtitleTopPadding
                    ),
                    text = stringResource(id = item.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected && item.hasSettings) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.styleCard.radioSize)
                        .clipToBorder(
                            width = MaterialTheme.dimensions.styleCard.radioBorderWidth,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = MaterialTheme.alpha.selectedIndicator
                            ),
                            shape = CircleShape
                        )
                        .onSingleClick { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(MaterialTheme.dimensions.styleCard.editIconSize),
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleStylePreview(
    modifier: Modifier = Modifier
) {
    PreviewStage(modifier = modifier) {
        val colorScheme = MaterialTheme.colorScheme
        val iconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward)
        val styleCardDimensions = MaterialTheme.dimensions.styleCard
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thickness = styleCardDimensions.capsuleThickness.toPx()
            val capsuleWidth = styleCardDimensions.capsuleWidth.toPx()
            val cornerRadius = styleCardDimensions.capsuleCornerRadius.toPx()
            val startX = -capsuleWidth * 0.20f
            val top = size.height / 2f - thickness / 2f
            val center = Offset(startX + capsuleWidth / 2f, top + thickness / 2f)

            drawRoundRect(
                color = colorScheme.primary,
                topLeft = Offset(startX, top),
                size = Size(capsuleWidth, thickness),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            val iconSize = styleCardDimensions.capsuleIconSize.toPx()
            rotate(0f, pivot = center) {
                translate(left = center.x, top = center.y - iconSize / 2f) {
                    drawPreviewIcon(
                        painter = iconPainter,
                        iconSize = iconSize,
                        tint = colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveStylePreview(
    modifier: Modifier = Modifier
) {
    PreviewStage(modifier = modifier) {
        val colorScheme = MaterialTheme.colorScheme
        val iconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val width = size.width
            val height = size.height
            val top = height * 0.02f
            val bottom = height * 0.98f
            val centerY = height / 2f
            val neckX = width * 0.06f
            val peakX = width * 0.4f

            path.moveTo(0f, 0f)
            path.lineTo(neckX, top)
            path.cubicTo(
                x1 = neckX,
                y1 = height * 0.22f,
                x2 = peakX,
                y2 = height * 0.26f,
                x3 = peakX,
                y3 = centerY
            )
            path.cubicTo(
                x1 = peakX,
                y1 = height * 0.74f,
                x2 = neckX,
                y2 = height * 0.78f,
                x3 = neckX,
                y3 = bottom
            )
            path.lineTo(0f, height)
            path.close()

            drawPath(path = path, color = colorScheme.primary)
        }

        Image(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MaterialTheme.dimensions.styleCard.waveIconStartPadding)
                .size(MaterialTheme.dimensions.styleCard.previewIconSize),
            painter = iconPainter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorScheme.onPrimary)
        )
    }
}

@Composable
private fun BubbleStylePreview(
    modifier: Modifier = Modifier
) {
    PreviewStage(modifier = modifier) {
        val colorScheme = MaterialTheme.colorScheme
        val iconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward)
        val bubbleDiameter = MaterialTheme.dimensions.styleCard.bubbleDiameter
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = bubbleDiameter.toPx()
            val radius = diameter / 2f
            val center = Offset(x = radius * 0.65f, y = size.height / 2f)

            drawCircle(
                color = colorScheme.primary,
                radius = radius,
                center = center
            )
        }

        Image(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MaterialTheme.dimensions.styleCard.bubbleIconStartPadding)
                .size(MaterialTheme.dimensions.styleCard.previewIconSize),
            painter = iconPainter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorScheme.onPrimary)
        )
    }
}

@Composable
private fun PreviewStage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = colorScheme.surfaceContainerLowest
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.dimensions.styleCard.previewBorderPadding)
                .clipToBorder(
                    width = MaterialTheme.dimensions.styleCard.radioBorderWidth,
                    color = colorScheme.outlineVariant.copy(
                        alpha = MaterialTheme.alpha.previewDivider
                    ),
                    shape = MaterialTheme.shapes.small
                ),
            content = content
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewIcon(
    painter: Painter,
    iconSize: Float,
    tint: Color
) {
    with(painter) {
        draw(
            size = Size(iconSize, iconSize),
            colorFilter = ColorFilter.tint(tint)
        )
    }
}

