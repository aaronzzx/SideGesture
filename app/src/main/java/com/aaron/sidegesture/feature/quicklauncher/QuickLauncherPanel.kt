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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.sidegesture.R
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
private val ITEM_VERTICAL_PADDING = 4.dp
private val ITEM_LABEL_TOP_PADDING = 4.dp
private val ITEM_LABEL_LINE_HEIGHT = 14.sp
private val GRID_HORIZONTAL_SPACING = 4.dp
private val GRID_VERTICAL_SPACING = 8.dp
private val PANEL_CORNER_RADIUS = 20.dp
private val PANEL_PADDING = 12.dp
private val PANEL_WIDTH = 260.dp
private val PANEL_MIN_HEIGHT = 200.dp
private val PANEL_MAX_HEIGHT = 360.dp
private val EDGE_PADDING = 16.dp
private val PAGE_INDICATOR_SIZE = 8.dp
private val PAGE_INDICATOR_SPACING = 8.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun QuickLauncherPanel(
    state: QuickLauncherPanelState,
    onLaunch: (Action, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
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
            val availableHeight = containerHeightPx - safeTop - safeBottom
            val itemHeightPx = with(density) {
                ITEM_ICON_SIZE.toPx() +
                    ITEM_VERTICAL_PADDING.toPx() * 2f +
                    ITEM_LABEL_TOP_PADDING.toPx() +
                    ITEM_LABEL_LINE_HEIGHT.toPx()
            }
            val pageLayout = remember(state.sessionId) {
                calculateQuickLauncherPageLayout(
                    itemCount = state.items.size,
                    availableHeight = availableHeight,
                    minPanelHeight = with(density) { PANEL_MIN_HEIGHT.toPx() },
                    maxPanelHeight = with(density) { PANEL_MAX_HEIGHT.toPx() },
                    itemHeight = itemHeightPx,
                    rowSpacing = with(density) { GRID_VERTICAL_SPACING.toPx() },
                    contentPadding = with(density) { PANEL_PADDING.toPx() },
                    indicatorHeight = with(density) { PAGE_INDICATOR_SIZE.toPx() },
                    indicatorSpacing = with(density) { PAGE_INDICATOR_SPACING.toPx() },
                    columns = GRID_COLUMNS
                )
            }
            val pages = remember(state.sessionId, pageLayout.itemsPerPage) {
                buildQuickLauncherPages(state.items, pageLayout.itemsPerPage)
            }
            val pagerState = remember(state.sessionId) {
                PagerState(currentPage = 0) { pages.size }
            }
            val itemHeight = with(density) { itemHeightPx.toDp() }
            val panelHeight = with(density) { pageLayout.panelHeight.toDp() }

            val panelOffset = remember(
                maxWidth, maxHeight,
                state.fingerAnchor, state.triggerEdge,
                safeLeft, safeTop, safeRight, safeBottom,
                pageLayout.panelHeight
            ) {
                computeQuickLauncherOffset(
                    containerWidth = with(density) { maxWidth.toPx() },
                    containerHeight = containerHeightPx,
                    panelWidth = panelWidthPx,
                    panelHeight = pageLayout.panelHeight,
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
                    .height(panelHeight)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(PANEL_PADDING)
                ) {
                    HorizontalPager(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = pagerState
                    ) { page ->
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Fixed(GRID_COLUMNS),
                            horizontalArrangement = Arrangement.spacedBy(GRID_HORIZONTAL_SPACING),
                            verticalArrangement = Arrangement.spacedBy(GRID_VERTICAL_SPACING),
                            userScrollEnabled = false
                        ) {
                            itemsIndexed(
                                items = pages[page],
                                key = { index, action ->
                                    "${action.value}:${action.data}:$index"
                                }
                            ) { _, action ->
                                QuickLauncherItem(
                                    action = action,
                                    itemHeight = itemHeight,
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
                    if (pageLayout.showPageIndicator) {
                        Spacer(modifier = Modifier.height(PAGE_INDICATOR_SPACING))
                        QuickLauncherPageIndicator(
                            currentPage = pagerState.currentPage,
                            pageCount = pages.size
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
    itemHeight: Dp,
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
            .height(itemHeight)
            .pointerInput(action) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(vertical = ITEM_VERTICAL_PADDING),
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
            lineHeight = ITEM_LABEL_LINE_HEIGHT,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ITEM_LABEL_TOP_PADDING)
        )
    }
}

@Composable
private fun QuickLauncherPageIndicator(
    currentPage: Int,
    pageCount: Int
) {
    val pageDescription = stringResource(
        R.string.quick_launcher_page_description,
        currentPage + 1,
        pageCount
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PAGE_INDICATOR_SIZE)
            .clearAndSetSemantics {
                contentDescription = pageDescription
            },
        horizontalArrangement = Arrangement.spacedBy(
            space = PAGE_INDICATOR_SPACING,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            Box(
                modifier = Modifier
                    .size(if (page == currentPage) PAGE_INDICATOR_SIZE else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (page == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        }
                    )
            )
        }
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
