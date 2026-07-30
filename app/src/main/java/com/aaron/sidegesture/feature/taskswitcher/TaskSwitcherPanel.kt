package com.aaron.sidegesture.feature.taskswitcher

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.RecentTask
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.appColors
import com.aaron.sidegesture.ui.theme.componentShapes
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.elevations
import com.aaron.sidegesture.ui.theme.textStyles
import com.aaron.sidegesture.utils.VibrateUtils
import kotlin.math.roundToInt

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TaskSwitcherPanel(
    state: TaskSwitcherPanelState,
    lockedPackageNames: Set<String>,
    onLaunch: (RecentTask) -> Unit,
    onClose: (RecentTask) -> Unit,
    onToggleLock: (String) -> Unit,
    onCloseAll: (List<RecentTask>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = MaterialTheme.dimensions.taskSwitcher
    val alpha = MaterialTheme.alpha
    val isDarkTheme = colorScheme.surface.luminance() < 0.5f

    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = state.visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.appColors.fixedBlack.copy(
                        alpha = if (isDarkTheme) alpha.overlayScrimDark else alpha.overlayScrimLight
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    state.hide()
                }
        ) {
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val cutoutInsets = WindowInsets.displayCutout
            val systemBarInsets = WindowInsets.systemBars
            val safeLeft = with(density) {
                dimensions.edgePadding.toPx() + maxOf(
                    cutoutInsets.getLeft(density, layoutDirection),
                    systemBarInsets.getLeft(density, layoutDirection)
                )
            }
            val safeTop = with(density) {
                dimensions.edgePadding.toPx() + maxOf(
                    cutoutInsets.getTop(density),
                    systemBarInsets.getTop(density)
                )
            }
            val safeRight = with(density) {
                dimensions.edgePadding.toPx() + maxOf(
                    cutoutInsets.getRight(density, layoutDirection),
                    systemBarInsets.getRight(density, layoutDirection)
                )
            }
            val safeBottom = with(density) {
                dimensions.edgePadding.toPx() + maxOf(
                    cutoutInsets.getBottom(density),
                    systemBarInsets.getBottom(density)
                )
            }
            val panelWidthPx = with(density) { dimensions.panelWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val measuredPanelHeightPx = remember { mutableFloatStateOf(0f) }
            val availableHeight = containerHeightPx - safeTop - safeBottom
            val panelMaxHeightPx = with(density) {
                dimensions.panelMaxHeight.toPx().coerceAtMost(availableHeight)
            }
            val clampedMaxHeight = with(density) { panelMaxHeightPx.toDp() }
            val listMaxHeight = (clampedMaxHeight - dimensions.closeAllHeight)
                .coerceAtLeast(dimensions.rowHeight)
            val estimatedPanelHeight = with(density) {
                val rows = state.items.size.coerceAtLeast(1).coerceAtMost(6)
                val content = rows * dimensions.rowHeight.toPx() +
                    dimensions.closeAllHeight.toPx() +
                    dimensions.panelPadding.toPx() * 2
                content.coerceIn(dimensions.panelMinHeight.toPx(), panelMaxHeightPx)
            }
            val panelHeightForOffset = measuredPanelHeightPx.floatValue
                .takeIf { it > 0f }
                ?: estimatedPanelHeight

            val panelOffset = remember(
                maxWidth, maxHeight,
                state.fingerAnchor, state.triggerEdge,
                safeLeft, safeTop, safeRight, safeBottom,
                state.items.size, panelHeightForOffset
            ) {
                computeTaskSwitcherOffset(
                    containerWidth = with(density) { maxWidth.toPx() },
                    containerHeight = containerHeightPx,
                    panelWidth = panelWidthPx,
                    panelHeight = panelHeightForOffset,
                    fingerAnchor = state.fingerAnchor,
                    triggerEdge = state.triggerEdge,
                    safeLeft = safeLeft,
                    safeTop = safeTop,
                    safeRight = safeRight,
                    safeBottom = safeBottom
                )
            }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) }
                    .width(dimensions.panelWidth)
                    .heightIn(min = dimensions.panelMinHeight, max = clampedMaxHeight)
                    .onGloballyPositioned { coordinates ->
                        measuredPanelHeightPx.floatValue = coordinates.size.height.toFloat()
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                ) { },
                shape = MaterialTheme.componentShapes.taskSwitcherPanel,
                color = colorScheme.surface,
                tonalElevation = MaterialTheme.elevations.overlayTonal,
                shadowElevation = if (isDarkTheme) {
                    MaterialTheme.elevations.overlayShadowDark
                } else {
                    MaterialTheme.elevations.overlayShadowLight
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = listMaxHeight),
                        contentPadding = PaddingValues(
                            start = dimensions.panelPadding,
                            top = dimensions.panelPadding,
                            end = dimensions.panelPadding,
                            bottom = dimensions.sectionBottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(dimensions.itemSpacing)
                    ) {
                        items(
                            items = state.items,
                            key = { "${it.taskId}:${it.packageName}" }
                        ) { task ->
                            TaskSwitcherRow(
                                task = task,
                                locked = task.packageName in lockedPackageNames,
                                onLaunch = { onLaunch(task) },
                                onClose = { onClose(task) },
                                onToggleLock = { onToggleLock(task.packageName) }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.closeAllHeight)
                            .padding(
                                horizontal = dimensions.closeAllHorizontalPadding,
                                vertical = dimensions.itemVerticalPadding
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            enabled = state.items.any { it.packageName !in lockedPackageNames },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colorScheme.onSurface,
                                disabledContentColor = colorScheme.onSurface.copy(
                                    alpha = MaterialTheme.alpha.disabledContent
                                )
                            ),
                            onClick = {
                                val closeTargets = state.items.filter { it.packageName !in lockedPackageNames }
                                onCloseAll(closeTargets)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.task_switcher_close_all),
                                style = MaterialTheme.textStyles.taskSwitcherLabel,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskSwitcherRow(
    task: RecentTask,
    locked: Boolean,
    onLaunch: () -> Unit,
    onClose: () -> Unit,
    onToggleLock: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = MaterialTheme.dimensions.taskSwitcher
    val context = LocalContext.current
    val icon = remember(task.packageName, context) {
        runCatching {
            context.packageManager.getApplicationIcon(task.packageName)
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions.rowHeight)
            .clip(MaterialTheme.componentShapes.taskSwitcherItem)
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = {
                    VibrateUtils.vibrate(context)
                    onToggleLock()
                }
            )
            .padding(
                start = dimensions.itemStartPadding,
                end = dimensions.itemEndPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = icon,
            contentDescription = task.label,
            imageLoader = context.imageLoader,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(dimensions.itemIconSize)
        )
        Text(
            text = task.label,
            color = colorScheme.onSurface,
            style = MaterialTheme.textStyles.taskSwitcherLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensions.panelPadding)
        )
        Box(
            modifier = Modifier
                .size(dimensions.actionButtonSize)
                .clip(CircleShape)
                .clickable(
                    enabled = !locked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onClose()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (locked) Icons.Default.Lock else Icons.Default.Close,
                contentDescription = null,
                tint = if (locked) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                modifier = Modifier.size(dimensions.closeAllIconSize)
            )
        }
    }
}

private fun computeTaskSwitcherOffset(
    containerWidth: Float,
    containerHeight: Float,
    panelWidth: Float,
    panelHeight: Float,
    fingerAnchor: Offset,
    triggerEdge: Position,
    safeLeft: Float,
    safeTop: Float,
    safeRight: Float,
    safeBottom: Float
): Offset {
    if (containerWidth <= 0f || containerHeight <= 0f) return Offset.Zero
    val anchor = if (fingerAnchor != Offset.Unspecified) {
        fingerAnchor
    } else {
        Offset(containerWidth / 2f, containerHeight / 2f)
    }
    val leftX = safeLeft
    val rightX = containerWidth - panelWidth - safeRight
    val minY = safeTop
    val maxY = (containerHeight - panelHeight - safeBottom).coerceAtLeast(minY)

    val x = when (triggerEdge) {
        Position.Left -> leftX
        Position.Right -> rightX
        Position.Bottom, Position.Top -> {
            if (anchor.x <= containerWidth / 2f) leftX else rightX
        }
    }
    val y = when (triggerEdge) {
        Position.Bottom -> maxY
        Position.Top -> minY
        else -> (anchor.y - panelHeight / 2f).coerceIn(minY, maxY)
    }
    return Offset(x, y)
}
