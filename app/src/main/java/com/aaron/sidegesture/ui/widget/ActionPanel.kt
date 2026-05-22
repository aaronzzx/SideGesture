package com.aaron.sidegesture.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toDp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.DimAlpha
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.FolderStyle
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.SectorStyle
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.ktx.actionIcon
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.alipayColor
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.isMiniWindow
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ktx.toIntOffset
import com.aaron.sidegesture.ktx.tryVibrateForActionPanel
import com.aaron.sidegesture.ktx.wechatColor
import com.aaron.sidegesture.ui.theme.RootPadding
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
    AnimatedVisibility(
        modifier = modifier,
        visible = actionPanelState.visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = Color.Black.copy(DimAlpha))
            )

            val selectedAction = actionPanelState.selectedAction
            val selectedLabel = actionText(selectedAction)
            val animationSpec = spring<Float>(stiffness = Spring.StiffnessHigh)
            val enter = fadeIn(animationSpec) + scaleIn(animationSpec, 0.9f)
            val exit = fadeOut(animationSpec) + scaleOut(animationSpec, 0.9f)


            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .displayCutoutPadding()
                    .padding(RootPadding),
                visible = selectedAction.value == GlobalActions.EXTRA_LAUNCH_APP,
                enter = enter,
                exit = ExitTransition.None
            ) {
                BoxWithConstraints {
                    Box(
                        modifier = Modifier
                            .let { thisModifier ->
                                val miniWindow = actionPanelState.triggerType.isMiniWindow(longPressLaunchPopup)
                                val maxWidth = this@BoxWithConstraints.maxWidth
                                val maxHeight = this@BoxWithConstraints.maxHeight
                                val spec = spring<Dp>(stiffness = 5000f)
                                val width by animateDpAsState(
                                    targetValue = when (miniWindow) {
                                        true -> 200.dp
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
                                color = Color.White.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp)
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
                    .padding(RootPadding),
                visible = selectedLabel.isNotEmpty(),
                enter = enter,
                exit = exit
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(
                            color = Color.Black, offset = Offset(2.0f, 2.0f), blurRadius = 3f
                        )
                    ),
                    color = Color.White
                )
            }
        }
    }
}

private val FolderEdgePadding = 16.dp
private val FolderCornerSafePadding = 56.dp
private val FolderScrollHotZone = 28.dp
private const val FolderAutoScrollSpeedPxPerFrame = 12f
private const val FolderAutoScrollFrameDelayMs = 16L

