package com.aaron.sidegesture.feature.actionpanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toDp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.AdvancedSettingsDefaults
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelAppSwitchWindowModeDelayMs
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelAppSwitchWindowModeDelayMs
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.FolderStyle
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.SectorStyle
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.feature.gesture.LongSlideState
import com.aaron.sidegesture.ktx.actionIcon
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.alipayColor
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ktx.toIntOffset
import com.aaron.sidegesture.ktx.tryVibrateForActionPanel
import com.aaron.sidegesture.utils.JsonHelper
import com.aaron.sidegesture.ktx.wechatColor
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.appColors
import com.aaron.sidegesture.ui.theme.componentShapes
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.elevations
import com.aaron.sidegesture.ui.theme.motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

@Composable
fun ActionPanel(
    actionPanelStyle: ActionPanelStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    longPressLaunchPopup: Boolean = false,
    vibrations: Vibrations? = null
) {
    val dimensions = MaterialTheme.dimensions
    val appColors = MaterialTheme.appColors
    val motion = MaterialTheme.motion
    AnimatedVisibility(
        modifier = modifier,
        visible = actionPanelState.visible,
        enter = fadeIn(spring(stiffness = motion.overlayVisibilityStiffness)),
        exit = fadeOut(spring(stiffness = motion.overlayVisibilityStiffness))
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = appColors.fixedBlack.copy(alpha = MaterialTheme.alpha.lowEmphasis)
                    )
            )

            val selectedAction = actionPanelState.selectedAction
            val selectedLabel = actionText(selectedAction)
            val animationSpec = spring<Float>(stiffness = motion.actionPanelPlacementStiffness)
            val enter = fadeIn(animationSpec) +
                scaleIn(animationSpec, MaterialTheme.motion.actionPanelSecondaryEnterScale)
            val exit = fadeOut(animationSpec) +
                scaleOut(animationSpec, MaterialTheme.motion.actionPanelSecondaryEnterScale)


            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .displayCutoutPadding()
                    .padding(dimensions.layout.overlayContentPadding),
                visible = selectedAction.value == GlobalActions.EXTRA_LAUNCH_APP ||
                        selectedAction.value == GlobalActions.EXTRA_LAUNCH_SHORTCUT,
                enter = enter,
                exit = ExitTransition.None
            ) {
                BoxWithConstraints {
                    Box(
                        modifier = Modifier
                            .let { thisModifier ->
                                val miniWindow = actionPanelState.isMiniWindow(longPressLaunchPopup)
                                val maxWidth = this@BoxWithConstraints.maxWidth
                                val maxHeight = this@BoxWithConstraints.maxHeight
                                val spec = spring<Dp>(stiffness = 5000f)
                                val width by animateDpAsState(
                                    targetValue = when (miniWindow) {
                                        true -> dimensions.actionPanel.miniWindowSize
                                        false -> maxWidth
                                    },
                                    animationSpec = spec
                                )
                                val height by animateDpAsState(
                                    targetValue = when (miniWindow) {
                                        true -> width / 0.75f
                                        false -> maxHeight
                                    },
                                    animationSpec = spec
                                )
                                thisModifier.size(width = width, height = height)
                            }
                            .background(
                                color = appColors.fixedWhite.copy(
                                    alpha = MaterialTheme.alpha.subtleBorder
                                ),
                                shape = MaterialTheme.componentShapes.actionPanelMiniWindow
                            )
                    )
                }
            }

            when (actionPanelStyle) {
                is SectorStyle -> {
                    SectorActionPanel(
                        modifier = Modifier.fillMaxSize(),
                        actionPanelStyle = actionPanelStyle,
                        actionPanelState = actionPanelState,
                        vibrations = vibrations
                    )
                }

                is ArcStyle -> {
                    SectorActionPanel(
                        modifier = Modifier.fillMaxSize(),
                        actionPanelStyle = SectorStyle(itemSize = actionPanelStyle.itemSize),
                        actionPanelState = actionPanelState,
                        vibrations = vibrations
                    )
                }

                is FolderStyle -> {
                    FolderActionPanel(
                        modifier = Modifier.fillMaxSize(),
                        actionPanelStyle = actionPanelStyle,
                        actionPanelState = actionPanelState,
                        vibrations = vibrations
                    )
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .displayCutoutPadding()
                    .padding(dimensions.layout.overlayContentPadding),
                visible = selectedLabel.isNotEmpty(),
                enter = enter,
                exit = exit
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(
                            color = appColors.fixedBlack,
                            offset = Offset(
                                dimensions.actionPanel.textShadowOffset.value,
                                dimensions.actionPanel.textShadowOffset.value
                            ),
                            blurRadius = dimensions.actionPanel.textShadowBlurRadius.value
                        )
                    ),
                    color = appColors.fixedWhite
                )
            }
        }
    }
}

private const val FolderAutoScrollFrameDelayMs = 16L

