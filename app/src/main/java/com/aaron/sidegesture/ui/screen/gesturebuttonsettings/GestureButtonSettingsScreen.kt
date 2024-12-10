package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.GestureButtonColorAlpha
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonLength
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonStart
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonWidth
import com.aaron.sidegesture.constant.GlobalSettings.MinGestureButtonLength
import com.aaron.sidegesture.constant.GlobalSettings.MinGestureButtonStart
import com.aaron.sidegesture.constant.GlobalSettings.MinGestureButtonWidth
import com.aaron.sidegesture.entity.ActionSelect
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.entity.TriggerDirection.Center
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Up
import com.aaron.sidegesture.ktx.actionTextCompose
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.fraction
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

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/28
 */

@Composable
fun GestureButtonSettingsScreen(
    onBack: () -> Unit,
    onNavToActionSelect: (ActionSelect) -> Unit,
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
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        initialColor = color,
                        controller = colorController,
                        onColorChanged = { colorEnvelope ->
                            vm.colorPickerDialog.onColorChange(colorEnvelope.color)
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

        Box {
            Column {
                TopBar(
                    onBack = onBack,
                    title = uiState.gestureButton.let {
                        if (it == null) return@let ""
                        when (it.position) {
                            Position.Left -> stringResource(id = R.string.left_gesture_button)
                            Position.Right -> stringResource(id = R.string.right_gesture_button)
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
                                            else -> Color(uiState.gestureButton.color).copy(alpha = GestureButtonColorAlpha)
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
                            val navToActionSelect: (TriggerDirection) -> Unit = {
                                val actionSelect = ActionSelect(
                                    gestureButtonId = gestureButton.id,
                                    position = gestureButton.position,
                                    direction = it,
                                    isLongSlide = false
                                )
                                onNavToActionSelect(actionSelect)
                            }
                            MyGestureSettings(
                                onClick = {
                                    navToActionSelect(Center)
                                },
                                gestureButton = gestureButton,
                                direction = Center,
                                isLongSlide = false,
                                secondaryText = gestureButton.slideActions.center.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    navToActionSelect(Up)
                                },
                                gestureButton = gestureButton,
                                direction = Up,
                                isLongSlide = false,
                                secondaryText = gestureButton.slideActions.up.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    navToActionSelect(Down)
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
                            val navToActionSelect: (TriggerDirection) -> Unit = {
                                val actionSelect = ActionSelect(
                                    gestureButtonId = gestureButton.id,
                                    position = gestureButton.position,
                                    direction = it,
                                    isLongSlide = true
                                )
                                onNavToActionSelect(actionSelect)
                            }
                            MyGestureSettings(
                                onClick = {
                                    navToActionSelect(Center)
                                },
                                gestureButton = gestureButton,
                                direction = Center,
                                isLongSlide = true,
                                secondaryText = gestureButton.longSlideActions.center.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    navToActionSelect(Up)
                                },
                                gestureButton = gestureButton,
                                direction = Up,
                                isLongSlide = true,
                                secondaryText = gestureButton.longSlideActions.up.actionTextCompose()
                            )
                            MyGestureSettings(
                                onClick = {
                                    navToActionSelect(Down)
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
                                onValueChangeFinished = { vm.onGestureButtonAdjustFinish() },
                                text = stringResource(id = R.string.gesture_button_width),
                                sliderValueHint = stringResource(id = R.string.slider_small) to stringResource(id = R.string.slider_large),
                                valueRange = MinGestureButtonWidth.toFloat()..MaxGestureButtonWidth.toFloat()
                            )
                            MyTextSlider(
                                value = gestureButton.fraction,
                                onValueChange = { vm.onGestureButtonLengthChange(it) },
                                onValueChangeFinished = { vm.onGestureButtonAdjustFinish() },
                                text = stringResource(id = R.string.gesture_button_length),
                                sliderValueHint = stringResource(id = R.string.slider_short) to stringResource(id = R.string.slider_long),
                                valueRange = MinGestureButtonLength..MaxGestureButtonLength
                            )
                            MyTextSlider(
                                value = MaxGestureButtonStart - gestureButton.start,
                                onValueChange = { vm.onGestureButtonLocationChange(it) },
                                onValueChangeFinished = { vm.onGestureButtonAdjustFinish() },
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
                            val color = when (button.isDefault) {
                                true -> colorScheme.primary
                                else -> Color(button.color)
                            }
                            val highlight = uiState.isGestureButtonAdjusting &&
                                    button.id == uiState.gestureButton?.id
                            drawRect(
                                color = when (highlight) {
                                    true -> color
                                    else -> color.copy(alpha = GestureButtonColorAlpha)
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
            Center -> when (gestureButton.position) {
                Position.Left -> stringResource(id = R.string.slide_to_right)
                Position.Right -> stringResource(id = R.string.slide_to_left)
            }
            Up -> when (gestureButton.position) {
                Position.Left -> stringResource(id = R.string.slide_to_top_right)
                Position.Right -> stringResource(id = R.string.slide_to_top_left)
            }
            Down -> when (gestureButton.position) {
                Position.Left -> stringResource(id = R.string.slide_to_bottom_right)
                Position.Right -> stringResource(id = R.string.slide_to_bottom_left)
            }
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
                            Up -> when (position) {
                                Position.Left -> -45f
                                Position.Right -> -135f
                            }
                            Center -> when (position) {
                                Position.Left -> 0f
                                Position.Right -> 180f
                            }
                            Down -> when (position) {
                                Position.Left -> 45f
                                Position.Right -> 135f
                            }
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