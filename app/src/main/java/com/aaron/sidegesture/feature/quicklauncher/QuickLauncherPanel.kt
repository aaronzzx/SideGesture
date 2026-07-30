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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.QuickLauncherSettings
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.getIcon
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.appColors
import com.aaron.sidegesture.ui.theme.componentShapes
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.elevations
import com.aaron.sidegesture.utils.VibrateUtils
import kotlin.math.roundToInt

private const val MAX_PAGE_INDICATOR_DOTS = 7

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun QuickLauncherPanel(
    state: QuickLauncherPanelState,
    settings: QuickLauncherSettings,
    onLaunch: (Action, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val dimensions = MaterialTheme.dimensions.quickLauncher
    val alpha = MaterialTheme.alpha
    val appColors = MaterialTheme.appColors
    val isDarkTheme = colorScheme.surface.luminance() < 0.5f
    val currentSettings = remember(settings) { settings.normalized() }

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
                    appColors.fixedBlack.copy(
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
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val availableWidth = containerWidthPx - safeLeft - safeRight
            val availableHeight = containerHeightPx - safeTop - safeBottom
            val sessionSettings = remember(state.sessionId) { currentSettings }
            val requestedIconSizePx = with(density) {
                sessionSettings.iconSizeDp.dp.toPx()
            }
            val itemHorizontalPaddingPx = with(density) {
                dimensions.itemHorizontalPadding.toPx()
            }
            val horizontalSpacingPx = with(density) {
                dimensions.gridHorizontalSpacing.toPx()
            }
            val panelPaddingPx = with(density) {
                dimensions.panelPadding.toPx()
            }
            val horizontalLayout = remember(
                state.sessionId,
                sessionSettings.columns,
                requestedIconSizePx,
                availableWidth,
                itemHorizontalPaddingPx,
                horizontalSpacingPx,
                panelPaddingPx
            ) {
                calculateQuickLauncherHorizontalLayout(
                    columns = sessionSettings.columns,
                    requestedIconSize = requestedIconSizePx,
                    availableWidth = availableWidth,
                    itemHorizontalPadding = itemHorizontalPaddingPx,
                    itemSpacing = horizontalSpacingPx,
                    contentPadding = panelPaddingPx
                )
            }
            val panelWidthPx = horizontalLayout.panelWidth
            val textSize = sessionSettings.textSizeSp.sp
            val labelLineHeight = (sessionSettings.textSizeSp + 3).sp
            val itemVerticalPaddingPx = with(density) {
                dimensions.itemVerticalPadding.toPx()
            }
            val itemLabelTopPaddingPx = with(density) {
                dimensions.itemLabelTopPadding.toPx()
            }
            val itemHeightPx = with(density) {
                horizontalLayout.iconSize +
                    itemVerticalPaddingPx * 2f +
                    itemLabelTopPaddingPx +
                    labelLineHeight.toPx()
            }
            val verticalSpacingPx = with(density) {
                dimensions.gridVerticalSpacing.toPx()
            }
            val indicatorHeightPx = with(density) {
                dimensions.pageIndicatorSize.toPx()
            }
            val indicatorSpacingPx = with(density) {
                dimensions.pageIndicatorSpacing.toPx()
            }
            val pageLayout = remember(
                state.sessionId,
                state.items.size,
                availableHeight,
                itemHeightPx,
                verticalSpacingPx,
                panelPaddingPx,
                indicatorHeightPx,
                indicatorSpacingPx,
                sessionSettings.columns,
                sessionSettings.rows
            ) {
                calculateQuickLauncherPageLayout(
                    itemCount = state.items.size,
                    availableHeight = availableHeight,
                    maxPanelHeight = availableHeight,
                    itemHeight = itemHeightPx,
                    rowSpacing = verticalSpacingPx,
                    contentPadding = panelPaddingPx,
                    indicatorHeight = indicatorHeightPx,
                    indicatorSpacing = indicatorSpacingPx,
                    columns = sessionSettings.columns,
                    maxRows = sessionSettings.rows
                )
            }
            val pages = remember(state.sessionId, pageLayout.itemsPerPage) {
                buildQuickLauncherPages(state.items, pageLayout.itemsPerPage)
            }
            val pagerState = remember(state.sessionId, pageLayout.itemsPerPage) {
                PagerState(currentPage = 0) { pages.size }
            }
            val itemHeight = with(density) { itemHeightPx.toDp() }
            val iconSize = with(density) { horizontalLayout.iconSize.toDp() }
            val panelWidth = with(density) { panelWidthPx.toDp() }
            val panelHeight = with(density) { pageLayout.panelHeight.toDp() }

            val panelOffset = remember(
                containerWidthPx, containerHeightPx,
                state.fingerAnchor, state.triggerEdge,
                safeLeft, safeTop, safeRight, safeBottom,
                panelWidthPx, pageLayout.panelHeight
            ) {
                computeQuickLauncherOffset(
                    containerWidth = containerWidthPx,
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
                    .width(panelWidth)
                    .height(panelHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                shape = MaterialTheme.componentShapes.quickLauncherPanel,
                color = colorScheme.surface,
                tonalElevation = MaterialTheme.elevations.overlayTonal,
                shadowElevation = if (isDarkTheme) {
                    MaterialTheme.elevations.overlayShadowDark
                } else {
                    MaterialTheme.elevations.overlayShadowLight
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = dimensions.panelPadding)
                ) {
                    HorizontalPager(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = pagerState
                    ) { page ->
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Fixed(sessionSettings.columns),
                            contentPadding = PaddingValues(horizontal = dimensions.panelPadding),
                            horizontalArrangement = Arrangement.spacedBy(dimensions.gridHorizontalSpacing),
                            verticalArrangement = Arrangement.spacedBy(dimensions.gridVerticalSpacing),
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
                                    iconSize = iconSize,
                                    textSize = textSize,
                                    lineHeight = labelLineHeight,
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
                        Spacer(modifier = Modifier.height(dimensions.pageIndicatorSpacing))
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
    iconSize: Dp,
    textSize: TextUnit,
    lineHeight: TextUnit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val dimensions = MaterialTheme.dimensions.quickLauncher
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
            .padding(vertical = dimensions.itemVerticalPadding),
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
                modifier = Modifier.size(iconSize)
            )
            if (hasMiniWindow) {
                Icon(
                    imageVector = Icons.Default.BrandingWatermark,
                    contentDescription = null,
                    modifier = Modifier.size(dimensions.miniWindowBadgeSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = label,
            fontSize = textSize,
            lineHeight = lineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions.itemLabelTopPadding)
        )
    }
}

@Composable
private fun QuickLauncherPageIndicator(
    currentPage: Int,
    pageCount: Int
) {
    val dimensions = MaterialTheme.dimensions.quickLauncher
    val pageDescription = stringResource(
        R.string.quick_launcher_page_description,
        currentPage + 1,
        pageCount
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions.pageIndicatorSize)
            .clearAndSetSemantics {
                contentDescription = pageDescription
            },
        horizontalArrangement = Arrangement.spacedBy(
            space = dimensions.pageIndicatorSpacing,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visibleDotCount = pageCount.coerceAtMost(MAX_PAGE_INDICATOR_DOTS)
        val firstVisiblePage = (currentPage - visibleDotCount / 2).coerceIn(
            minimumValue = 0,
            maximumValue = (pageCount - visibleDotCount).coerceAtLeast(0)
        )
        repeat(visibleDotCount) { index ->
            val page = firstVisiblePage + index
            Box(
                modifier = Modifier
                    .size(
                        if (page == currentPage) {
                            dimensions.pageIndicatorSize
                        } else {
                            dimensions.inactivePageIndicatorSize
                        }
                    )
                    .clip(CircleShape)
                    .background(
                        if (page == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = MaterialTheme.alpha.subtleBorder
                            )
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
