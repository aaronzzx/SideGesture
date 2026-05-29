package com.aaron.sidegesture.quicktools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.QuickToolType
import com.aaron.sidegesture.entity.global.QuickToolsSettings
import com.aaron.sidegesture.ktx.gotoManageWriteSettings
import com.aaron.sidegesture.ktx.gotoNotificationListenerSettings
import com.aaron.sidegesture.ui.widget.MySlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun QuickToolsControlCenter(
    service: SideGestureService,
    settings: QuickToolsSettings,
    state: QuickToolsControlCenterState,
    onOverlayTouchChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledTypes = remember(settings) {
        settings.items.filter { it.enabled }.map { it.type }
    }
    val mediaState = rememberQuickToolsMediaControllerState()
    var brightness by remember(state.visible, state.refreshTick) {
        mutableFloatStateOf(QuickToolsExecutor.currentBrightnessRatio(service))
    }
    var volume by remember(state.visible, state.refreshTick) {
        mutableFloatStateOf(QuickToolsExecutor.currentVolumeRatio(service))
    }
    val wifiEnabled = remember(state.visible, state.refreshTick) {
        QuickToolsExecutor.currentWifiEnabled(service)
    }
    val bluetoothEnabled = remember(state.visible, state.refreshTick) {
        QuickToolsExecutor.currentBluetoothEnabled(service)
    }
    val muteEnabled = remember(state.visible, state.refreshTick) {
        QuickToolsExecutor.currentMuteEnabled(service)
    }
    val flashlightEnabled = remember(state.visible, state.refreshTick) {
        QuickToolsExecutor.currentFlashlightEnabled(service)
    }
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.surface.luminance() < 0.5f
    val panelColors = remember(colorScheme, isDarkTheme) {
        quickToolsPanelColors(colorScheme = colorScheme, isDarkTheme = isDarkTheme)
    }

    LaunchedEffect(state.visible) {
        onOverlayTouchChange(state.visible)
        if (state.visible) {
            mediaState.refresh()
        }
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
        ) {
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val cutoutInsets = WindowInsets.displayCutout
            val safeLeftPadding = with(density) { 16.dp + cutoutInsets.getLeft(density, layoutDirection).toDp() }
            val safeTopPadding = with(density) { 16.dp + cutoutInsets.getTop(density).toDp() }
            val safeRightPadding = with(density) { 16.dp + cutoutInsets.getRight(density, layoutDirection).toDp() }
            val safeBottomPadding = with(density) { 16.dp + cutoutInsets.getBottom(density).toDp() }
            val panelSize = remember(density) {
                IntSize(
                    with(density) { QuickToolsGridSpec.PanelWidth.toPx().roundToInt() },
                    with(density) { QuickToolsGridSpec.PanelHeight.toPx().roundToInt() }
                )
            }
            val panelOffset = remember(
                this.maxWidth,
                maxHeight,
                panelSize,
                safeLeftPadding,
                safeTopPadding,
                safeRightPadding,
                safeBottomPadding,
                state.fingerAnchor,
                state.triggerEdge
            ) {
                val containerSize = with(density) {
                    IntSize(maxWidth.roundToPx(), maxHeight.roundToPx())
                }
                computePanelOffset(
                    containerSize = containerSize,
                    panelSize = panelSize,
                    fingerAnchor = state.fingerAnchor,
                    triggerEdge = state.triggerEdge,
                    safeLeftPadding = safeLeftPadding,
                    safeTopPadding = safeTopPadding,
                    safeRightPadding = safeRightPadding,
                    safeBottomPadding = safeBottomPadding,
                    density = density
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        state.hide()
                }
            )

            Surface(
                modifier = Modifier
                    .offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) }
                    .padding(QuickToolsGridSpec.PanelOuterPadding)
                    .width(QuickToolsGridSpec.PanelWidth)
                    .height(QuickToolsGridSpec.PanelHeight)
                    .clip(RoundedCornerShape(QuickToolsGridSpec.PanelCornerRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                shape = RoundedCornerShape(QuickToolsGridSpec.PanelCornerRadius),
                color = panelColors.panelContainer,
                contentColor = panelColors.onPanel,
                tonalElevation = 0.dp,
                shadowElevation = if (isDarkTheme) 10.dp else 14.dp
            ) {
                QuickToolsGrid(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(QuickToolsGridSpec.PanelInnerPadding),
                    types = enabledTypes,
                    mediaState = mediaState,
                    brightness = brightness,
                    volume = volume,
                    colors = panelColors,
                    wifiEnabled = wifiEnabled,
                    bluetoothEnabled = bluetoothEnabled,
                    muteEnabled = muteEnabled,
                    flashlightEnabled = flashlightEnabled,
                    onOpenPermission = {
                        service.gotoNotificationListenerSettings()
                        state.hide()
                    },
                    onBrightnessChange = { value ->
                        brightness = value
                        scope.launch {
                            when (QuickToolsExecutor.setBrightnessRatio(service, value)) {
                                QuickToolsOperationResult.Success -> state.refresh()
                                QuickToolsOperationResult.NeedsWriteSettingsOrShizuku -> {
                                    service.gotoManageWriteSettings()
                                }
                                else -> Unit
                            }
                        }
                    },
                    onVolumeChange = { value ->
                        volume = value
                        QuickToolsExecutor.setVolumeRatio(service, value)
                    },
                    onClick = { type ->
                        scope.launch {
                            handleQuickToolClick(
                                type = type,
                                service = service,
                                state = state
                            )
                        }
                    }
                )
            }
        }
    }
}

