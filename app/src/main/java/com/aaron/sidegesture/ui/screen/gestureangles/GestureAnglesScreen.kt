package com.aaron.sidegesture.ui.screen.gestureangles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.angle1
import com.aaron.sidegesture.ui.widget.TopBar
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/26
 */

@Serializable
data object GestureAngles

@Composable
fun GestureAnglesScreen(
    onBack: () -> Unit,
    vm: GestureAnglesVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        if (uiState.showResetWarningDialog) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.surface,
                onDismissRequest = {
                    vm.showResetWarningDialog(false)
                },
                title = { Text(text = stringResource(id = R.string.reset_default_settings)) },
                text = {
                    Text(text = stringResource(id = R.string.reset_gesture_angles_warning))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.showResetWarningDialog(false)
                            vm.saveSettings()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vm.showResetWarningDialog(false)
                            vm.reset()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            )
        }
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.gesture_angles),
                actions = {
                    IconButton(onClick = { vm.showResetWarningDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Reset"
                        )
                    }
                    IconButton(onClick = { vm.saveSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Save"
                        )
                    }
                }
            )
            AdjustAngle(
                modifier = Modifier.fillMaxSize(),
                angle = uiState.angle,
                position = uiState.position
            )
        }
    }
}

@Composable
private fun AdjustAngle(
    angle: GestureAngle,
    modifier: Modifier = Modifier,
    position: Int = GestureButton.LEFT,
    color: Color = MaterialTheme.colorScheme.primary
) {

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val myCenter = when (position) {
            GestureButton.LEFT -> center.copy(x = 0f)
            GestureButton.RIGHT -> center.copy(x = size.width)
            else -> error("Unknown position: $position")
        }
        clipRect {
            drawCircle(
                color = color,
                radius = radius,
                center = myCenter,
                alpha = 0.1f
            )
            drawCircle(
                color = color,
                radius = radius,
                center = myCenter,
                alpha = 0.35f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        drawLine(
            color = color,
            start = myCenter,
            end = Offset(x = radius, y = myCenter.y),
            strokeWidth = 4.dp.toPx()
        )

        val radians1 = Math.toRadians(angle.angle1.toDouble())
        val sin1 = sin(radians1)
        val x1 = radius * sin1
        val y1 = sqrt(radius.pow(2) - x1.pow(2))
        drawLine(
            color = color,
            start = myCenter,
            end = Offset(x = x1.toFloat(), y = y1.toFloat()),
            strokeWidth = 4.dp.toPx()
        )
    }
}