@Composable
private fun AnimatedVisibilityScope.FolderActionPanel(
    actionPanelStyle: FolderStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    vibrations: Vibrations? = null
) {
    val density = LocalDensity.current
    val dimensions = MaterialTheme.dimensions.actionPanel
    val motion = MaterialTheme.motion
    val itemSize = actionPanelStyle.itemSize.toDp()
    val itemSpacing = actionPanelStyle.itemSpacing.toDp()
    val horizontalPadding = actionPanelStyle.horizontalPadding.toDp()
    val verticalPadding = actionPanelStyle.verticalPadding.toDp()
    val cornerRadius = actionPanelStyle.cornerRadius.toDp()
    val itemSizePx = itemSize.toPx()
    val itemSpacingPx = itemSpacing.toPx()
    val horizontalPaddingPx = horizontalPadding.toPx()
    val verticalPaddingPx = verticalPadding.toPx()
    val edgePaddingPx = dimensions.edgePadding.toPx()
    val cornerSafePaddingPx = dimensions.cornerSafePadding.toPx()
    val cutoutInsets = WindowInsets.displayCutout
    val systemBarInsets = WindowInsets.systemBars
    val topSafeInsetPx = maxOf(
        cutoutInsets.getTop(density),
        systemBarInsets.getTop(density)
    ).toFloat()
    val scrollHotZonePx = actionPanelStyle.scrollHotZoneHeight.coerceAtLeast(1).toFloat()
    val autoScrollSpeedPxPerFrame = actionPanelStyle.scrollSpeed.coerceAtLeast(1).toFloat()
    val columns = actionPanelStyle.columns.coerceAtLeast(1)
    val rowCount = actionPanelStyle.rows.coerceAtLeast(1)
    val totalRows = ceil(actionPanelState.actions.size / columns.toFloat()).roundToInt()
    val visibleRows = totalRows.coerceAtMost(rowCount).coerceAtLeast(1)
    val gridWidthPx = columns * itemSizePx + (columns - 1) * itemSpacingPx
    val gridHeightPx = visibleRows * itemSizePx + (visibleRows - 1) * itemSpacingPx
    val panelWidthPx = gridWidthPx + horizontalPaddingPx * 2f
    val panelHeightPx = gridHeightPx + verticalPaddingPx * 2f
    val panelWidth = with(density) { panelWidthPx.toDp() }
    val panelHeight = with(density) { panelHeightPx.toDp() }
    val gridHeight = with(density) { gridHeightPx.toDp() }
    val gridState = rememberLazyGridState()
    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var parentSize by remember { mutableStateOf(Size.Zero) }
    var stableOrigin by remember { mutableStateOf(Offset.Unspecified) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var autoScrollDirection by remember { mutableStateOf(0) }

    if (actionPanelState.origin.isSpecified) {
        stableOrigin = actionPanelState.origin
    }

    LaunchedEffect(actionPanelState.actions.size) {
        itemBounds.clear()
        selectedIndex = null
        gridState.scrollToItem(0)
        autoScrollDirection = 0
    }

    LaunchedEffect(autoScrollDirection, gridState, autoScrollSpeedPxPerFrame) {
        val direction = autoScrollDirection
        while (direction != 0 && gridState.canScroll(direction)) {
            gridState.scrollBy(direction * autoScrollSpeedPxPerFrame)
            delay(FolderAutoScrollFrameDelayMs)
        }
    }

    LaunchedEffect(gridState, itemBounds) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.index } }
            .collect { visibleIndexes ->
                val visibleIndexSet = visibleIndexes.toSet()
                itemBounds.keys
                    .filter { it !in visibleIndexSet }
                    .fastForEach { itemBounds.remove(it) }
            }
    }

    LaunchedEffect(
        transition,
        actionPanelState,
        gridState,
        itemBounds,
        panelBounds,
        scrollHotZonePx
    ) {
        snapshotFlow { actionPanelState.finger }
            .filter {
                it.isSpecified &&
                        !transition.isRunning &&
                        transition.currentState == Visible
            }
            .collect { finger ->
                val inPanel = panelBounds.contains(finger)
                autoScrollDirection = when {
                    !inPanel -> 0
                    finger.y <= panelBounds.top + scrollHotZonePx &&
                            gridState.canScrollBackward -> -1
                    finger.y >= panelBounds.bottom - scrollHotZonePx &&
                            gridState.canScrollForward -> 1
                    else -> 0
                }

                if (!inPanel || autoScrollDirection != 0) {
                    selectedIndex?.let { index ->
                        actionPanelState.select(index, Action.NONE)
                        selectedIndex = null
                    }
                    return@collect
                }

                val hit = itemBounds.entries.firstOrNull { it.value.contains(finger) }
                if (hit == null) {
                    selectedIndex?.let { index ->
                        actionPanelState.select(index, Action.NONE)
                        selectedIndex = null
                    }
                    return@collect
                }

                val action = actionPanelState.actions.getOrNull(hit.key) ?: return@collect
                if (selectedIndex != hit.key || !actionPanelState.isSelected(action)) {
                    selectedIndex?.let { index ->
                        if (index != hit.key) {
                            actionPanelState.select(index, Action.NONE)
                        }
                    }
                    selectedIndex = hit.key
                    actionPanelState.select(hit.key, action)
                    vibrations?.tryVibrateForActionPanel()
                }
            }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    parentSize = it.size.toSize()
                }
                .matchParentSize()
        )

        val anchor = remember(
            parentSize,
            stableOrigin,
            actionPanelState.position,
            panelWidthPx,
            panelHeightPx,
            edgePaddingPx,
            cornerSafePaddingPx,
            topSafeInsetPx
        ) {
            folderPanelAnchor(
                parentSize = parentSize,
                origin = stableOrigin,
                position = actionPanelState.position,
                panelWidthPx = panelWidthPx,
                panelHeightPx = panelHeightPx,
                edgePaddingPx = edgePaddingPx,
                cornerSafePaddingPx = cornerSafePaddingPx,
                topSafeInsetPx = topSafeInsetPx
            )
        }

        val colorScheme = MaterialTheme.colorScheme
        val isDarkTheme = colorScheme.surface.luminance() < 0.5f
        Surface(
            modifier = Modifier
                .graphicsLayer {
                    if (parentSize.isEmpty()) return@graphicsLayer
                    translationX = anchor.x
                    translationY = anchor.y
                }
                .width(panelWidth)
                .height(panelHeight)
                .onGloballyPositioned {
                    panelBounds = it.boundsInRoot()
                }
                .animateEnterExit(
                    enter = fadeIn(spring(stiffness = motion.overlayVisibilityStiffness)) +
                            scaleIn(
                                spring(stiffness = motion.overlayVisibilityStiffness),
                                MaterialTheme.motion.actionPanelEnterScale
                            ),
                    exit = fadeOut(spring(stiffness = motion.overlayVisibilityStiffness)) +
                            scaleOut(
                                spring(stiffness = motion.overlayVisibilityStiffness),
                                MaterialTheme.motion.actionPanelEnterScale
                            )
                ),
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = MaterialTheme.elevations.overlayTonal,
            shadowElevation = if (isDarkTheme) {
                MaterialTheme.elevations.overlayShadowDark
            } else {
                MaterialTheme.elevations.overlayShadowLight
            }
        ) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(columns),
                state = gridState,
                contentPadding = PaddingValues(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                ),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                itemsIndexed(
                    items = actionPanelState.actions,
                    key = { index, _ -> index }
                ) { index, action ->
                    val scale by animateFloatAsState(
                        targetValue = if (actionPanelState.isSelected(action)) {
                            MaterialTheme.motion.actionPanelSelectionScale
                        } else {
                            1f
                        },
                        animationSpec = spring(stiffness = motion.actionPanelSelectionStiffness)
                    )
                    ActionPanelItem(
                        modifier = Modifier
                            .size(itemSize)
                            .onGloballyPositioned {
                                itemBounds[index] = it.boundsInRoot()
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        action = action
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridState.canScroll(direction: Int): Boolean {
    return when {
        direction < 0 -> canScrollBackward
        direction > 0 -> canScrollForward
        else -> false
    }
}

private fun folderPanelAnchor(
    parentSize: Size,
    origin: Offset,
    position: Position,
    panelWidthPx: Float,
    panelHeightPx: Float,
    edgePaddingPx: Float,
    cornerSafePaddingPx: Float,
    topSafeInsetPx: Float
): Offset {
    if (parentSize.isEmpty()) return Offset.Zero

    val safeOrigin = if (origin.isSpecified) {
        origin
    } else {
        Offset(parentSize.width / 2f, parentSize.height / 2f)
    }

    return when (position) {
        Position.Left -> Offset(
            x = edgePaddingPx,
            y = (safeOrigin.y - panelHeightPx / 2f).coerceInSafely(
                minimumValue = cornerSafePaddingPx,
                maximumValue = parentSize.height - cornerSafePaddingPx - panelHeightPx
            )
        )

        Position.Right -> Offset(
            x = parentSize.width - edgePaddingPx - panelWidthPx,
            y = (safeOrigin.y - panelHeightPx / 2f).coerceInSafely(
                minimumValue = cornerSafePaddingPx,
                maximumValue = parentSize.height - cornerSafePaddingPx - panelHeightPx
            )
        )

        Position.Bottom -> Offset(
            x = (safeOrigin.x - panelWidthPx / 2f).coerceInSafely(
                minimumValue = cornerSafePaddingPx,
                maximumValue = parentSize.width - cornerSafePaddingPx - panelWidthPx
            ),
            y = parentSize.height - edgePaddingPx - panelHeightPx
        )

        Position.Top -> Offset(
            x = (safeOrigin.x - panelWidthPx / 2f).coerceInSafely(
                minimumValue = cornerSafePaddingPx,
                maximumValue = parentSize.width - cornerSafePaddingPx - panelWidthPx
            ),
            y = topSafeInsetPx + edgePaddingPx
        )
    }
}

private const val SectorAngleDegree = 180.0
private const val SectorRadiusStepRatio = 1.25f
@Composable
private fun AnimatedVisibilityScope.SectorActionPanel(
    actionPanelStyle: SectorStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    vibrations: Vibrations? = null
) {
    val density = LocalDensity.current
    val dimensions = MaterialTheme.dimensions.actionPanel
    val motion = MaterialTheme.motion
    val defaultItemSize = actionPanelStyle.itemSize.toDp()
    val defaultItemSizePx = defaultItemSize.toPx()
    val initialRadiusRatio = actionPanelStyle.initialRadiusRatio.coerceAtLeast(0.1f)
    val itemSpacingRatio = actionPanelStyle.itemSpacingRatio.coerceAtLeast(0.1f)
    val edgePaddingPx = dimensions.edgePadding.toPx()
    val cornerSafePaddingPx = dimensions.cornerSafePadding.toPx()
    val cutoutInsets = WindowInsets.displayCutout
    val systemBarInsets = WindowInsets.systemBars
    val topSafeInsetPx = maxOf(
        cutoutInsets.getTop(density),
        systemBarInsets.getTop(density)
    ).toFloat()
    val minItemSizePx = dimensions.sectorMinItemSize.toPx()
    var parentSize by remember { mutableStateOf(Size.Zero) }
    var stableOrigin by remember { mutableStateOf(Offset.Unspecified) }

    if (actionPanelState.origin.isSpecified) {
        stableOrigin = actionPanelState.origin
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    parentSize = it.size.toSize()
                }
                .matchParentSize()
        )

        val itemSizePx = remember(
            parentSize,
            actionPanelState.actions.size,
            actionPanelState.position,
            defaultItemSizePx,
            initialRadiusRatio,
            itemSpacingRatio,
            cornerSafePaddingPx,
            minItemSizePx
        ) {
            sectorItemSizePx(
                itemCount = actionPanelState.actions.size,
                defaultItemSizePx = defaultItemSizePx,
                minItemSizePx = minItemSizePx,
                parentSize = parentSize,
                position = actionPanelState.position,
                cornerSafePaddingPx = cornerSafePaddingPx,
                initialRadiusRatio = initialRadiusRatio,
                itemSpacingRatio = itemSpacingRatio
            )
        }
        val itemSize = with(density) { itemSizePx.toDp() }
        val layouts = remember(
            actionPanelState.actions.size,
            itemSizePx,
            initialRadiusRatio,
            itemSpacingRatio
        ) {
            sectorLayerLayouts(
                itemCount = actionPanelState.actions.size,
                itemSizePx = itemSizePx,
                initialRadiusRatio = initialRadiusRatio,
                itemSpacingRatio = itemSpacingRatio
            )
        }
        val itemOffsets = remember(
            actionPanelState.position,
            layouts
        ) {
            sectorItemOffsets(
                layouts = layouts,
                position = actionPanelState.position
            )
        }
        val anchor = remember(
            parentSize,
            stableOrigin,
            actionPanelState.position,
            itemOffsets,
            itemSizePx,
            edgePaddingPx,
            cornerSafePaddingPx,
            topSafeInsetPx
        ) {
            sectorAnchor(
                parentSize = parentSize,
                origin = stableOrigin,
                position = actionPanelState.position,
                itemOffsets = itemOffsets,
                itemSizePx = itemSizePx,
                edgePaddingPx = edgePaddingPx,
                cornerSafePaddingPx = cornerSafePaddingPx,
                topSafeInsetPx = topSafeInsetPx
            )
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    if (parentSize.isEmpty()) return@graphicsLayer
                    val itemSizeHalf = itemSizePx / 2f
                    translationX = anchor.x - itemSizeHalf
                    translationY = anchor.y - itemSizeHalf
                }
                .size(itemSize)
        ) {
            val transition = transition
            actionPanelState.actions.fastForEachIndexed { index, action ->
                key(index) {
                    val targetAnimOffset = itemOffsets.getOrElse(index) { Offset.Zero }
                    val selectAnim = remember { Animatable(1f) }
                    val selectionScale = MaterialTheme.motion.actionPanelSelectionScale
                    var originBounds by remember { mutableStateOf(Rect.Zero) }

                    LaunchedEffect(transition, actionPanelState, index, action, targetAnimOffset) {
                        snapshotFlow { actionPanelState.finger }
                            .filter {
                                it.isSpecified &&
                                        !transition.isRunning &&
                                        transition.currentState == Visible
                            }
                            .collect { finger ->
                                val transFinger = finger - targetAnimOffset
                                if (originBounds.contains(transFinger)) {
                                    if (!actionPanelState.isSelected(action)) {
                                        launch { selectAnim.animateTo(selectionScale) }
                                        actionPanelState.select(index, action)
                                        vibrations?.tryVibrateForActionPanel()
                                    }
                                } else {
                                    if (actionPanelState.isSelected(action)) {
                                        launch { selectAnim.animateTo(1f) }
                                        actionPanelState.select(index, Action.NONE)
                                    }
                                }
                            }
                    }

                    ActionPanelItem(
                        modifier = Modifier
                            .onGloballyPositioned {
                                originBounds = it.boundsInRoot()
                            }
                            .graphicsLayer {
                                translationX = targetAnimOffset.x
                                translationY = targetAnimOffset.y
                                scaleX = selectAnim.value
                                scaleY = selectAnim.value
                            }
                            .run animateEnterExit@{
                                val stiffness = motion.overlayVisibilityStiffness
                                animateEnterExit(
                                    enter = scaleIn(spring(stiffness = stiffness)) +
                                            slideIn(animationSpec = spring(stiffness = stiffness)) {
                                                -targetAnimOffset.toIntOffset()
                                            },
                                    exit = scaleOut(spring(stiffness = stiffness)) +
                                            slideOut(animationSpec = spring(stiffness = stiffness)) {
                                                -targetAnimOffset.toIntOffset()
                                            }
                                )
                            }
                            .matchParentSize(),
                        action = action
                    )
                }
            }
        }
    }
}

