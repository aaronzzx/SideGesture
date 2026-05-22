package com.aaron.sidegesture.ui.screen.actionpanelstyle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBorder
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.ui.screen.actionpanelstyle.ActionPanelStyleSelectVM.ActionPanelStyleItem
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.TopBar
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author OpenAI
 * @since 2026/5/22
 */
@Composable
fun ActionPanelStyleSelectScreen(
    onBack: () -> Unit,
    onNavToStyleConfig: (Int) -> Unit,
    vm: ActionPanelStyleSelectVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.action_panel_style)
            )
            MyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.items.forEach { item ->
                    ActionPanelStyleCard(
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
private fun ActionPanelStyleCard(
    item: ActionPanelStyleItem,
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
            Box(modifier = Modifier.size(80.dp)) {
                when (item.type) {
                    ActionPanelStyles.TYPE_FOLDER -> FolderStylePreview(modifier = Modifier.fillMaxSize())
                    else -> SectorStylePreview(modifier = Modifier.fillMaxSize())
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
private fun FolderStylePreview(
    modifier: Modifier = Modifier
) {
    PreviewStage(modifier = modifier) {
        val colorScheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 58.dp, height = 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorScheme.primary.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorStylePreview(
    modifier: Modifier = Modifier
) {
    PreviewStage(modifier = modifier) {
        val colorScheme = MaterialTheme.colorScheme
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = colorScheme.primary
            val anchor = Offset(
                x = -size.width * 0.08f,
                y = size.height * 0.5f
            )
            val itemRadius = 4.dp.toPx()
            val innerArcRadius = size.minDimension * 0.37f
            val outerArcRadius = size.minDimension * 0.56f

            fun pointOnArc(radius: Float, angleDegree: Float): Offset {
                val radian = Math.toRadians(angleDegree.toDouble())
                return Offset(
                    x = anchor.x + cos(radian).toFloat() * radius,
                    y = anchor.y + sin(radian).toFloat() * radius
                )
            }

            fun drawItem(radius: Float, angleDegree: Float) {
                drawCircle(
                    color = color,
                    radius = itemRadius,
                    center = pointOnArc(radius, angleDegree)
                )
            }

            listOf(-30f, 0f, 30f).forEach { angle ->
                drawItem(innerArcRadius, angle)
            }

            listOf(-30f, -10f, 10f, 30f).forEach { angle ->
                drawItem(outerArcRadius, angle)
            }
        }
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
