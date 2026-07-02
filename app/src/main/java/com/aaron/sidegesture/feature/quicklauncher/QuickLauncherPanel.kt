package com.aaron.sidegesture.feature.quicklauncher

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.getIcon
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.utils.VibrateUtils
import kotlin.math.roundToInt

private const val GRID_COLUMNS = 4
private val ITEM_ICON_SIZE = 44.dp
private val PANEL_CORNER_RADIUS = 20.dp
private val PANEL_PADDING = 12.dp
private val PANEL_WIDTH = 260.dp
private val PANEL_MIN_HEIGHT = 200.dp
private val PANEL_MAX_HEIGHT = 360.dp
private val EDGE_PADDING = 16.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun QuickLauncherPanel(
    state: QuickLauncherPanelState,
    onLaunch: (Action, Boolean) -> Unit,
    onOverlayTouchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                .background(Color.Black.copy(alpha = if (isDarkTheme) 0.52f else 0.28f))
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
            val estimatedPanelHeight = with(density) {
                val rows = ((state.items.size + GRID_COLUMNS - 1) / GRID_COLUMNS).coerceAtLeast(1)
                val rowHeight = ITEM_ICON_SIZE.toPx() + 8.dp.toPx() + 14.sp.toPx() + 4.dp.toPx()
                val spacing = 8.dp.toPx()
                val content = rows * rowHeight + (rows - 1) * spacing + PANEL_PADDING.toPx() * 2
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
                computeQuickLauncherOffset(
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    contentPadding = PaddingValues(PANEL_PADDING),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.items,
                        key = { it.data }
                    ) { action ->
                        QuickLauncherItem(
                            action = action,
                            onClick = {
                                val appInfo = action.appInfo
                                val shortcutInfo = action.shortcutInfo
                                val hasMiniWindow = appInfo?.miniWindow
                                    ?: shortcutInfo?.miniWindow ?: false
                                onLaunch(action, hasMiniWindow)
                                state.hide()
                            },
                            onLongClick = {
                                VibrateUtils.vibrate(context)
                                val appInfo = action.appInfo
                                val shortcutInfo = action.shortcutInfo
                                val hasMiniWindow = appInfo?.miniWindow
                                    ?: shortcutInfo?.miniWindow ?: false
                                onLaunch(action, !hasMiniWindow)
                                state.hide()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLauncherItem(
    action: Action,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val appInfo = action.appInfo
    val shortcutInfo = action.shortcutInfo
    val hasMiniWindow = appInfo?.miniWindow ?: shortcutInfo?.miniWindow ?: false
    val label = context.actionText(action)

    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = when {
                    appInfo != null -> appInfo.getIcon(context)
                    shortcutInfo != null -> shortcutInfo.getIcon(context)
                    else -> null
                },
                contentDescription = label,
                imageLoader = context.imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(ITEM_ICON_SIZE)
            )
            if (hasMiniWindow) {
                Icon(
                    imageVector = Icons.Default.BrandingWatermark,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

private fun computeQuickLauncherOffset(
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