private fun sectorItemSizePx(
    itemCount: Int,
    defaultItemSizePx: Float,
    minItemSizePx: Float,
    parentSize: Size,
    position: Position,
    cornerSafePaddingPx: Float,
    initialRadiusRatio: Float,
    itemSpacingRatio: Float
): Float {
    if (parentSize.isEmpty() || itemCount <= 0 || defaultItemSizePx <= 0f) {
        return defaultItemSizePx
    }

    val defaultOffsets = sectorItemOffsets(
        itemCount = itemCount,
        itemSizePx = defaultItemSizePx,
        position = position,
        initialRadiusRatio = initialRadiusRatio,
        itemSpacingRatio = itemSpacingRatio
    )
    val requiredSize = sectorRequiredSafeAxisSize(
        itemOffsets = defaultOffsets,
        itemSizePx = defaultItemSizePx,
        position = position
    )
    val availableSize = when (position) {
        Position.Left, Position.Right -> parentSize.height
        Position.Bottom, Position.Top -> parentSize.width
    } - cornerSafePaddingPx * 2f
    if (requiredSize <= availableSize || availableSize <= 0f) {
        return defaultItemSizePx
    }

    return (defaultItemSizePx * (availableSize / requiredSize))
        .coerceAtLeast(minItemSizePx)
}

