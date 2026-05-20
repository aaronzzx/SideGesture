package com.aaron.sidegesture.ui.screen.animationstyle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBorder
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ktx.getIcon
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
                        waveStyle = uiState.waveStyle,
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
    waveStyle: WaveStyle,
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
                        modifier = Modifier.fillMaxSize(),
                        style = waveStyle
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
private fun WaveStylePreview(
    style: WaveStyle,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {}

        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val width = size.width
            val height = size.height
            val top = height * 0.04f
            val bottom = height * 0.96f
            val centerY = height / 2f
            val waveX = width * 0.48f
            val curveHalfHeight = (bottom - top) / 2.6f
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

            drawPath(path = path, color = Color(style.backgroundColor))
            if (style.strokeWidth > 0) {
                drawPath(
                    path = path,
                    color = Color(style.strokeColor),
                    style = Stroke(width = style.strokeWidth.coerceAtLeast(1).toFloat())
                )
            }
        }

        Image(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(20.dp),
            painter = style.getIcon(),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color(style.iconColor))
        )
    }
}
