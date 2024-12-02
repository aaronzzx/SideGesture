package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalActions.NONE
import com.aaron.sidegesture.constant.GlobalSettings.GestureButtonColorAlpha
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonLength
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonStart
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonWidth
import com.aaron.sidegesture.constant.GlobalSettings.MinGestureButtonLength
import com.aaron.sidegesture.constant.GlobalSettings.MinGestureButtonStart
import com.aaron.sidegesture.constant.GlobalSettings.MinGestureButtonWidth
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.constant.TriggerDirection.Center
import com.aaron.sidegesture.constant.TriggerDirection.Down
import com.aaron.sidegesture.constant.TriggerDirection.Up
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.actionTextCompose
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.fraction
import com.aaron.sidegesture.ktx.whenPosition
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.MarkColorSize
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.theme.SectionPaddingNoTitle
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/28
 */

@Serializable
@Keep
data class GestureButtonSettings(
    val buttonId: String,
    val position: Int
)

@Composable
fun GestureButtonSettingsScreen(
    onBack: () -> Unit,
    vm: GestureButtonSettingsVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = { }) { uiState ->
        if (uiState.showDeleteWarningDialog) {
            MyAlertDialog(
                onDismissRequest = { vm.showDeleteWarningDialog(false) },
                title = stringResource(id = R.string.delete_gesture_button),
                text = stringResource(id = R.string.delete_gesture_button_hint),
                onConfirmClick = { vm.deleteGestureButton() }
            )
        }
        if (uiState.colorPickerDialog.first) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.surface,
                onDismissRequest = {
                    vm.colorPickerDialog.show(false)
                },
                title = { },
                text = {
                    val color = uiState.colorPickerDialog.second
                    val colorController = rememberColorPickerController()
                    HsvColorPicker(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = 0.5f
                            }
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        initialColor = color,
                        controller = colorController,
                        onColorChanged = { colorEnvelope ->
                            vm.colorPickerDialog.onColorChange(colorEnvelope.color.copy(alpha = 0.2f))
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.colorPickerDialog.confirm()
                            vm.colorPickerDialog.show(false)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vm.colorPickerDialog.show(false)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            )
        }
        if (uiState.actionDialog.first) {
            ActionDialog(
                onDismissRequest = { vm.hideActionDialog() },
                onSelected = { vm.onActionSelect(it) },
                selected = uiState.actionDialog.second,
                actions = GlobalActions.ALL
            )
        }
        if (uiState.longActionDialog.first) {
            LongActionDialog(
                onDismissRequest = { vm.hideLongActionDialog() },
                onConfirm = { vm.onLongActionConfirm(it) },
                onSelected = { vm.onLongActionSelect(it) },
                selected = uiState.longActionDialog.second,
                actions = GlobalActions.ALL
            )
        }

        Box {
            Column {
                TopBar(
                    onBack = onBack,
                    title = uiState.gestureButton.let {
                        when (it?.position) {
                            LEFT -> stringResource(id = R.string.left_gesture_button)
                            RIGHT -> stringResource(id = R.string.right_gesture_button)
                            else -> ""
                        }
                    },
                    postfixTitle = {
                        if (uiState.gestureButton != null) {
                            Box(
                                modifier = Modifier
                                    .padding(start = IconTextPadding)
                                    .size(MarkColorSize)
                                    .background(
                                        color = when (uiState.gestureButton.isDefault) {
                                            true -> MaterialTheme.colorScheme.primary.copy(alpha = GestureButtonColorAlpha)
                                            else -> Color(uiState.gestureButton.color)
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    },
                    actions = {
                        if (uiState.gestureButton?.isDefault != true) {
                            IconButton(onClick = { vm.showDeleteWarningDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
                val gestureButton = uiState.gestureButton
                if (gestureButton != null) {
                    MyColumn {
                        MySection(title = stringResource(id = R.string.slide_action)) {
                            MyGestureSettings(
                                onClick = {
                                    vm.showActionDialog(Center, gestureButton.slideActions.center)
                                },
                                gestureButton = gestureButton,
                                direction = Center,
                                isLongSlide = false,
                                secondaryText = gestureButton.slideActions.center.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    vm.showActionDialog(Up, gestureButton.slideActions.up)
                                },
                                gestureButton = gestureButton,
                                direction = Up,
                                isLongSlide = false,
                                secondaryText = gestureButton.slideActions.up.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    vm.showActionDialog(Down, gestureButton.slideActions.down)
                                },
                                gestureButton = gestureButton,
                                direction = Down,
                                isLongSlide = false,
                                secondaryText = gestureButton.slideActions.down.actionTextCompose()
                            )
                        }

                        MySection(
                            modifier = Modifier.padding(top = SectionPadding),
                            title = stringResource(id = R.string.long_slide_action)
                        ) {
                            MyGestureSettings(
                                onClick = {
                                    vm.showLongActionDialog(Center, gestureButton.longSlideActions.center)
                                },
                                gestureButton = gestureButton,
                                direction = Center,
                                isLongSlide = true,
                                secondaryText = gestureButton.longSlideActions.center.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    vm.showLongActionDialog(Up, gestureButton.longSlideActions.up)
                                },
                                gestureButton = gestureButton,
                                direction = Up,
                                isLongSlide = true,
                                secondaryText = gestureButton.longSlideActions.up.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    vm.showLongActionDialog(Down, gestureButton.longSlideActions.down)
                                },
                                gestureButton = gestureButton,
                                direction = Down,
                                isLongSlide = true,
                                secondaryText = gestureButton.longSlideActions.down.actionTextCompose()
                            )
                        }

                        MySection(modifier = Modifier.padding(top = SectionPaddingNoTitle)) {
                            MyTextSlider(
                                value = gestureButton.width.toFloat(),
                                onValueChange = { vm.onGestureButtonWidthChange(it) },
                                onValueChangeFinished = { vm.saveSettings() },
                                text = stringResource(id = R.string.gesture_button_width),
                                sliderValueHint = stringResource(id = R.string.slider_small) to stringResource(id = R.string.slider_large),
                                valueRange = MinGestureButtonWidth.toFloat()..MaxGestureButtonWidth.toFloat()
                            )
                            MyTextSlider(
                                value = gestureButton.fraction,
                                onValueChange = { vm.onGestureButtonLengthChange(it) },
                                onValueChangeFinished = { vm.saveSettings() },
                                text = stringResource(id = R.string.gesture_button_length),
                                sliderValueHint = stringResource(id = R.string.slider_small) to stringResource(id = R.string.slider_large),
                                valueRange = MinGestureButtonLength..MaxGestureButtonLength
                            )
                            MyTextSlider(
                                value = MaxGestureButtonStart - gestureButton.start,
                                onValueChange = { vm.onGestureButtonLocationChange(it) },
                                onValueChangeFinished = { vm.saveSettings() },
                                text = stringResource(id = R.string.gesture_button_location),
                                sliderValueHint = stringResource(id = R.string.slider_low) to stringResource(id = R.string.slider_high),
                                valueRange = MinGestureButtonStart..MaxGestureButtonStart
                            )
                            MyTextSwitch(
                                onCheckedChange = { vm.onGestureButtonAlignChange(it) },
                                checked = uiState.alignRegion,
                                text = stringResource(id = R.string.gesture_button_align),
                                secondaryText = stringResource(id = R.string.gesture_button_align_hint)
                            )
                        }
                        if (!gestureButton.isDefault) {
                            MySection(modifier = Modifier.padding(top = SectionPaddingNoTitle)) {
                                MyTextButton(
                                    onClick = { vm.colorPickerDialog.show(true) },
                                    text = stringResource(id = R.string.gesture_button_color),
                                    prefix = {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(
                                                    color = Color(gestureButton.color),
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val colorScheme = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        uiState.gestureButtons.fastForEach { button ->
                            val bounds = button.bounds()
                            drawRect(
                                color = when (button.isDefault) {
                                    true -> colorScheme.primary.copy(GestureButtonColorAlpha)
                                    else -> Color(button.color)
                                },
                                topLeft = bounds.topLeft,
                                size = bounds.size
                            )
                        }
                    }
            )
        }
    }
}

@Composable
private fun MyGestureSettings(
    onClick: () -> Unit,
    gestureButton: GestureButton,
    direction: TriggerDirection,
    isLongSlide: Boolean,
    secondaryText: String
) {
    MyTextButton(
        onClick = onClick,
        text = when (direction) {
            Center -> whenPosition(
                onLeft = { stringResource(id = R.string.slide_to_right) },
                onRight = { stringResource(id = R.string.slide_to_left) },
                position = gestureButton.position
            )
            Up -> whenPosition(
                onLeft = { stringResource(id = R.string.slide_to_top_right) },
                onRight = { stringResource(id = R.string.slide_to_top_left) },
                position = gestureButton.position
            )
            Down -> whenPosition(
                onLeft = { stringResource(id = R.string.slide_to_bottom_right) },
                onRight = { stringResource(id = R.string.slide_to_bottom_left) },
                position = gestureButton.position
            )
        },
        secondaryText = run {
            if (secondaryText.isNotEmpty()) {
                return@run secondaryText
            }
            stringResource(id = R.string.action_none)
        },
        secondaryTextColor = MaterialTheme.colorScheme.primary,
        prefix = {
            val imageVector = Icons.Default.ArrowForward
            Icon(
                modifier = Modifier
                    .graphicsLayer {
                        val position = gestureButton.position
                        rotationZ = when (direction) {
                            Up -> if (position == LEFT) -45f else -135f
                            Center -> if (position == LEFT) 0f else 180f
                            Down -> if (position == LEFT) 45f else 135f
                        }
                    }
                    .size(20.dp)
                    .background(
                        color = when (isLongSlide) {
                            true -> MaterialTheme.colorScheme.outlineVariant
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    ),
                imageVector = imageVector,
                contentDescription = null,
                tint = LocalContentColor.current
            )
        }
    )
}

@Composable
private fun ActionDialog(
    onDismissRequest: () -> Unit,
    onSelected: (String) -> Unit,
    selected: String,
    actions: List<String>
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        title = null,
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(
                    items = actions,
                    key = { it }
                ) { item ->
                    Row(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .onSingleClick {
                                onSelected(item)
                                onDismissRequest()
                            }
                            .padding(vertical = ContentPaddingVertical,),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = actionText(action = item, emptyIfNone = false),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        RadioButton(
                            selected = item == selected,
                            onClick = {
                                onSelected(item)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun LongActionDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    onSelected: (List<String>) -> Unit,
    selected: List<String>,
    actions: List<String>,
    maxSelectedCount: Int = 5
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        title = null,
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(
                    items = actions,
                    key = { it }
                ) { item ->
                    val isItemDisabled = (item !in selected && selected.size >= maxSelectedCount) ||
                            (item == NONE && item !in selected && selected.isNotEmpty()) ||
                            (item != NONE && NONE in selected)
                    Row(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = if (isItemDisabled) {
                                    0.36f
                                } else {
                                    1f
                                }
                            }
                            .fillParentMaxWidth()
                            .onClick(enabled = !isItemDisabled) {
                                val newList = selected
                                    .toMutableList()
                                    .also { list ->
                                        if (item in list) {
                                            list.remove(item)
                                        } else if (selected.size < maxSelectedCount) {
                                            list.add(item)
                                        }
                                    }
                                onSelected(newList)
                            }
                            .padding(vertical = ContentPaddingVertical,),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = actionText(action = item, emptyIfNone = false),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Checkbox(
                            enabled = !isItemDisabled,
                            checked = item in selected,
                            onCheckedChange = onCheckedChange@{
                                val newList = selected.toMutableList().also { list ->
                                    if (item in list) {
                                        list.remove(item)
                                    } else if (selected.size < maxSelectedCount) {
                                        list.add(item)
                                    }
                                }
                                onSelected(newList)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selected)
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}