package com.aaron.sidegesture.ui.screen.gestureangles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.onClick
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
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.widget.TopBar
import com.blankj.utilcode.util.NumberUtils
import kotlinx.serialization.Serializable
import kotlin.math.atan
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
        Box {
            TopBar(
                modifier = Modifier.zIndex(1f),
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
                onAngleChange = {
                    vm.updateGestureAngle(it)
                },
                position = uiState.position
            )
            Icon(
                modifier = Modifier
                    .align(
                        alignment = when (uiState.position) {
                            LEFT -> Alignment.CenterEnd
                            RIGHT -> Alignment.CenterStart
                            else -> error("Unknown position: ${uiState.position}")
                        }
                    )
                    .padding(horizontal = ItemPadding)
                    .size(MinInteractiveSize)
                    .graphicsLayer {
                        rotationZ = when (uiState.position) {
                            LEFT -> 0f
                            RIGHT -> 180f
                            else -> error("Unknown position: ${uiState.position}")
                        }
                    }
                    .clipToBackground(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .onClick(enableRipple = false) {
                        val newPosition = when (uiState.position) {
                            LEFT -> RIGHT
                            RIGHT -> LEFT
                            else -> error("Unknown position: ${uiState.position}")
                        }
                        vm.switchPosition(newPosition)
                    },
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AdjustAngle(
    onAngleChange: (GestureAngle) -> Unit,
    angle: GestureAngle,
    modifier: Modifier = Modifier,
    position: Int = LEFT,
    color: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
) {
    require(position == LEFT || position == RIGHT) {
        "Unknown position: $position"
    }
    val curOnAngleChange by rememberUpdatedState(newValue = onAngleChange)
    var centerOffset by remember { mutableStateOf(Offset.Unspecified) }
    var dragOffset by remember { mutableStateOf(Offset.Unspecified) }
    var p1 by remember { mutableStateOf(Rect.Zero) }
    var p2 by remember { mutableStateOf(Rect.Zero) }
    var p3 by remember { mutableStateOf(Rect.Zero) }
    var p4 by remember { mutableStateOf(Rect.Zero) }
    var isP1Drag by remember { mutableStateOf(false) }
    var isP2Drag by remember { mutableStateOf(false) }
    var isP3Drag by remember { mutableStateOf(false) }
    var isP4Drag by remember { mutableStateOf(false) }
    val degrees = remember(angle) {
        listOf(angle.degree1, angle.degree2, angle.degree3, angle.degree4)
    }
    val arcDegrees = remember(angle) {
        listOf(angle.arcDegree1, angle.arcDegree2, angle.arcDegree3, angle.arcDegree4, angle.arcDegree5)
    }
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    dragOffset = if (p1.contains(offset)) {
                        isP1Drag = true
                        offset
                    } else if (p2.contains(offset)) {
                        isP2Drag = true
                        offset
                    } else if (p3.contains(offset)) {
                        isP3Drag = true
                        offset
                    } else if (p4.contains(offset)) {
                        isP4Drag = true
                        offset
                    } else {
                        Offset.Unspecified
                    }
                },
                onDrag = { _, dragAmount ->
                    if (dragOffset != Offset.Unspecified &&
                        centerOffset != Offset.Unspecified
                    ) {
                        dragOffset += dragAmount
                        val x = dragOffset.x
                        val y = centerOffset.y - dragOffset.y
                        val tanVal = x / y
                        val radians = atan(tanVal)
                        var degree = Math.toDegrees(radians.toDouble())
                        if (degree < 0f) {
                            degree = 90f + (degree + 90f)
                        }
                        if (isP1Drag) {
                            val degreeAmount = degree - angle.degree1
                            val newDegree = angle.degree1 + degreeAmount
                            val fraction = newDegree / 180f
                            curOnAngleChange(angle.copy(p1 = fraction.toFloat()))
                        } else if (isP2Drag) {
                            val degreeAmount = degree - angle.degree2
                            val newDegree = angle.degree2 + degreeAmount
                            val fraction = newDegree / 180f
                            curOnAngleChange(angle.copy(p2 = fraction.toFloat()))
                        } else if (isP3Drag) {
                            val degreeAmount = degree - angle.degree3
                            val newDegree = angle.degree3 + degreeAmount
                            val fraction = newDegree / 180f
                            curOnAngleChange(angle.copy(p3 = fraction.toFloat()))
                        } else if (isP4Drag) {
                            val degreeAmount = degree - angle.degree4
                            val newDegree = angle.degree4 + degreeAmount
                            val fraction = newDegree / 180f
                            curOnAngleChange(angle.copy(p4 = fraction.toFloat()))
                        }
                    }
                },
                onDragEnd = {
                    dragOffset = Offset.Unspecified
                    isP1Drag = false
                    isP2Drag = false
                    isP3Drag = false
                    isP4Drag = false
                },
                onDragCancel = {
                    dragOffset = Offset.Unspecified
                    isP1Drag = false
                    isP2Drag = false
                    isP3Drag = false
                    isP4Drag = false
                }
            )
        }
    ) {
        val radius = size.minDimension / 2f
        val myCenter = when (position) {
            LEFT -> center.copy(x = 0f)
            else -> center.copy(x = size.width)
        }
        centerOffset = myCenter
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

        degrees.fastForEachIndexed { index, degree ->
            val (x, y) = calcXY(myCenter, degree, position, radius, size)
            val lineWidth = 6.dp.toPx()
            val pointRadius = 20.dp.toPx()
            val bounds = Rect(center = Offset(x, y), radius = pointRadius)
            when (index) {
                0 -> p1 = bounds
                1 -> p2 = bounds
                2 -> p3 = bounds
                3 -> p4 = bounds
                else -> error("Unknown index: $index")
            }
            drawLine(
                color = color,
                start = myCenter,
                end = Offset(x = x, y = y),
                strokeWidth = lineWidth
            )
            drawCircle(
                color = color,
                radius = pointRadius,
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
                    color = when (index) {
                        0, arcDegrees.lastIndex -> inactiveColor
                        else -> color
                    },
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