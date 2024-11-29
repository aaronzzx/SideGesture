package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import androidx.annotation.Keep
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.actionTextCompose
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.fraction
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.theme.SectionPaddingNoTitle
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
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
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

        Box {
            Column {
                TopBar(
                    onBack = onBack,
                    title = stringResource(id = R.string.gesture_button_settings)
                )
                val gestureButton = uiState.gestureButton
                if (gestureButton != null) {
                    MyColumn {
                        MySection(title = stringResource(id = R.string.press_action)) {
                            MyGestureSettings(
                                onClick = { /*TODO*/ },
                                gestureButton = gestureButton,
                                direction = TriggerDirection.Center,
                                isLongSlide = false,
                                secondaryText = gestureButton.pressActions.center.actionTextCompose
                            )
                            MyGestureSettings(
                                onClick = { /*TODO*/ },
                                gestureButton = gestureButton,
                                direction = TriggerDirection.Up,
                                isLongSlide = false,
                                secondaryText = gestureButton.pressActions.up.actionTextCompose
                            )
                            MyGestureSettings(
                                onClick = { /*TODO*/ },
                                gestureButton = gestureButton,
                                direction = TriggerDirection.Down,
                                isLongSlide = false,
                                secondaryText = gestureButton.pressActions.down.actionTextCompose
                            )
                        }

                        MySection(
                            modifier = Modifier.padding(top = SectionPadding),
                            title = stringResource(id = R.string.long_press_action)
                        ) {
                            MyGestureSettings(
                                onClick = { /*TODO*/ },
                                gestureButton = gestureButton,
                                direction = TriggerDirection.Center,
                                isLongSlide = true,
                                secondaryText = gestureButton.longPressActions.center.actionTextCompose
                            )
                            MyGestureSettings(
                                onClick = { /*TODO*/ },
                                gestureButton = gestureButton,
                                direction = TriggerDirection.Up,
                                isLongSlide = true,
                                secondaryText = gestureButton.longPressActions.up.actionTextCompose
                            )
                            MyGestureSettings(
                                onClick = { /*TODO*/ },
                                gestureButton = gestureButton,
                                direction = TriggerDirection.Down,
                                isLongSlide = true,
                                secondaryText = gestureButton.longPressActions.down.actionTextCompose
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
                        val button = uiState.gestureButton ?: return@drawBehind
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
            TriggerDirection.Center -> when (gestureButton.position) {
                LEFT -> stringResource(id = R.string.slide_to_right)
                RIGHT -> stringResource(id = R.string.slide_to_left)
                else -> error("Unknown position: ${gestureButton.position}")
            }
            TriggerDirection.Up -> when (gestureButton.position) {
                LEFT -> stringResource(id = R.string.slide_to_top_right)
                RIGHT -> stringResource(id = R.string.slide_to_top_left)
                else -> error("Unknown position: ${gestureButton.position}")
            }
            TriggerDirection.Down -> when (gestureButton.position) {
                LEFT -> stringResource(id = R.string.slide_to_bottom_right)
                RIGHT -> stringResource(id = R.string.slide_to_bottom_left)
                else -> error("Unknown position: ${gestureButton.position}")
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
                            TriggerDirection.Up -> if (position == LEFT) -45f else -135f
                            TriggerDirection.Center -> if (position == LEFT) 0f else 180f
                            TriggerDirection.Down -> if (position == LEFT) 45f else 135f
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