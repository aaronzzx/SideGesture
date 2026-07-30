package com.aaron.sidegesture.feature.quicktools

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
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.QuickToolType
import com.aaron.sidegesture.entity.global.QuickToolsSettings
import com.aaron.sidegesture.ktx.gotoManageWriteSettings
import com.aaron.sidegesture.ktx.gotoNotificationListenerSettings
import com.aaron.sidegesture.ui.theme.AppAlpha
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.appColors
import com.aaron.sidegesture.ui.theme.componentShapes
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.elevations
import com.aaron.sidegesture.ui.theme.textStyles
import com.aaron.sidegesture.ui.widget.MySlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun QuickToolsControlCenter(
    service: SideGestureService,
    settings: QuickToolsSettings,
    state: QuickToolsControlCenterState,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledTypes = remember(settings) {
        settings.items.filter { it.enabled }.map { it.type }
    }
    val brightness = state.brightnessRatio
    var volume by remember(state.visible, state.refreshTick) {
        mutableFloatStateOf(QuickToolsExecutor.currentVolumeRatio(service))
    }
    var lastNonZeroVolume by remember(state.visible) {
        mutableFloatStateOf(
            QuickToolsExecutor.currentVolumeRatio(service).takeIf { it > 0f } ?: 0.3f
        )
    }
    val brightnessAutoEnabled = state.brightnessAutoEnabled
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
    val dimensions = MaterialTheme.dimensions.quickTools
    val layout = remember(dimensions) { calculateQuickToolsLayout(dimensions) }
    val alpha = MaterialTheme.alpha
    val isDarkTheme = colorScheme.surface.luminance() < 0.5f
    val panelColors = remember(colorScheme, isDarkTheme, alpha) {
        quickToolsPanelColors(
            colorScheme = colorScheme,
            isDarkTheme = isDarkTheme,
            alpha = alpha
        )
    }

    AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visible = state.visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val mediaState = rememberQuickToolsMediaControllerState()

        LaunchedEffect(state.visible) {
            if (state.visible) {
                mediaState.refresh()
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.appColors.fixedBlack.copy(
                        alpha = if (isDarkTheme) alpha.overlayScrimDark else alpha.overlayScrimLight
                    )
                )
        ) {
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val cutoutInsets = WindowInsets.displayCutout
            val systemBarInsets = WindowInsets.systemBars
            val safeLeftPadding = with(density) {
                dimensions.edgePadding + maxOf(
                    cutoutInsets.getLeft(density, layoutDirection),
                    systemBarInsets.getLeft(density, layoutDirection)
                ).toDp()
            }
            val safeTopPadding = with(density) {
                dimensions.edgePadding + maxOf(
                    cutoutInsets.getTop(density),
                    systemBarInsets.getTop(density)
                ).toDp()
            }
            val safeRightPadding = with(density) {
                dimensions.edgePadding + maxOf(
                    cutoutInsets.getRight(density, layoutDirection),
                    systemBarInsets.getRight(density, layoutDirection)
                ).toDp()
            }
            val safeBottomPadding = with(density) {
                dimensions.edgePadding + maxOf(
                    cutoutInsets.getBottom(density),
                    systemBarInsets.getBottom(density)
                ).toDp()
            }
            val panelSize = remember(
                density,
                layout.panelWidth,
                layout.panelHeight
            ) {
                IntSize(
                    with(density) { layout.panelWidth.toPx().roundToInt() },
                    with(density) { layout.panelHeight.toPx().roundToInt() }
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
                    .padding(layout.panelOuterPadding)
                    .width(layout.panelWidth)
                    .height(layout.panelHeight)
                    .clip(MaterialTheme.componentShapes.quickToolsPanel)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                shape = MaterialTheme.componentShapes.quickToolsPanel,
                color = panelColors.panelContainer,
                contentColor = panelColors.onPanel,
                tonalElevation = MaterialTheme.elevations.quickToolsTonal,
                shadowElevation = if (isDarkTheme) {
                    MaterialTheme.elevations.quickToolsShadowDark
                } else {
                    MaterialTheme.elevations.quickToolsShadowLight
                }
            ) {
                QuickToolsGrid(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(layout.panelInnerPadding),
                    layout = layout,
                    types = enabledTypes,
                    mediaState = mediaState,
                    brightness = brightness,
                    volume = volume,
                    brightnessAutoEnabled = brightnessAutoEnabled,
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
                        if (!state.brightnessCanWrite) {
                            state.hide()
                            service.gotoManageWriteSettings()
                        } else {
                            scope.launch {
                                when (state.setBrightnessRatio(value)) {
                                    QuickToolsOperationResult.NeedsWriteSettingsOrShizuku -> {
                                        state.hide()
                                        service.gotoManageWriteSettings()
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    },
                    onBrightnessAutoClick = {
                        if (!state.brightnessCanWrite) {
                            state.hide()
                            service.gotoManageWriteSettings()
                        } else {
                            scope.launch {
                                when (state.toggleBrightnessAuto()) {
                                    QuickToolsOperationResult.NeedsWriteSettingsOrShizuku -> {
                                        state.hide()
                                        service.gotoManageWriteSettings()
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    },
                    onVolumeChange = { value ->
                        volume = value
                        if (value > 0f) {
                            lastNonZeroVolume = value
                        }
                        QuickToolsExecutor.setVolumeRatio(service, value)
                    },
                    onMediaMuteClick = {
                        if (volume > 0f) {
                            lastNonZeroVolume = volume
                            volume = 0f
                            QuickToolsExecutor.setVolumeRatio(service, 0f)
                        } else {
                            val restore = lastNonZeroVolume
                            volume = restore
                            QuickToolsExecutor.setVolumeRatio(service, restore)
                        }
                    },
                    onClick = { type ->
                        scope.launch {
                            handleQuickToolClick(
                                type = type,
                                service = service,
                                state = state,
                                onAction = onAction
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
    state: QuickToolsControlCenterState,
    onAction: (Action) -> Unit
) {
    when (type) {
        QuickToolType.Flashlight -> {
            onAction(Action(GlobalActions.FLASHLIGHT))
            delay(150)
            state.refresh()
        }
        QuickToolType.Mute -> {
            onAction(Action(GlobalActions.MUTE))
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
            onAction(Action(GlobalActions.OPEN_NOTIFICATION_PANEL))
            state.hide()
        }
        QuickToolType.QuickSettingsPanel -> {
            onAction(Action(GlobalActions.OPEN_QUICK_PANEL))
            state.hide()
        }
        QuickToolType.LockScreen -> {
            onAction(Action(GlobalActions.LOCK_SCREEN))
            state.hide()
        }
        QuickToolType.Screenshot -> {
            onAction(Action(GlobalActions.SCREENSHOT))
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
    layout: QuickToolsLayoutMetrics,
    types: List<QuickToolType>,
    mediaState: QuickToolsMediaControllerState,
    brightness: Float,
    volume: Float,
    brightnessAutoEnabled: Boolean,
    colors: QuickToolsPanelColors,
    wifiEnabled: Boolean,
    bluetoothEnabled: Boolean,
    muteEnabled: Boolean,
    flashlightEnabled: Boolean,
    onOpenPermission: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessAutoClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMediaMuteClick: () -> Unit,
    onClick: (QuickToolType) -> Unit
) {
    LazyVerticalGrid(
        modifier = modifier,
        contentPadding = contentPadding,
        columns = GridCells.Fixed(QuickToolsGridSpec.Columns),
        horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing)
    ) {
        items(
            items = types,
            key = { it },
            span = { type -> GridItemSpan(type.layoutSpan().columnSpan) }
        ) { type ->
            QuickToolGridItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(type.layoutSpan().itemHeight(layout)),
                type = type,
                mediaState = mediaState,
                brightness = brightness,
                volume = volume,
                brightnessAutoEnabled = brightnessAutoEnabled,
                colors = colors,
                wifiEnabled = wifiEnabled,
                bluetoothEnabled = bluetoothEnabled,
                muteEnabled = muteEnabled,
                flashlightEnabled = flashlightEnabled,
                compactButtonSize = layout.compactButtonSize,
                onOpenPermission = onOpenPermission,
                onBrightnessChange = onBrightnessChange,
                onBrightnessAutoClick = onBrightnessAutoClick,
                onVolumeChange = onVolumeChange,
                onMediaMuteClick = onMediaMuteClick,
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
    brightnessAutoEnabled: Boolean,
    colors: QuickToolsPanelColors,
    wifiEnabled: Boolean,
    bluetoothEnabled: Boolean,
    muteEnabled: Boolean,
    flashlightEnabled: Boolean,
    compactButtonSize: Dp,
    onOpenPermission: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessAutoClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onMediaMuteClick: () -> Unit,
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
            onValueChange = onBrightnessChange,
            active = brightnessAutoEnabled,
            onIconClick = onBrightnessAutoClick
        )
        QuickToolType.Volume -> CompactSliderRow(
            modifier = modifier,
            icon = if (volume <= 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = stringResource(R.string.quick_tool_volume),
            value = volume,
            colors = colors,
            onValueChange = onVolumeChange,
            active = volume <= 0f,
            onIconClick = onMediaMuteClick
        )
        else -> QuickToolCircleButton(
            modifier = modifier,
            type = type,
            buttonSize = compactButtonSize,
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
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onIconClick: (() -> Unit)? = null
) {
    val dimensions = MaterialTheme.dimensions.quickTools
    val iconContainerColor = if (active) colors.primary else colors.primarySoft
    val iconTint = if (active) colors.onPrimary else colors.primary
    val click = onIconClick
    val iconModifier = if (click != null) {
        Modifier
            .size(dimensions.sliderIconContainerSize)
            .shapedClickable(CircleShape) {
                click()
            }
    } else {
        Modifier
            .size(dimensions.sliderIconContainerSize)
            .clip(CircleShape)
    }
    Surface(
        modifier = modifier,
        color = colors.rowContainer,
        contentColor = colors.onPanel,
        shape = MaterialTheme.componentShapes.quickToolsSlider
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimensions.sliderHorizontalPadding,
                    vertical = dimensions.sliderVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.sliderContentGap)
        ) {
            Box(
                modifier = iconModifier.background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(dimensions.sliderIconSize)
                )
            }
            MySlider(
                modifier = Modifier
                    .weight(1f)
                    .height(dimensions.sliderHeight),
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
    val dimensions = MaterialTheme.dimensions.quickTools
    val appColors = MaterialTheme.appColors
    val alpha = MaterialTheme.alpha
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
    val titleColor = if (hasArtwork) appColors.fixedWhite else colors.onPanel
    val secondaryColor = if (hasArtwork) appColors.fixedWhite else colors.subText
    val iconContainerColor = if (hasArtwork) {
        appColors.fixedBlack.copy(alpha = alpha.quickToolsArtworkIconContainer)
    } else {
        colors.iconContainer
    }
    Surface(
        modifier = modifier,
        color = colors.mediaContainer,
        contentColor = colors.onPanel,
        shape = MaterialTheme.componentShapes.quickToolsMedia
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
                                    appColors.fixedBlack.copy(
                                        alpha = alpha.quickToolsArtworkGradientStart
                                    ),
                                    appColors.fixedBlack.copy(
                                        alpha = alpha.quickToolsArtworkGradientEnd
                                    )
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = dimensions.mediaHorizontalPadding,
                        vertical = dimensions.mediaVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensions.mediaContentGap)
            ) {
                if (!info.permissionGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(dimensions.permissionOuterPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.shapedClickable(
                                MaterialTheme.componentShapes.quickToolsPermission
                            ) {
                                onOpenPermission()
                            },
                            shape = MaterialTheme.componentShapes.quickToolsPermission,
                            color = colors.primary
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    horizontal = dimensions.permissionHorizontalPadding,
                                    vertical = dimensions.permissionVerticalPadding
                                ),
                                text = stringResource(R.string.quick_tools_open_listener_settings),
                                color = colors.onPrimary,
                                style = MaterialTheme.textStyles.quickToolsPermission,
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
                        style = MaterialTheme.textStyles.quickToolsMedia,
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
                        Box(
                            modifier = Modifier.size(dimensions.mediaButtonSize),
                            contentAlignment = Alignment.Center
                        ) {
                            CompactIconButton(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.quick_tool_media_control),
                                onClick = {
                                    state.skipPrevious()
                                },
                                size = dimensions.mediaButtonSize,
                                iconSize = dimensions.mediaIconSize,
                                containerColor = iconContainerColor,
                                iconTint = titleColor
                            )
                        }
                        Box(
                            modifier = Modifier.size(dimensions.mediaPrimaryButtonSize),
                            contentAlignment = Alignment.Center
                        ) {
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
                                        modifier = Modifier.size(dimensions.mediaPrimaryIconSize)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.size(dimensions.mediaButtonSize),
                            contentAlignment = Alignment.Center
                        ) {
                            CompactIconButton(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.quick_tool_media_control),
                                onClick = {
                                    state.skipNext()
                                },
                                size = dimensions.mediaButtonSize,
                                iconSize = dimensions.mediaIconSize,
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
    val dimensions = MaterialTheme.dimensions.quickTools
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
                    modifier = Modifier.size(dimensions.toolIconSize)
                )
            }
        }
        if (statusDotAlignment != null) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .padding(dimensions.statusDotPadding),
                contentAlignment = statusDotAlignment
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensions.statusDotSize)
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
    size: Dp,
    iconSize: Dp,
    containerColor: Color = Color.Transparent,
    iconTint: Color
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
    isDarkTheme: Boolean,
    alpha: AppAlpha
): QuickToolsPanelColors {
    return if (isDarkTheme) {
        QuickToolsPanelColors(
            panelContainer = colorScheme.surfaceContainer.copy(alpha = alpha.quickToolsDarkPanel),
            rowContainer = colorScheme.surfaceContainerHighest.copy(alpha = alpha.quickToolsDarkRow),
            mediaContainer = colorScheme.surfaceContainerHighest.copy(alpha = alpha.quickToolsDarkMedia),
            iconContainer = colorScheme.surfaceContainerHigh,
            primary = colorScheme.primary,
            primarySoft = colorScheme.primary.copy(alpha = alpha.quickToolsDarkPrimarySoft),
            onPrimary = colorScheme.onPrimary,
            onPanel = colorScheme.onSurface,
            subText = colorScheme.onSurfaceVariant,
            track = colorScheme.onSurface.copy(alpha = alpha.quickToolsDarkTrack),
            statusDot = colorScheme.secondary
        )
    } else {
        QuickToolsPanelColors(
            panelContainer = colorScheme.surface,
            rowContainer = colorScheme.surfaceContainer,
            mediaContainer = colorScheme.surfaceContainerHigh,
            iconContainer = colorScheme.surfaceContainerHighest,
            primary = colorScheme.primary,
            primarySoft = colorScheme.primary.copy(alpha = alpha.quickToolsLightPrimarySoft),
            onPrimary = colorScheme.onPrimary,
            onPanel = colorScheme.onSurface,
            subText = colorScheme.onSurfaceVariant,
            track = colorScheme.onSurface.copy(alpha = alpha.quickToolsLightTrack),
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
        Position.Top -> {
            val attachLeft = anchor.x <= containerSize.width / 2f
            Offset(if (attachLeft) leftX else rightX, minY)
        }
    }

    return Offset(
        x = preferred.x.coerceIn(leftX, rightX.coerceAtLeast(leftX)),
        y = preferred.y.coerceIn(minY, maxY.coerceAtLeast(minY))
    )
}
