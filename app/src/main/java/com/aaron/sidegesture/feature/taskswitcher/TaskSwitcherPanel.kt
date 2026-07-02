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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.RecentTask
import com.aaron.sidegesture.utils.VibrateUtils
import kotlin.math.roundToInt

private val ITEM_ICON_SIZE = 36.dp
private val ROW_HEIGHT = 48.dp
private val ACTION_BUTTON_SIZE = 36.dp
private val PANEL_CORNER_RADIUS = 28.dp
private val PANEL_PADDING = 12.dp
private val PANEL_WIDTH = 240.dp
private val PANEL_MIN_HEIGHT = 120.dp
private val PANEL_MAX_HEIGHT = 300.dp
private val CLOSE_ALL_HEIGHT = 52.dp
private val EDGE_PADDING = 16.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TaskSwitcherPanel(
    state: TaskSwitcherPanelState,
    lockedPackageNames: Set<String>,
    onLaunch: (RecentTask) -> Unit,
    onClose: (RecentTask) -> Unit,
    onToggleLock: (String) -> Unit,
    onCloseAll: (List<RecentTask>) -> Unit,
    onOverlayTouchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.surface.luminance() < 0.5f

    LaunchedEffect(state.visible) {
        onOverlayTouchChange(state.visible)
    }

    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = state.visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color.Black.copy(alpha = 0.52f) else Color.Black.copy(alpha = 0.28f))
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
            val safeLeft = with(density) { EDGE_PADDING.toPx() + cutoutInsets.getLeft(density, layoutDirection) }
            val safeTop = with(density) { EDGE_PADDING.toPx() + cutoutInsets.getTop(density) }
            val safeRight = with(density) { EDGE_PADDING.toPx() + cutoutInsets.getRight(density, layoutDirection) }
            val safeBottom = with(density) { EDGE_PADDING.toPx() + cutoutInsets.getBottom(density) }
            val panelWidthPx = with(density) { PANEL_WIDTH.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val measuredPanelHeightPx = remember { mutableFloatStateOf(0f) }
            val availableHeight = containerHeightPx - safeTop - safeBottom
            val panelMaxHeightPx = with(density) {
                PANEL_MAX_HEIGHT.toPx().coerceAtMost(availableHeight)
            }
            val clampedMaxHeight = with(density) { panelMaxHeightPx.toDp() }
            val listMaxHeight = (clampedMaxHeight - CLOSE_ALL_HEIGHT).coerceAtLeast(ROW_HEIGHT)
            val estimatedPanelHeight = with(density) {
                val rows = state.items.size.coerceAtLeast(1).coerceAtMost(6)
                val content = rows * ROW_HEIGHT.toPx() + CLOSE_ALL_HEIGHT.toPx() + PANEL_PADDING.toPx() * 2
                content.coerceIn(PANEL_MIN_HEIGHT.toPx(), panelMaxHeightPx)
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
                    .width(PANEL_WIDTH)
                    .heightIn(min = PANEL_MIN_HEIGHT, max = clampedMaxHeight)
                    .onGloballyPositioned { coordinates ->
                        measuredPanelHeightPx.floatValue = coordinates.size.height.toFloat()
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                ) { },
                shape = RoundedCornerShape(PANEL_CORNER_RADIUS),
                color = colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = if (isDarkTheme) 8.dp else 12.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = listMaxHeight),
                        contentPadding = PaddingValues(
                            start = PANEL_PADDING,
                            top = PANEL_PADDING,
                            end = PANEL_PADDING,
                            bottom = 4.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            .height(CLOSE_ALL_HEIGHT)
                            .padding(horizontal = PANEL_PADDING, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            enabled = state.items.any { it.packageName !in lockedPackageNames },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = colorScheme.onSurface,
                                disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            onClick = {
                                val closeTargets = state.items.filter { it.packageName !in lockedPackageNames }
                                onCloseAll(closeTargets)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.task_switcher_close_all),
                                fontSize = 15.sp,
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
    val context = LocalContext.current
    val icon = remember(task.packageName, context) {
        runCatching {
            context.packageManager.getApplicationIcon(task.packageName)
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = {
                    VibrateUtils.vibrate(context)
                    onToggleLock()
                }
            )
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = icon,
            contentDescription = task.label,
            imageLoader = context.imageLoader,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(ITEM_ICON_SIZE)
        )
        Text(
            text = task.label,
            color = colorScheme.onSurface,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Box(
            modifier = Modifier
                .size(ACTION_BUTTON_SIZE)
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
                modifier = Modifier.size(20.dp)
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
        Position.Bottom -> {
            if (anchor.x <= containerWidth / 2f) leftX else rightX
        }
    }
    val y = when (triggerEdge) {
        Position.Bottom -> maxY
        else -> (anchor.y - panelHeight / 2f).coerceIn(minY, maxY)
    }
    return Offset(x, y)
}