@Composable
private fun AnimatedVisibilityScope.FolderActionPanel(
    actionPanelStyle: FolderStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    vibrations: Vibrations? = null
) {
    val density = LocalDensity.current
    val itemSize = actionPanelStyle.itemSize.toDp()
    val itemSpacing = actionPanelStyle.itemSpacing.toDp()
    val horizontalPadding = actionPanelStyle.horizontalPadding.toDp()
    val verticalPadding = actionPanelStyle.verticalPadding.toDp()
    val cornerRadius = actionPanelStyle.cornerRadius.toDp()
    val itemSizePx = itemSize.toPx()
    val itemSpacingPx = itemSpacing.toPx()
    val horizontalPaddingPx = horizontalPadding.toPx()
    val verticalPaddingPx = verticalPadding.toPx()
    val edgePaddingPx = FolderEdgePadding.toPx()
    val cornerSafePaddingPx = FolderCornerSafePadding.toPx()
    val scrollHotZonePx = FolderScrollHotZone.toPx()
    val columns = actionPanelStyle.columns.coerceAtLeast(1)
    val maxRows = actionPanelStyle.maxRows.coerceAtLeast(1)
    val rows = ceil(actionPanelState.actions.size / columns.toFloat()).roundToInt()
    val visibleRows = rows.coerceAtMost(maxRows).coerceAtLeast(1)
    val gridWidthPx = columns * itemSizePx + (columns - 1) * itemSpacingPx
    val gridHeightPx = visibleRows * itemSizePx + (visibleRows - 1) * itemSpacingPx
    val panelWidthPx = gridWidthPx + horizontalPaddingPx * 2f
    val panelHeightPx = gridHeightPx + verticalPaddingPx * 2f
    val panelWidth = with(density) { panelWidthPx.toDp() }
    val panelHeight = with(density) { panelHeightPx.toDp() }
    val gridHeight = with(density) { gridHeightPx.toDp() }
    val scrollState = rememberScrollState()
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
        scrollState.scrollTo(0)
        autoScrollDirection = 0
    }

    LaunchedEffect(autoScrollDirection, scrollState) {
        val direction = autoScrollDirection
        while (direction != 0 && scrollState.maxValue > 0) {
            val target = (scrollState.value + direction * FolderAutoScrollSpeedPxPerFrame)
                .roundToInt()
                .coerceIn(0, scrollState.maxValue)
            if (target != scrollState.value) {
                scrollState.scrollTo(target)
            }
            delay(FolderAutoScrollFrameDelayMs)
        }
    }

    LaunchedEffect(
        transition,
        actionPanelState,
        scrollState,
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
                val canScroll = scrollState.maxValue > 0
                autoScrollDirection = when {
                    !inPanel || !canScroll -> 0
                    finger.y <= panelBounds.top + scrollHotZonePx -> -1
                    finger.y >= panelBounds.bottom - scrollHotZonePx -> 1
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
            cornerSafePaddingPx
        ) {
            folderPanelAnchor(
                parentSize = parentSize,
                origin = stableOrigin,
                position = actionPanelState.position,
                panelWidthPx = panelWidthPx,
                panelHeightPx = panelHeightPx,
                edgePaddingPx = edgePaddingPx,
                cornerSafePaddingPx = cornerSafePaddingPx
            )
        }

        Box(
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
                .background(
                    color = Color.White.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                )
                .animateEnterExit(
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                            scaleIn(spring(stiffness = Spring.StiffnessMedium), 0.92f),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                            scaleOut(spring(stiffness = Spring.StiffnessMedium), 0.92f)
                )
        ) {
            Column(
                modifier = Modifier
                    .height(gridHeight)
                    .verticalScroll(scrollState, enabled = false),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                repeat(rows) { rowIndex ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                    ) {
                        repeat(columns) { columnIndex ->
                            val index = rowIndex * columns + columnIndex
                            val action = actionPanelState.actions.getOrNull(index)
                            if (action == null) {
                                Box(modifier = Modifier.size(itemSize))
                            } else {
                                key(index) {
                                    val scale by animateFloatAsState(
                                        targetValue = if (actionPanelState.isSelected(action)) 1.15f else 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
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
            }
        }
    }
}

private fun folderPanelAnchor(
    parentSize: Size,
    origin: Offset,
    position: Position,
    panelWidthPx: Float,
    panelHeightPx: Float,
    edgePaddingPx: Float,
    cornerSafePaddingPx: Float
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
    }
}

private const val SectorAngleDegree = 180.0
private const val SectorInitialRadiusRatio = 1.5f
private const val SectorRadiusStepRatio = 1.25f
private const val SectorItemSpacingRatio = 1.12f
private const val SectorSingleLayerMaxCount = 5
private val SectorEdgePadding = 16.dp
private val SectorCornerSafePadding = 56.dp
private val SectorMinItemSize = 32.dp

@Composable
private fun AnimatedVisibilityScope.SectorActionPanel(
    actionPanelStyle: SectorStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    vibrations: Vibrations? = null
) {
    val density = LocalDensity.current
    val defaultItemSize = actionPanelStyle.itemSize.toDp()
    val defaultItemSizePx = defaultItemSize.toPx()
    val edgePaddingPx = SectorEdgePadding.toPx()
    val cornerSafePaddingPx = SectorCornerSafePadding.toPx()
    val minItemSizePx = SectorMinItemSize.toPx()
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
            cornerSafePaddingPx,
            minItemSizePx
        ) {
            sectorItemSizePx(
                itemCount = actionPanelState.actions.size,
                defaultItemSizePx = defaultItemSizePx,
                minItemSizePx = minItemSizePx,
                parentSize = parentSize,
                position = actionPanelState.position,
                cornerSafePaddingPx = cornerSafePaddingPx
            )
        }
        val itemSize = with(density) { itemSizePx.toDp() }
        val layouts = remember(
            actionPanelState.actions.size,
            itemSizePx
        ) {
            sectorLayerLayouts(
                itemCount = actionPanelState.actions.size,
                itemSizePx = itemSizePx
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
        val firstLayerOffsets = remember(
            actionPanelState.position,
            layouts
        ) {
            sectorItemOffsets(
                layouts = layouts.take(1),
                position = actionPanelState.position
            )
        }
        val anchor = remember(
            parentSize,
            stableOrigin,
            actionPanelState.position,
            itemOffsets,
            firstLayerOffsets,
            itemSizePx,
            edgePaddingPx,
            cornerSafePaddingPx
        ) {
            sectorAnchor(
                parentSize = parentSize,
                origin = stableOrigin,
                position = actionPanelState.position,
                itemOffsets = itemOffsets,
                firstLayerOffsets = firstLayerOffsets,
                itemSizePx = itemSizePx,
                edgePaddingPx = edgePaddingPx,
                cornerSafePaddingPx = cornerSafePaddingPx
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
                                        launch { selectAnim.animateTo(1.15f) }
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
                                val stiffness = Spring.StiffnessMedium
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
    cornerSafePaddingPx: Float
): Float {
    if (parentSize.isEmpty() || itemCount <= 0 || defaultItemSizePx <= 0f) {
        return defaultItemSizePx
    }

    val defaultOffsets = sectorItemOffsets(
        itemCount = itemCount,
        itemSizePx = defaultItemSizePx,
        position = position
    )
    val requiredSize = sectorRequiredSafeAxisSize(
        itemOffsets = defaultOffsets,
        itemSizePx = defaultItemSizePx,
        position = position
    )
    val availableSize = when (position) {
        Position.Left, Position.Right -> parentSize.height
        Position.Bottom -> parentSize.width
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

        Position.Bottom -> {
            val minX = itemOffsets.minOf { it.x }
            val maxX = itemOffsets.maxOf { it.x }
            maxX - minX + itemSizePx
        }
    }
}

private fun sectorItemOffsets(
    itemCount: Int,
    itemSizePx: Float,
    position: Position
): List<Offset> {
    if (itemCount <= 0 || itemSizePx <= 0f) return emptyList()

    val layers = sectorLayerLayouts(itemCount, itemSizePx)
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
    itemSizePx: Float
): List<SectorLayerLayout> {
    if (itemCount <= 0 || itemSizePx <= 0f) return emptyList()

    val targetSpacing = itemSizePx * SectorItemSpacingRatio
    if (itemCount <= SectorSingleLayerMaxCount) {
        val radius = sectorSingleLayerRadius(itemCount, itemSizePx, targetSpacing)
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
            targetSpacing = targetSpacing
        )
        val counts = sectorLayerCountsOrNull(
            itemCount = itemCount,
            capacities = capacities
        )
        if (counts != null) {
            val layouts = counts.mapIndexed { layer, count ->
                val radius = sectorLayerRadius(layer, itemSizePx, targetSpacing)
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
            radius = sectorLayerRadius(0, itemSizePx, targetSpacing),
            angles = sectorLayerAngles(
                count = itemCount,
                radius = sectorLayerRadius(0, itemSizePx, targetSpacing),
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
    targetSpacing: Float
): Float {
    return itemSizePx * SectorInitialRadiusRatio + layer * targetSpacing
}

private fun sectorSingleLayerRadius(
    count: Int,
    itemSizePx: Float,
    targetSpacing: Float
): Float {
    val baseRadius = sectorLayerRadius(0, itemSizePx, targetSpacing)
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
        high *= 1.25f
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
    targetSpacing: Float
): Int {
    val radius = sectorLayerRadius(layer, itemSizePx, targetSpacing)
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
    firstLayerOffsets: List<Offset>,
    itemSizePx: Float,
    edgePaddingPx: Float,
    cornerSafePaddingPx: Float
): Offset {
    if (parentSize.isEmpty()) return Offset.Zero

    val itemSizeHalf = itemSizePx / 2f
    val minX = itemOffsets.minOfOrNull { it.x } ?: 0f
    val maxX = itemOffsets.maxOfOrNull { it.x } ?: 0f
    val minY = itemOffsets.minOfOrNull { it.y } ?: 0f
    val maxY = itemOffsets.maxOfOrNull { it.y } ?: 0f
    val firstLayerMinX = firstLayerOffsets.minOfOrNull { it.x } ?: 0f
    val firstLayerMaxX = firstLayerOffsets.maxOfOrNull { it.x } ?: 0f
    val firstLayerMaxY = firstLayerOffsets.maxOfOrNull { it.y } ?: 0f
    val safeOrigin = if (origin.isSpecified) {
        origin
    } else {
        Offset(parentSize.width / 2f, parentSize.height / 2f)
    }

    return when (position) {
        Position.Left -> Offset(
            x = edgePaddingPx + itemSizeHalf - firstLayerMinX,
            y = safeOrigin.y.coerceInSafely(
                minimumValue = cornerSafePaddingPx + itemSizeHalf - minY,
                maximumValue = parentSize.height - cornerSafePaddingPx - itemSizeHalf - maxY
            )
        )

        Position.Right -> Offset(
            x = parentSize.width - edgePaddingPx - itemSizeHalf - firstLayerMaxX,
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
            y = parentSize.height - edgePaddingPx - itemSizeHalf - firstLayerMaxY
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
                    ColorFilter.tint(Color.White)
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
        val animationSpec = spring<Float>(stiffness = Spring.StiffnessHigh)
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .displayCutoutPadding()
                .padding(RootPadding),
            visible = selectedLabel.isNotEmpty(),
            enter = fadeIn(animationSpec) + scaleIn(animationSpec, 0.9f),
            exit = fadeOut(animationSpec) + scaleOut(animationSpec, 0.9f)
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(
                        color = Color.Black, offset = Offset(2.0f, 2.0f), blurRadius = 3f
                    )
                ),
                color = Color.White
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
                            }
                        }
                        val oy = origin.y.let {
                            when (actionPanelState.position) {
                                Position.Left, Position.Right -> it.coerceIn(
                                    minimumValue = iconOffset,
                                    maximumValue = parentSize.height - iconOffset
                                )

                                Position.Bottom -> it.coerceAtLeast(parentSize.height / 2f)
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
                        }
                        // 需要移动的y距离
                        val transY = when (actionPanelState.position) {
                            Position.Left, Position.Right -> neighbor
                            Position.Bottom -> -opposite
                        }
                        Offset(x = transX.toFloat(), y = transY.toFloat())
                    }
                    val selectAnim = remember { Animatable(1f) }

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
                                        launch { selectAnim.animateTo(1.15f) }
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
                                val stiffness = Spring.StiffnessMedium
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
                                    ColorFilter.tint(Color.White)
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
fun rememberActionPanelState(): ActionPanelState {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        ActionPanelState(coroutineScope)
    }
}

class ActionPanelState(private val coroutineScope: CoroutineScope) : LongSlideState() {

    var visible: Boolean by mutableStateOf(false)
        private set
    var actions: List<Action> by mutableStateOf(emptyList())
        private set
    var position: Position by mutableStateOf(Position.Left)
        private set
    private val pendingActions: MutableMap<Int, Action> = mutableStateMapOf()

    val selectedAction: Action by derivedStateOf {
        pendingActions.values.find { it != Action.NONE } ?: Action.NONE
    }
    var triggerType: TriggerType by mutableStateOf(TriggerType.Press)
        private set
    private var delayTriggerTypeChangedJob: Job? = null

    override fun onDragStart(offset: Offset) {
        super.onDragStart(offset)
        visible = true
    }

    fun ready(position: Position, actions: List<Action>) {
        this.position = position
        this.actions = actions
    }

    fun done(): Action {
        val action = selectedAction
        val triggerType = triggerType
        reset()
        return action.copy(extra = triggerType)
    }

    fun isSelected(action: Action): Boolean {
        return pendingActions.values.find { it == action } != null
    }

    fun select(index: Int, action: Action) {
        pendingActions[index] = action

        delayTriggerTypeChangedJob?.cancel()
        triggerType = TriggerType.Press
        delayTriggerTypeChangedJob = coroutineScope.launch {
            delay(500)
            triggerType = TriggerType.LongPress
        }
    }

    override fun reset() {
        visible = false
        pendingActions.clear()
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        delayTriggerTypeChangedJob?.cancel()
        triggerType = TriggerType.Press
    }

    /**
     * 用于实现短按和长按
     */
    enum class TriggerType {

        Press, LongPress
    }
}