private fun sectorRequiredSafeAxisSize(
    itemOffsets: List<Offset>,
    itemSizePx: Float,
    position: Position
): Float {
    if (itemOffsets.isEmpty()) return itemSizePx

    return when (position) {
        Position.Left, Position.Right -> {
            val minY = itemOffsets.minOf { it.y }
            val maxY = itemOffsets.maxOf { it.y }
            maxY - minY + itemSizePx
        }

        Position.Bottom, Position.Top -> {
            val minX = itemOffsets.minOf { it.x }
            val maxX = itemOffsets.maxOf { it.x }
            maxX - minX + itemSizePx
        }
    }
}

private fun sectorItemOffsets(
    itemCount: Int,
    itemSizePx: Float,
    position: Position,
    initialRadiusRatio: Float,
    itemSpacingRatio: Float
): List<Offset> {
    if (itemCount <= 0 || itemSizePx <= 0f) return emptyList()

    val layers = sectorLayerLayouts(
        itemCount = itemCount,
        itemSizePx = itemSizePx,
        initialRadiusRatio = initialRadiusRatio,
        itemSpacingRatio = itemSpacingRatio
    )
    return sectorItemOffsets(layers, position)
}

private fun sectorItemOffsets(
    layouts: List<SectorLayerLayout>,
    position: Position
): List<Offset> {
    val offsets = ArrayList<Offset>(layouts.sumOf { it.angles.size })

    layouts.fastForEach { layer ->
        layer.angles.fastForEach { angle ->
            val inward = cos(angle) * layer.radius
            val cross = sin(angle) * layer.radius
            offsets += when (position) {
                Position.Left -> Offset(inward.toFloat(), cross.toFloat())
                Position.Right -> Offset((-inward).toFloat(), cross.toFloat())
                Position.Bottom -> Offset(cross.toFloat(), (-inward).toFloat())
                Position.Top -> Offset(cross.toFloat(), inward.toFloat())
            }
        }
    }

    return offsets
}

private data class SectorLayerLayout(
    val radius: Float,
    val angles: List<Double>
)

private data class SectorLayerCandidate(
    val layouts: List<SectorLayerLayout>,
    val countScore: Int,
    val distanceScore: Float
)

