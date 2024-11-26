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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.arcDegree1
import com.aaron.sidegesture.ktx.arcDegree2
import com.aaron.sidegesture.ktx.arcDegree3
import com.aaron.sidegesture.ktx.arcDegree4
import com.aaron.sidegesture.ktx.arcDegree5
import com.aaron.sidegesture.ktx.degree1
import com.aaron.sidegesture.ktx.degree2
import com.aaron.sidegesture.ktx.degree3
import com.aaron.sidegesture.ktx.degree4
import com.aaron.sidegesture.ui.widget.TopBar
import com.blankj.utilcode.util.NumberUtils
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
    position: Int = LEFT,
    color: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    disabledColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    require(position == LEFT || position == RIGHT) {
        "Unknown position: $position"
    }
    val degrees = remember(angle) {
        listOf(angle.degree1, angle.degree2, angle.degree3, angle.degree4)
    }
    val arcDegrees = remember(angle) {
        listOf(angle.arcDegree1, angle.arcDegree2, angle.arcDegree3, angle.arcDegree4, angle.arcDegree5)
    }
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val myCenter = when (position) {
            LEFT -> center.copy(x = 0f)
            else -> center.copy(x = size.width)
        }
        clipRect {
            drawCircle(
                color = color,
                radius = radius,
                center = myCenter,
                alpha = 0.1f
            )
            /*translate(left = -radius) {
                val topLeft = myCenter.copy(y = myCenter.y - radius)
                val arcSize = Size(size.minDimension, size.minDimension)
                drawArc(
                    color = disabledColor,
                    startAngle = -90f,
                    sweepAngle = angle.arcDegree1,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
                drawArc(
                    color = disabledColor,
                    startAngle = angle.degree4 - 90f,
                    sweepAngle = angle.arcDegree4,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
            }*/
            drawCircle(
                color = color,
                radius = radius,
                center = myCenter,
                alpha = 0.35f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        degrees.fastForEach { degree ->
            val (x, y) = calcXY(myCenter, degree, position, radius, size)
            drawLine(
                color = color,
                start = myCenter,
                end = Offset(x = x, y = y),
                strokeWidth = 6.dp.toPx()
            )
            drawCircle(
                color = color,
                radius = 20.dp.toPx(),
                center = Offset(x = x, y = y)
            )
        }

        arcDegrees.fastForEachIndexed { index, arcDegree ->
            val degree = degrees.getOrNull(index) ?: 180f
            val (textX, textY) = calcXY(
                myCenter = myCenter,
                degree = degree - (arcDegree / 2f),
                position = position,
                radius = radius + 40.dp.toPx(),
                size = size
            )
            val displayArcDegree = NumberUtils.format(arcDegree, 1)
            drawText(
                textMeasurer = textMeasurer,
                text = displayArcDegree,
                topLeft = Offset(
                    x = textX - textMeasurer.measure(displayArcDegree).size.width / 2f,
                    y = textY - textMeasurer.measure(displayArcDegree).size.height / 2f
                ),
                style = TextStyle.Default.copy(
                    fontSize = 16.sp,
                    fontWeight = when (index) {
                        0, arcDegrees.lastIndex -> FontWeight.Normal
                        else -> FontWeight.Bold
                    }
                )
            )
        }
    }
}

private fun calcXY(
    myCenter: Offset,
    degree: Float,
    position: Int,
    radius: Float,
    size: Size
): Offset {
    val modifiedDegree = when (degree > 90f) {
        true -> 180f - degree
        else -> degree
    }
    val radians = Math.toRadians(modifiedDegree.toDouble())
    val sin = sin(radians)
    val x = radius * sin
    val y = sqrt(radius.pow(2) - x.pow(2))
    val finalX = when (position) {
        LEFT -> myCenter.x + x.toFloat()
        else -> size.width - x.toFloat()
    }
    val finalY = when (degree > 90f) {
        true -> myCenter.y + y.toFloat()
        else -> myCenter.y - y.toFloat()
    }
    return Offset(x = finalX, y = finalY)
}