private suspend fun handleQuickToolClick(
    type: QuickToolType,
    service: SideGestureService,
    state: QuickToolsControlCenterState
) {
    when (type) {
        QuickToolType.Flashlight -> {
            QuickToolsExecutor.performExistingAction(service, GlobalActions.FLASHLIGHT)
            delay(150)
            state.refresh()
        }
        QuickToolType.Mute -> {
            QuickToolsExecutor.performExistingAction(service, GlobalActions.MUTE)
            delay(150)
            state.refresh()
        }
        QuickToolType.Wifi -> {
            when (QuickToolsExecutor.toggleWifi(service)) {
                QuickToolsOperationResult.Success -> state.refresh()
                QuickToolsOperationResult.NeedsShizuku -> {
                    QuickToolsExecutor.openWifiFallback(service)
                    state.hide()
                }
                else -> {
                    QuickToolsExecutor.openWifiFallback(service)
                    state.hide()
                }
            }
        }
        QuickToolType.Bluetooth -> {
            when (QuickToolsExecutor.toggleBluetooth(service)) {
                QuickToolsOperationResult.Success -> state.refresh()
                QuickToolsOperationResult.NeedsShizuku -> {
                    QuickToolsExecutor.openBluetoothFallback(service)
                    state.hide()
                }
                else -> {
                    QuickToolsExecutor.openBluetoothFallback(service)
                    state.hide()
                }
            }
        }
        QuickToolType.NotificationPanel -> {
            QuickToolsExecutor.performExistingAction(service, GlobalActions.OPEN_NOTIFICATION_PANEL)
            state.hide()
        }
        QuickToolType.QuickSettingsPanel -> {
            QuickToolsExecutor.performExistingAction(service, GlobalActions.OPEN_QUICK_PANEL)
            state.hide()
        }
        QuickToolType.LockScreen -> {
            QuickToolsExecutor.performExistingAction(service, GlobalActions.LOCK_SCREEN)
            state.hide()
        }
        QuickToolType.Screenshot -> {
            QuickToolsExecutor.performExistingAction(service, GlobalActions.SCREENSHOT)
            state.hide()
        }
        QuickToolType.MediaControl,
        QuickToolType.Brightness,
        QuickToolType.Volume -> Unit
    }
}