private fun sectorLayerLayouts(
    itemCount: Int,
    itemSizePx: Float,
    initialRadiusRatio: Float,
    itemSpacingRatio: Float
): List<SectorLayerLayout> {
    if (itemCount <= 0 || itemSizePx <= 0f) return emptyList()

    val targetSpacing = itemSizePx * itemSpacingRatio
    val firstLayerCapacity = sectorLayerCapacity(
        layer = 0,
        itemSizePx = itemSizePx,
        targetSpacing = targetSpacing,
        initialRadiusRatio = initialRadiusRatio
    )
    if (itemCount <= firstLayerCapacity) {
        val radius = sectorSingleLayerRadius(
            count = itemCount,
            itemSizePx = itemSizePx,
            targetSpacing = targetSpacing,
            initialRadiusRatio = initialRadiusRatio
        )
        return listOf(
            SectorLayerLayout(
                radius = radius,
                angles = sectorLayerAngles(
                    count = itemCount,
                    radius = radius,
                    itemSizePx = itemSizePx,
                    targetSpacing = targetSpacing
                )
            )
        )
    }

    val capacities = ArrayList<Int>()
    var layerCount = 1
    var bestCandidate: SectorLayerCandidate? = null

    while (layerCount <= itemCount) {
        capacities += sectorLayerCapacity(
            layer = layerCount - 1,
            itemSizePx = itemSizePx,
            targetSpacing = targetSpacing,
            initialRadiusRatio = initialRadiusRatio
        )
        val counts = sectorLayerCountsOrNull(
            itemCount = itemCount,
            capacities = capacities
        )
        if (counts != null) {
            val layouts = counts.mapIndexed { layer, count ->
                val radius = sectorLayerRadius(
                    layer = layer,
                    itemSizePx = itemSizePx,
                    targetSpacing = targetSpacing,
                    initialRadiusRatio = initialRadiusRatio
                )
                SectorLayerLayout(
                    radius = radius,
                    angles = sectorLayerAngles(
                        count = count,
                        radius = radius,
                        itemSizePx = itemSizePx,
                        targetSpacing = targetSpacing
                    )
                )
            }
            val candidate = SectorLayerCandidate(
                layouts = layouts,
                countScore = sectorLayerCountScore(counts),
                distanceScore = sectorLayerDistanceScore(layouts, targetSpacing)
            )
            if (bestCandidate == null ||
                candidate.countScore < bestCandidate.countScore ||
                candidate.countScore == bestCandidate.countScore &&
                candidate.distanceScore < bestCandidate.distanceScore
            ) {
                bestCandidate = candidate
            }
        }
        layerCount++
    }

    return bestCandidate?.layouts ?: listOf(
        SectorLayerLayout(
            radius = sectorLayerRadius(
                layer = 0,
                itemSizePx = itemSizePx,
                targetSpacing = targetSpacing,
                initialRadiusRatio = initialRadiusRatio
            ),
            angles = sectorLayerAngles(
                count = itemCount,
                radius = sectorLayerRadius(
                    layer = 0,
                    itemSizePx = itemSizePx,
                    targetSpacing = targetSpacing,
                    initialRadiusRatio = initialRadiusRatio
                ),
                itemSizePx = itemSizePx,
                targetSpacing = targetSpacing
            )
        )
    )
}

private fun sectorLayerCountsOrNull(
    itemCount: Int,
    capacities: List<Int>
): List<Int>? {
    val layerCount = capacities.size
    if (layerCount == 1) {
        return if (itemCount <= capacities.first()) listOf(itemCount) else null
    }

    val minRequiredCount = layerCount * (layerCount + 1) / 2
    if (itemCount < minRequiredCount) return null

    val memo = mutableMapOf<Triple<Int, Int, Int>, List<Int>?>()
    return sectorLayerCountsOrNull(
        index = 0,
        previousCount = 0,
        remainingCount = itemCount,
        capacities = capacities,
        memo = memo
    )
}

private fun sectorLayerCountsOrNull(
    index: Int,
    previousCount: Int,
    remainingCount: Int,
    capacities: List<Int>,
    memo: MutableMap<Triple<Int, Int, Int>, List<Int>?>
): List<Int>? {
    val key = Triple(index, previousCount, remainingCount)
    if (key in memo) return memo[key]

    if (index == capacities.size) {
        return if (remainingCount == 0) emptyList() else null
    }

    val remainingLayerCount = capacities.size - index - 1
    val minCount = previousCount + 1
    val maxCount = min(capacities[index], remainingCount)
    var bestCounts: List<Int>? = null
    var bestScore: Int? = null

    for (count in minCount..maxCount) {
        val minRemainingCount = remainingLayerCount * count +
                remainingLayerCount * (remainingLayerCount + 1) / 2
        if (remainingCount - count < minRemainingCount) continue

        val nextCounts = sectorLayerCountsOrNull(
            index = index + 1,
            previousCount = count,
            remainingCount = remainingCount - count,
            capacities = capacities,
            memo = memo
        ) ?: continue
        val counts = listOf(count) + nextCounts
        val score = sectorLayerCountScore(counts)
        if (bestScore == null || score < bestScore) {
            bestScore = score
            bestCounts = counts
        }
    }

    memo[key] = bestCounts
    return bestCounts
}

private fun sectorLayerCountScore(counts: List<Int>): Int {
    if (counts.size <= 1) return 0
    return counts.zipWithNext().sumOf { (innerCount, outerCount) ->
        val diff = outerCount - innerCount - 1
        diff * diff
    }
}

private fun sectorLayerDistanceScore(
    layouts: List<SectorLayerLayout>,
    targetSpacing: Float
): Float {
    val points = layouts.flatMap { layer ->
        layer.angles.map { angle ->
            Offset(
                x = (cos(angle) * layer.radius).toFloat(),
                y = (sin(angle) * layer.radius).toFloat()
            )
        }
    }
    if (points.size <= 1) return 0f

    val nearestDistances = points.mapIndexed { index, point ->
        points.indices
            .filter { it != index }
            .minOf { otherIndex ->
                val other = points[otherIndex]
                sqrt(
                    (point.x - other.x).pow(2) +
                            (point.y - other.y).pow(2)
                )
            }
    }
    val distanceRange = (nearestDistances.maxOrNull() ?: 0f) -
            (nearestDistances.minOrNull() ?: 0f)
    val averageDistance = nearestDistances.average().toFloat()
    return distanceRange + (averageDistance - targetSpacing).let { it * it / targetSpacing }
}

private fun sectorLayerRadius(
    layer: Int,
    itemSizePx: Float,
    targetSpacing: Float,
    initialRadiusRatio: Float
): Float {
    return itemSizePx * initialRadiusRatio + layer * targetSpacing
}

private fun sectorSingleLayerRadius(
    count: Int,
    itemSizePx: Float,
    targetSpacing: Float,
    initialRadiusRatio: Float
): Float {
    val baseRadius = sectorLayerRadius(
        layer = 0,
        itemSizePx = itemSizePx,
        targetSpacing = targetSpacing,
        initialRadiusRatio = initialRadiusRatio
    )
    if (count <= 1) return baseRadius

    val satisfiesSpacing: (Float) -> Boolean = { radius ->
        val preferredAngle = sectorPreferredAngleStep(radius, targetSpacing) * (count - 1)
        preferredAngle <= sectorAvailableAngle(radius, itemSizePx)
    }
    if (satisfiesSpacing(baseRadius)) {
        return baseRadius
    }

    var low = baseRadius
    var high = baseRadius
    while (!satisfiesSpacing(high) && high < itemSizePx * 32f) {
        high *= SectorRadiusStepRatio
    }
    if (!satisfiesSpacing(high)) {
        return high
    }

    repeat(20) {
        val mid = (low + high) / 2f
        if (satisfiesSpacing(mid)) {
            high = mid
        } else {
            low = mid
        }
    }
    return high
}

