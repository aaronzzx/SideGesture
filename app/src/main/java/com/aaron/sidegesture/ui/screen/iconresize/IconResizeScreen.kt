package com.aaron.sidegesture.ui.screen.iconresize

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.DimAlpha
import com.aaron.sidegesture.constant.ScaleableDefaults.DEFAULT_SCALE
import com.aaron.sidegesture.constant.ScaleableDefaults.MAX_SCALE
import com.aaron.sidegesture.constant.ScaleableDefaults.MIN_SCALE
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.widget.ColorPickerDialog
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */

@Composable
fun IconResizeScreen(
    onBack: () -> Unit,
    vm: IconResizeVM = viewModel()
) {
    val defaultBgColor = MaterialTheme.colorScheme.primary
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        if (uiState.showResetWarningDialog) {
            MyAlertDialog(
                onDismissRequest = { vm.showResetWarningDialog(false) },
                onConfirmClick = { vm.reset() },
                title = stringResource(id = R.string.reset_default_settings_warning),
                text = stringResource(id = R.string.reset_icon_settings)
            )
        }
        if (uiState.showColorPickerDialog) {
            ColorPickerDialog(
                onDismissRequest = {
                    vm.showColorPickerDialog(false)
                },
                onColorPicked = { color ->
                    vm.onBgColorChange(color)
                },
                initialColor = uiState.selectedBgColor?.color ?: defaultBgColor
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                TopBar(
                    onBack = onBack,
                    title = stringResource(id = R.string.icon_resize),
                    actions = {
                        IconButton(onClick = { vm.showResetWarningDialog(true) }) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                        }
                        IconButton(onClick = { vm.done() }) {
                            Icon(imageVector = Icons.Default.Done, contentDescription = null)
                        }
                    }
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        vertical = ContentPaddingVerticalWithSection,
                        horizontal = ContentPaddingHorizontal * 2
                    ),
                    horizontalArrangement = Arrangement.spacedBy(ContentPaddingHorizontal)
                ) {
                    itemsIndexed(
                        items = uiState.ids,
                        key = { _, item -> item }
                    ) { _, id ->
                        BadgedBox(
                            modifier = Modifier
                                .size(MinInteractiveSize)
                                .onClick(enableRipple = false) {
                                    vm.onSelectedIdChange(id)
                                },
                            badge = {
                                val curScaleFactors by rememberUpdatedState(newValue = uiState.scaleFactors)
                                val curBgColors by rememberUpdatedState(newValue = uiState.bgColors)
                                val visible by remember(id) {
                                    derivedStateOf {
                                        val scale = curScaleFactors[id]
                                        (scale != null && scale != DEFAULT_SCALE) ||
                                                (curBgColors[id]?.enabled == true)
                                    }
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    Badge(
                                        modifier = Modifier.requiredSize(16.dp),
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        ) {
                            AsyncImage(
                                modifier = Modifier.matchParentSize(),
                                model = uiState.icons[id],
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(250.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .drawWithCache {
                            val bounds = Rect(Offset.Zero, size)
                            val path = Path().apply {
                                addOval(bounds)
                            }
                            onDrawWithContent {
                                drawContent()
                                clipPath(path = path, clipOp = ClipOp.Difference) {
                                    drawRect(color = Color.Black.copy(DimAlpha))
                                }
                            }
                        }
                        .clip(RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    LazyVerticalGrid(
                        modifier = Modifier.matchParentSize(),
                        columns = GridCells.Fixed(11),
                        userScrollEnabled = false
                    ) {
                        items(11 * 11) { index ->
                            val color = when (index % 2 == 0) {
                                true -> Color.LightGray
                                else -> Color.White
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(color = color)
                            )
                        }
                    }

                    AnimatedContent(
                        modifier = Modifier.matchParentSize(),
                        targetState = uiState.selectedId to uiState.selectedBgColor,
                        label = "IconChangeAnimation"
                    ) { (id, selectedBgColor) ->
                        val scaleFactor by rememberUpdatedState(newValue = uiState.scaleFactors[id] ?: DEFAULT_SCALE)
                        Box {
                            if (selectedBgColor?.enabled == true) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(CircleShape)
                                        .background(color = selectedBgColor.color ?: defaultBgColor)
                                )
                            }

                            AsyncImage(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            val newScale =
                                                (scaleFactor * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                            vm.onScaleChange(newScale)
                                        }
                                    }
                                    .graphicsLayer {
                                        scaleX = scaleFactor
                                        scaleY = scaleFactor
                                    },
                                model = uiState.icons[id],
                                contentDescription = null
                            )
                        }
                    }
                }

                MyTextSwitch(
                    onTextClick = {
                        vm.showColorPickerDialog(true)
                    },
                    onCheckedChange = { enabled ->
                        vm.onBgColorEnabled(enabled, defaultBgColor)
                    },
                    checked = uiState.selectedBgColor?.enabled ?: false,
                    text = stringResource(id = R.string.background_color),
                    markColor = uiState.selectedBgColor?.color ?: defaultBgColor
                )
            }
        }
    }
}