@Composable
private fun QuickToolsGrid(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    types: List<QuickToolType>,
    mediaState: QuickToolsMediaControllerState,
    brightness: Float,
    volume: Float,
    colors: QuickToolsPanelColors,
    wifiEnabled: Boolean,
    bluetoothEnabled: Boolean,
    muteEnabled: Boolean,
    flashlightEnabled: Boolean,
    onOpenPermission: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onClick: (QuickToolType) -> Unit
) {
    LazyVerticalGrid(
        modifier = modifier,
        contentPadding = contentPadding,
        columns = GridCells.Fixed(QuickToolsGridSpec.Columns),
        horizontalArrangement = Arrangement.spacedBy(QuickToolsGridSpec.ItemSpacing),
        verticalArrangement = Arrangement.spacedBy(QuickToolsGridSpec.ItemSpacing)
    ) {
        items(
            items = types,
            key = { it },
            span = { type -> GridItemSpan(type.layoutSpan().columnSpan) }
        ) { type ->
            QuickToolGridItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(type.layoutSpan().itemHeight()),
                type = type,
                mediaState = mediaState,
                brightness = brightness,
                volume = volume,
                colors = colors,
                wifiEnabled = wifiEnabled,
                bluetoothEnabled = bluetoothEnabled,
                muteEnabled = muteEnabled,
                flashlightEnabled = flashlightEnabled,
                onOpenPermission = onOpenPermission,
                onBrightnessChange = onBrightnessChange,
                onVolumeChange = onVolumeChange,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun QuickToolGridItem(
    type: QuickToolType,
    mediaState: QuickToolsMediaControllerState,
    brightness: Float,
    volume: Float,
    colors: QuickToolsPanelColors,
    wifiEnabled: Boolean,
    bluetoothEnabled: Boolean,
    muteEnabled: Boolean,
    flashlightEnabled: Boolean,
    onOpenPermission: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onClick: (QuickToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    when (type) {
        QuickToolType.MediaControl -> CompactMediaCard(
            modifier = modifier,
            state = mediaState,
            colors = colors,
            onOpenPermission = onOpenPermission,
        )
        QuickToolType.Brightness -> CompactSliderRow(
            modifier = modifier,
            icon = quickToolIcon(QuickToolType.Brightness),
            contentDescription = stringResource(R.string.quick_tool_brightness),
            value = brightness,
            colors = colors,
            onValueChange = onBrightnessChange
        )
        QuickToolType.Volume -> CompactSliderRow(
            modifier = modifier,
            icon = quickToolIcon(QuickToolType.Volume),
            contentDescription = stringResource(R.string.quick_tool_volume),
            value = volume,
            colors = colors,
            onValueChange = onVolumeChange
        )
        else -> QuickToolCircleButton(
            modifier = modifier,
            type = type,
            buttonSize = QuickToolsGridSpec.CompactButtonSize,
            colors = colors,
            active = when (type) {
                QuickToolType.Flashlight -> flashlightEnabled
                QuickToolType.Mute -> muteEnabled
                QuickToolType.Wifi -> wifiEnabled
                QuickToolType.Bluetooth -> bluetoothEnabled
                else -> false
            },
            statusDotAlignment = when (type) {
                QuickToolType.Flashlight,
                QuickToolType.Mute,
                QuickToolType.Wifi,
                QuickToolType.Bluetooth -> if (
                    (type == QuickToolType.Flashlight && flashlightEnabled) ||
                    (type == QuickToolType.Mute && muteEnabled) ||
                    (type == QuickToolType.Wifi && wifiEnabled) ||
                    (type == QuickToolType.Bluetooth && bluetoothEnabled)
                ) {
                    Alignment.TopEnd
                } else {
                    null
                }
                else -> null
            },
            onClick = {
                onClick(type)
            }
        )
    }
}

@Composable
private fun CompactSliderRow(
    icon: ImageVector,
    contentDescription: String,
    value: Float,
    colors: QuickToolsPanelColors,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = colors.rowContainer,
        contentColor = colors.onPanel,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.primarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            MySlider(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                value = value,
                onValueChange = {
                    onValueChange(it)
                }
            )
        }
    }
}

@Composable
private fun CompactMediaCard(
    state: QuickToolsMediaControllerState,
    colors: QuickToolsPanelColors,
    onOpenPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = state.info
    val mediaText = remember(info.title, info.artist) {
        when {
            info.title.isNotBlank() && info.artist.isNotBlank() -> "${info.title} · ${info.artist}"
            info.title.isNotBlank() -> info.title
            info.artist.isNotBlank() -> info.artist
            else -> ""
        }
    }
    val hasArtwork = info.artwork != null
    val titleColor = if (hasArtwork) Color.White else colors.onPanel
    val secondaryColor = if (hasArtwork) Color.White else colors.subText
    val iconContainerColor = if (hasArtwork) Color.Black.copy(alpha = 0.54f) else colors.iconContainer
    Surface(
        modifier = modifier,
        color = colors.mediaContainer,
        contentColor = colors.onPanel,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasArtwork) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = info.artwork!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.42f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!info.permissionGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.shapedClickable(RoundedCornerShape(12.dp)) {
                                onOpenPermission()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primary
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                text = stringResource(R.string.quick_tools_open_listener_settings),
                                color = colors.onPrimary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    return@Column
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE),
                        text = mediaText.ifBlank { stringResource(R.string.quick_tools_no_media) },
                        color = if (mediaText.isBlank()) secondaryColor else titleColor,
                        fontSize = 15.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                            CompactIconButton(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.quick_tool_media_control),
                                onClick = {
                                    state.skipPrevious()
                                },
                                size = 28.dp,
                                iconSize = 16.dp,
                                containerColor = iconContainerColor,
                                iconTint = titleColor
                            )
                        }
                        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = colors.primary
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .shapedClickable(CircleShape) {
                                            state.togglePlayPause()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (info.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.quick_tool_media_control),
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                            CompactIconButton(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.quick_tool_media_control),
                                onClick = {
                                    state.skipNext()
                                },
                                size = 28.dp,
                                iconSize = 16.dp,
                                containerColor = iconContainerColor,
                                iconTint = titleColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolCircleButton(
    type: QuickToolType,
    buttonSize: Dp,
    colors: QuickToolsPanelColors,
    active: Boolean,
    statusDotAlignment: Alignment?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToggle = type.isToggleType()
    val containerColor = if (isToggle && active) colors.primary else colors.iconContainer
    val iconTint = if (isToggle && active) colors.onPrimary else colors.onPanel
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(buttonSize)
                .shapedClickable(CircleShape, onClick),
            shape = CircleShape,
            color = containerColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = quickToolIcon(type),
                    contentDescription = quickToolText(type),
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        if (statusDotAlignment != null) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .padding(3.dp),
                contentAlignment = statusDotAlignment
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.statusDot)
                )
            }
        }
    }
}