private fun sectorLayerAngles(
    count: Int,
    radius: Float,
    itemSizePx: Float,
    targetSpacing: Float
): List<Double> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(0.0)

    val availableAngle = sectorAvailableAngle(radius, itemSizePx)
    val preferredAngleStep = sectorPreferredAngleStep(radius, targetSpacing)
    val preferredAngle = preferredAngleStep * (count - 1)
    val angleStep = if (preferredAngle <= availableAngle) {
        preferredAngleStep
    } else {
        availableAngle / (count - 1)
    }
    val startAngle = -angleStep * (count - 1) / 2.0

    return List(count) { index ->
        startAngle + angleStep * index
    }
}

private fun sectorLayerCapacity(
    layer: Int,
    itemSizePx: Float,
    targetSpacing: Float,
    initialRadiusRatio: Float
): Int {
    val radius = sectorLayerRadius(
        layer = layer,
        itemSizePx = itemSizePx,
        targetSpacing = targetSpacing,
        initialRadiusRatio = initialRadiusRatio
    )
    val availableAngle = sectorAvailableAngle(radius, itemSizePx)
    val angleStep = sectorPreferredAngleStep(radius, targetSpacing)
    if (angleStep <= 0.0) return 1
    return floor(availableAngle / angleStep).toInt().coerceAtLeast(0) + 1
}

private fun sectorAvailableAngle(
    radius: Float,
    itemSizePx: Float
): Double {
    val halfSectorAngle = Math.toRadians(SectorAngleDegree) / 2.0
    val edgeSafeAngle = min(
        halfSectorAngle,
        kotlin.math.acos((itemSizePx / 2f / radius).coerceIn(0f, 1f).toDouble())
    )
    return edgeSafeAngle * 2.0
}

private fun sectorPreferredAngleStep(
    radius: Float,
    targetSpacing: Float
): Double {
    return 2.0 * kotlin.math.asin(
        (targetSpacing / (2f * radius)).coerceIn(0f, 1f).toDouble()
    )
}

private fun sectorAnchor(
    parentSize: Size,
    origin: Offset,
    position: Position,
    itemOffsets: List<Offset>,
    itemSizePx: Float,
    edgePaddingPx: Float,
    cornerSafePaddingPx: Float,
    topSafeInsetPx: Float
): Offset {
    if (parentSize.isEmpty()) return Offset.Zero

    val itemSizeHalf = itemSizePx / 2f
    val minX = itemOffsets.minOfOrNull { it.x } ?: 0f
    val maxX = itemOffsets.maxOfOrNull { it.x } ?: 0f
    val minY = itemOffsets.minOfOrNull { it.y } ?: 0f
    val maxY = itemOffsets.maxOfOrNull { it.y } ?: 0f
    val safeOrigin = if (origin.isSpecified) {
        origin
    } else {
        Offset(parentSize.width / 2f, parentSize.height / 2f)
    }

    return when (position) {
        Position.Left, Position.Right -> Offset(
            x = safeOrigin.x.coerceInSafely(
                minimumValue = edgePaddingPx + itemSizeHalf - minX,
                maximumValue = parentSize.width - edgePaddingPx - itemSizeHalf - maxX
            ),
            y = safeOrigin.y.coerceInSafely(
                minimumValue = cornerSafePaddingPx + itemSizeHalf - minY,
                maximumValue = parentSize.height - cornerSafePaddingPx - itemSizeHalf - maxY
            )
        )

        Position.Bottom -> Offset(
            x = safeOrigin.x.coerceInSafely(
                minimumValue = cornerSafePaddingPx + itemSizeHalf - minX,
                maximumValue = parentSize.width - cornerSafePaddingPx - itemSizeHalf - maxX
            ),
            y = safeOrigin.y.coerceInSafely(
                minimumValue = cornerSafePaddingPx + itemSizeHalf - minY,
                maximumValue = parentSize.height - edgePaddingPx - itemSizeHalf - maxY
            )
        )

        Position.Top -> Offset(
            x = safeOrigin.x.coerceInSafely(
                minimumValue = cornerSafePaddingPx + itemSizeHalf - minX,
                maximumValue = parentSize.width - cornerSafePaddingPx - itemSizeHalf - maxX
            ),
            y = safeOrigin.y.coerceInSafely(
                minimumValue = topSafeInsetPx + edgePaddingPx + itemSizeHalf - minY,
                maximumValue = parentSize.height - cornerSafePaddingPx - itemSizeHalf - maxY
            )
        )
    }
}

private fun Float.coerceInSafely(
    minimumValue: Float,
    maximumValue: Float
): Float {
    if (minimumValue <= maximumValue) {
        return coerceIn(minimumValue, maximumValue)
    }
    return (minimumValue + maximumValue) / 2f
}

