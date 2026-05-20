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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
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
            MyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBorder(width = 1.5.dp, color = borderColor, shape = shape)
            .onSingleClick { onClick() },
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinItemHeightNoSecondary + 28.dp)
                .padding(
                    horizontal = ContentPaddingHorizontal,
                    vertical = ContentPaddingVerticalWithSection
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ItemPadding)
        ) {
            Box(
                modifier = Modifier.size(80.dp)
            ) {
                when (item.type) {
                    AnimationStyles.TYPE_WAVE -> WaveStylePreview(
                        modifier = Modifier.fillMaxSize()
                    )
                    AnimationStyles.TYPE_CAPSULE -> CapsuleStylePreview(
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
                    modifier = Modifier.padding(top = 2.dp),
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
                        .size(36.dp)
                        .clipToBorder(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .onSingleClick { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
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
        val iconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thickness = 26.dp.toPx()
            val strokeWidth = 1.5.dp.toPx()
            val capsuleWidth = 48.dp.toPx()
            val cornerRadius = 13.dp.toPx()
            val startX = -capsuleWidth * 0.20f
            val top = size.height / 2f - thickness / 2f
            val center = Offset(startX + capsuleWidth / 2f, top + thickness / 2f)

            drawRoundRect(
                color = FixedCapsuleBackgroundColor,
                topLeft = Offset(startX, top),
                size = Size(capsuleWidth, thickness),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
            drawRoundRect(
                color = FixedCapsuleStrokeColor,
                topLeft = Offset(startX, top),
                size = Size(capsuleWidth, thickness),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeWidth)
            )

            val iconSize = 15.dp.toPx()
            rotate(0f, pivot = center) {
                translate(left = center.x - iconSize / 2f, top = center.y - iconSize / 2f) {
                    drawPreviewIcon(
                        painter = iconPainter,
                        iconSize = iconSize,
                        tint = FixedCapsuleIconColor
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
        val iconPainter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val width = size.width
            val height = size.height
            val top = height * 0.12f
            val bottom = height * 0.88f
            val centerY = height / 2f
            val waveX = width * 0.42f
            val curveHalfHeight = (bottom - top) / 2.2f
            val upperCurveY = centerY - curveHalfHeight
            val lowerCurveY = centerY + curveHalfHeight

            path.moveTo(0f, top)
            path.cubicTo(
                x1 = 0f,
                y1 = upperCurveY,
                x2 = waveX,
                y2 = upperCurveY,
                x3 = waveX,
                y3 = centerY
            )
            path.cubicTo(
                x1 = waveX,
                y1 = lowerCurveY,
                x2 = 0f,
                y2 = lowerCurveY,
                x3 = 0f,
                y3 = bottom
            )

            drawPath(path = path, color = FixedWaveBackgroundColor)
            drawPath(
                path = path,
                color = FixedWaveStrokeColor,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Image(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(18.dp),
            painter = iconPainter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(FixedWaveIconColor)
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
                .padding(4.dp)
                .clipToBorder(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.9f),
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

private val FixedWaveBackgroundColor = Color(0xFF171717)
private val FixedWaveStrokeColor = Color(0x2AFFFFFF)
private val FixedWaveIconColor = Color(0xCCFFFFFF)
private val FixedCapsuleBackgroundColor = Color(0xFF161616)
private val FixedCapsuleStrokeColor = Color(0x24FFFFFF)
private val FixedCapsuleIconColor = Color(0xE8FFFFFF)