@Composable
private fun CompactIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp,
    containerColor: Color = Color.Transparent,
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shapedClickable(CircleShape, onClick),
            shape = CircleShape,
            color = containerColor
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(iconSize),
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    tint = iconTint
                )
            }
        }
    }
}

private fun Modifier.shapedClickable(
    shape: Shape,
    onClick: () -> Unit
): Modifier {
    return clip(shape).clickable(onClick = onClick)
}

private fun QuickToolType.isToggleType(): Boolean {
    return this == QuickToolType.Flashlight ||
        this == QuickToolType.Mute ||
        this == QuickToolType.Wifi ||
        this == QuickToolType.Bluetooth
}

private data class QuickToolsPanelColors(
    val panelContainer: Color,
    val rowContainer: Color,
    val mediaContainer: Color,
    val iconContainer: Color,
    val primary: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val onPanel: Color,
    val subText: Color,
    val track: Color,
    val statusDot: Color
)

private fun quickToolsPanelColors(
    colorScheme: ColorScheme,
    isDarkTheme: Boolean
): QuickToolsPanelColors {
    return if (isDarkTheme) {
        QuickToolsPanelColors(
            panelContainer = colorScheme.surfaceContainer.copy(alpha = 0.96f),
            rowContainer = colorScheme.surfaceContainerHighest.copy(alpha = 0.94f),
            mediaContainer = colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
            iconContainer = colorScheme.surfaceContainerHigh,
            primary = colorScheme.primary,
            primarySoft = colorScheme.primary.copy(alpha = 0.18f),
            onPrimary = colorScheme.onPrimary,
            onPanel = colorScheme.onSurface,
            subText = colorScheme.onSurfaceVariant,
            track = colorScheme.onSurface.copy(alpha = 0.14f),
            statusDot = colorScheme.secondary
        )
    } else {
        QuickToolsPanelColors(
            panelContainer = colorScheme.surface,
            rowContainer = colorScheme.surfaceContainer,
            mediaContainer = colorScheme.surfaceContainerHigh,
            iconContainer = colorScheme.surfaceContainerHighest,
            primary = colorScheme.primary,
            primarySoft = colorScheme.primary.copy(alpha = 0.12f),
            onPrimary = colorScheme.onPrimary,
            onPanel = colorScheme.onSurface,
            subText = colorScheme.onSurfaceVariant,
            track = colorScheme.onSurface.copy(alpha = 0.12f),
            statusDot = colorScheme.secondary
        )
    }
}

private fun computePanelOffset(
    containerSize: IntSize,
    panelSize: IntSize,
    fingerAnchor: Offset,
    triggerEdge: Position,
    safeLeftPadding: Dp,
    safeTopPadding: Dp,
    safeRightPadding: Dp,
    safeBottomPadding: Dp,
    density: androidx.compose.ui.unit.Density
): Offset {
    if (containerSize == IntSize.Zero || panelSize == IntSize.Zero) {
        return Offset.Zero
    }
    val safeLeftPx = with(density) { safeLeftPadding.toPx() }
    val safeTopPx = with(density) { safeTopPadding.toPx() }
    val safeRightPx = with(density) { safeRightPadding.toPx() }
    val safeBottomPx = with(density) { safeBottomPadding.toPx() }
    val anchor = if (fingerAnchor != Offset.Unspecified) {
        fingerAnchor
    } else {
        Offset(containerSize.width / 2f, containerSize.height / 2f)
    }
    val minY = safeTopPx
    val maxY = containerSize.height - panelSize.height - safeBottomPx
    val bottomY = containerSize.height - panelSize.height - safeBottomPx
    val leftX = safeLeftPx
    val rightX = containerSize.width - panelSize.width - safeRightPx

    val preferred = when (triggerEdge) {
        Position.Left -> Offset(leftX, anchor.y - panelSize.height / 2f)
        Position.Right -> Offset(rightX, anchor.y - panelSize.height / 2f)
        Position.Bottom -> {
            val attachLeft = anchor.x <= containerSize.width / 2f
            Offset(if (attachLeft) leftX else rightX, bottomY)
        }
    }

    return Offset(
        x = preferred.x.coerceIn(leftX, rightX.coerceAtLeast(leftX)),
        y = preferred.y.coerceIn(minY, maxY.coerceAtLeast(minY))
    )
}