@Composable
private fun ActionPanelItem(
    action: Action,
    modifier: Modifier = Modifier
) {
    val actionIcon = actionIcon(action = action)
    val isWechatAlipay = remember(actionIcon) {
        actionIcon == R.drawable.wechat_scan ||
                actionIcon == R.drawable.wechat_paycode ||
                actionIcon == R.drawable.alipay_scan ||
                actionIcon == R.drawable.alipay_paycode
    }
    Box(
        modifier = modifier
            .clipToBackground(
                color = actionPanelItemColor(action, actionIcon),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (actionIcon is ImageVector) {
            Image(
                imageVector = actionIcon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
            )
        } else {
            AsyncImage(
                modifier = Modifier
                    .graphicsLayer {
                        if (isWechatAlipay) {
                            scaleX = 0.5f
                            scaleY = 0.5f
                        } else {
                            val appInfo = action.appInfo
                            if (appInfo != null) {
                                scaleX = appInfo.iconScale
                                scaleY = appInfo.iconScale
                                return@graphicsLayer
                            }
                            val shortcutInfo = action.shortcutInfo
                            if (shortcutInfo != null) {
                                scaleX = shortcutInfo.iconScale
                                scaleY = shortcutInfo.iconScale
                            }
                        }
                    },
                model = actionIcon,
                contentDescription = null,
                imageLoader = LocalContext.current.imageLoader,
                colorFilter = if (!isWechatAlipay) null else {
                    ColorFilter.tint(MaterialTheme.appColors.fixedWhite)
                }
            )
        }
    }
}

@Composable
private fun actionPanelItemColor(
    action: Action,
    actionIcon: Any?
): Color {
    return when (action.value) {
        GlobalActions.WECHAT_SCAN,
        GlobalActions.WECHAT_PAY -> MaterialTheme.colorScheme.wechatColor

        GlobalActions.ALIPAY_SCAN,
        GlobalActions.ALIPAY_PAY -> MaterialTheme.colorScheme.alipayColor

        GlobalActions.EXTRA_LAUNCH_APP -> when (actionIcon is ImageVector) {
            true -> MaterialTheme.colorScheme.primary
            else -> Color(action.appInfo!!.iconBgColor)
        }

        GlobalActions.EXTRA_LAUNCH_SHORTCUT -> when (actionIcon is ImageVector) {
            true -> MaterialTheme.colorScheme.primary
            else -> Color(action.shortcutInfo!!.iconBgColor)
        }

        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun AnimatedVisibilityScope.ArcActionPanel(
    actionPanelStyle: ArcStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    vibrations: Vibrations? = null
) {
    val motion = MaterialTheme.motion
    val itemSize = actionPanelStyle.itemSize.toDp()
    // 斜边，从origin原点到item中心的距离，值越大item散得越开
    val hypot = itemSize.toPx() * 2f
    var parentSize by remember { mutableStateOf(Size.Zero) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    parentSize = it.size.toSize()
                }
                .matchParentSize()
        )

        val selectedLabel: String = actionText(actionPanelState.selectedAction)
        val animationSpec = spring<Float>(stiffness = motion.actionPanelPlacementStiffness)
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .displayCutoutPadding()
                .padding(MaterialTheme.dimensions.layout.overlayContentPadding),
            visible = selectedLabel.isNotEmpty(),
            enter = fadeIn(animationSpec) +
                scaleIn(animationSpec, MaterialTheme.motion.actionPanelSecondaryEnterScale),
            exit = fadeOut(animationSpec) +
                scaleOut(animationSpec, MaterialTheme.motion.actionPanelSecondaryEnterScale)
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(
                        color = MaterialTheme.appColors.fixedBlack,
                        offset = Offset(
                            MaterialTheme.dimensions.actionPanel.textShadowOffset.value,
                            MaterialTheme.dimensions.actionPanel.textShadowOffset.value
                        ),
                        blurRadius = MaterialTheme.dimensions.actionPanel.textShadowBlurRadius.value
                    )
                ),
                color = MaterialTheme.appColors.fixedWhite
            )
        }

        Box(
            modifier = Modifier
                .run {
                    val origin = remember(actionPanelState) { actionPanelState.origin }
                    graphicsLayer {
                        if (parentSize.isEmpty()) return@graphicsLayer
                        val itemSizeHalf = itemSize.toPx() / 2f
                        // 限制展开位置，防止显示不全
                        val iconOffset = itemSize.toPx() * 3f
                        val ox = origin.x.let {
                            when (actionPanelState.position) {
                                Position.Left -> it.coerceAtMost(parentSize.width / 2f)
                                Position.Right -> it.coerceAtLeast(parentSize.width / 2f)
                                Position.Bottom -> it.coerceIn(
                                    iconOffset,
                                    parentSize.width - iconOffset
                                )
                                Position.Top -> it.coerceIn(
                                    iconOffset,
                                    parentSize.width - iconOffset
                                )
                            }
                        }
                        val oy = origin.y.let {
                            when (actionPanelState.position) {
                                Position.Left, Position.Right -> it.coerceIn(
                                    minimumValue = iconOffset,
                                    maximumValue = parentSize.height - iconOffset
                                )

                                Position.Bottom -> it.coerceAtLeast(parentSize.height / 2f)
                                Position.Top -> it.coerceAtMost(parentSize.height / 2f)
                            }
                        }
                        translationX = ox - itemSizeHalf
                        translationY = oy - itemSizeHalf
                    }
                }
                .size(itemSize)
        ) {
            val transition = transition
            actionPanelState.actions.fastForEachIndexed { index, action ->
                key(index) {
                    val targetAnimOffset = remember {
                        // 平均每个块之间的角度
                        val avgAngleDegree = 35.0
                        val totalAngleDegree = avgAngleDegree * (actionPanelState.actions.size - 1)
                        val angleDegree = -90.0 - totalAngleDegree / 2.0 + avgAngleDegree * index
                        val radians = Math.toRadians(angleDegree)
                        val neighbor = hypot * cos(radians)
                        val opposite = sqrt(hypot.pow(2) - neighbor.pow(2))
                        // 需要移动的x距离
                        val transX = when (actionPanelState.position) {
                            Position.Left -> opposite
                            Position.Right -> -opposite
                            Position.Bottom -> neighbor
                            Position.Top -> neighbor
                        }
                        // 需要移动的y距离
                        val transY = when (actionPanelState.position) {
                            Position.Left, Position.Right -> neighbor
                            Position.Bottom -> -opposite
                            Position.Top -> opposite
                        }
                        Offset(x = transX.toFloat(), y = transY.toFloat())
                    }
                    val selectAnim = remember { Animatable(1f) }
                    val selectionScale = MaterialTheme.motion.actionPanelSelectionScale

                    var originBounds by remember { mutableStateOf(Rect.Zero) }
                    LaunchedEffect(transition, actionPanelState, index, action, selectAnim) {
                        snapshotFlow { actionPanelState.finger }
                            .filter {
                                it.isSpecified &&
                                        !transition.isRunning &&
                                        transition.currentState == Visible
                            }
                            .collect { finger ->
                                val transFinger = finger - targetAnimOffset
                                if (originBounds.contains(transFinger)) {
                                    if (!actionPanelState.isSelected(action)) {
                                        launch { selectAnim.animateTo(selectionScale) }
                                        actionPanelState.select(index, action)
                                        vibrations?.tryVibrateForActionPanel()
                                    }
                                } else {
                                    if (actionPanelState.isSelected(action)) {
                                        launch { selectAnim.animateTo(1f) }
                                        actionPanelState.select(index, Action.NONE)
                                    }
                                }
                            }
                    }

                    val actionIcon = actionIcon(action = action)
                    val isWechatAlipay = remember(actionIcon) {
                        actionIcon == R.drawable.wechat_scan ||
                                actionIcon == R.drawable.wechat_paycode ||
                                actionIcon == R.drawable.alipay_scan ||
                                actionIcon == R.drawable.alipay_paycode
                    }
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned {
                                originBounds = it.boundsInRoot()
                            }
                            .graphicsLayer {
                                translationX = targetAnimOffset.x
                                translationY = targetAnimOffset.y
                                scaleX = selectAnim.value
                                scaleY = selectAnim.value
                            }
                            .run animateEnterExit@{
                                val stiffness = motion.overlayVisibilityStiffness
                                animateEnterExit(
                                    enter = scaleIn(spring(stiffness = stiffness)) +
                                            slideIn(animationSpec = spring(stiffness = stiffness)) {
                                                -targetAnimOffset.toIntOffset()
                                            },
                                    exit = scaleOut(spring(stiffness = stiffness)) +
                                            slideOut(animationSpec = spring(stiffness = stiffness)) {
                                                -targetAnimOffset.toIntOffset()
                                            }
                                )
                            }
                            .matchParentSize()
                            .clipToBackground(
                                color = when (action.value) {
                                    GlobalActions.WECHAT_SCAN,
                                    GlobalActions.WECHAT_PAY -> MaterialTheme.colorScheme.wechatColor

                                    GlobalActions.ALIPAY_SCAN,
                                    GlobalActions.ALIPAY_PAY -> MaterialTheme.colorScheme.alipayColor

                                    GlobalActions.EXTRA_LAUNCH_APP -> when (actionIcon is ImageVector) {
                                        true -> MaterialTheme.colorScheme.primary
                                        else -> Color(action.appInfo!!.iconBgColor)
                                    }

                                    GlobalActions.EXTRA_LAUNCH_SHORTCUT -> when (actionIcon is ImageVector) {
                                        true -> MaterialTheme.colorScheme.primary
                                        else -> Color(action.shortcutInfo!!.iconBgColor)
                                    }

                                    else -> MaterialTheme.colorScheme.primary
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (actionIcon is ImageVector) {
                            Image(
                                imageVector = actionIcon,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                            )
                        } else {
                            AsyncImage(
                                modifier = Modifier
                                    .graphicsLayer {
                                        if (isWechatAlipay) {
                                            scaleX = 0.5f
                                            scaleY = 0.5f
                                        } else {
                                            val appInfo = action.appInfo
                                            if (appInfo != null) {
                                                scaleX = appInfo.iconScale
                                                scaleY = appInfo.iconScale
                                                return@graphicsLayer
                                            }
                                            val shortcutInfo = action.shortcutInfo
                                            if (shortcutInfo != null) {
                                                scaleX = shortcutInfo.iconScale
                                                scaleY = shortcutInfo.iconScale
                                            }
                                        }
                                    },
                                model = actionIcon,
                                contentDescription = null,
                                imageLoader = LocalContext.current.imageLoader,
                                colorFilter = if (!isWechatAlipay) null else {
                                    ColorFilter.tint(MaterialTheme.appColors.fixedWhite)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberActionPanelState(
    windowModeSwitchDelayMs: Long = AdvancedSettingsDefaults.ActionPanelAppSwitchWindowModeDelayMs
): ActionPanelState {
    val coroutineScope = rememberCoroutineScope()
    val state = remember {
        ActionPanelState(coroutineScope)
    }
    LaunchedEffect(state, windowModeSwitchDelayMs) {
        state.updateWindowModeSwitchDelayMs(windowModeSwitchDelayMs)
    }
    return state
}

class ActionPanelState(private val coroutineScope: CoroutineScope) : LongSlideState() {

    var visible: Boolean by mutableStateOf(false)
        private set
    var actions: List<Action> by mutableStateOf(emptyList())
        private set
    var position: Position by mutableStateOf(Position.Left)
        private set
    var button: GestureButton? by mutableStateOf(null)
        private set
    private val pendingActions: MutableMap<Int, Action> = mutableStateMapOf()

    val selectedAction: Action by derivedStateOf {
        pendingActions.values.find { it != Action.NONE } ?: Action.NONE
    }
    private var triggerType: TriggerType by mutableStateOf(TriggerType.Press)
    private var delayTriggerTypeChangedJob: Job? = null
    private var windowModeSwitchDelayMs: Long = AdvancedSettingsDefaults.ActionPanelAppSwitchWindowModeDelayMs

    override fun onDragStart(offset: Offset) {
        super.onDragStart(offset)
        visible = true
    }

    fun ready(button: GestureButton, actions: List<Action>) {
        this.button = button
        this.position = button.position
        this.actions = actions
    }

    fun done(longPressLaunchPopup: Boolean): Action {
        val action = selectedAction
        val miniWindow = isMiniWindow(longPressLaunchPopup)
        val appInfo = action.appInfo
        val shortcutInfo = action.shortcutInfo
        val data = when {
            appInfo != null -> JsonHelper.encodeToString(appInfo.copy(miniWindow = miniWindow))
            shortcutInfo != null -> JsonHelper.encodeToString(shortcutInfo.copy(miniWindow = miniWindow))
            else -> action.data
        }
        reset()
        return action.copy(data = data)
    }

    fun isMiniWindow(longPressLaunchPopup: Boolean): Boolean {
        val miniWindowInConfig = selectedAction.appInfo?.miniWindow
            ?: selectedAction.shortcutInfo?.miniWindow
            ?: false
        return triggerType.isMiniWindow(!miniWindowInConfig && longPressLaunchPopup)
    }

    fun isSelected(action: Action): Boolean {
        return pendingActions.values.find { it == action } != null
    }

    fun select(index: Int, action: Action) {
        pendingActions[index] = action

        delayTriggerTypeChangedJob?.cancel()
        triggerType = TriggerType.Press
        delayTriggerTypeChangedJob = coroutineScope.launch {
            delay(windowModeSwitchDelayMs)
            triggerType = TriggerType.LongPress
        }
    }

    fun updateWindowModeSwitchDelayMs(value: Long) {
        windowModeSwitchDelayMs = value.coerceIn(
            MinActionPanelAppSwitchWindowModeDelayMs,
            MaxActionPanelAppSwitchWindowModeDelayMs
        )
    }

    override fun reset() {
        visible = false
        pendingActions.clear()
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        button = null
        delayTriggerTypeChangedJob?.cancel()
        triggerType = TriggerType.Press
    }
}

/**
 * 用于实现短按和长按
 */
private enum class TriggerType {

    Press, LongPress
}

private fun TriggerType.isMiniWindow(longPressLaunchPopup: Boolean): Boolean {
    return when (this) {
        TriggerType.Press -> !longPressLaunchPopup
        TriggerType.LongPress -> longPressLaunchPopup
    }
}
