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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
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
import com.aaron.sidegesture.ktx.GESTURE_ANGLE_BASE
import com.aaron.sidegesture.ktx.copyNew
import com.aaron.sidegesture.ktx.getArcDegrees
import com.aaron.sidegesture.ktx.getDegree
import com.aaron.sidegesture.ktx.getDegrees
import com.aaron.sidegesture.ktx.getKProperty
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAnglesVM.UiEvent
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.widget.TopBar
import kotlinx.serialization.Serializable
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.reflect.KProperty0

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
    UDFComponent(
        component = vm.udfComponent,
        onEvent = { event ->
            when (event) {
                UiEvent.Finish -> onBack()
            }
        }
    ) { uiState ->
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
                            vm.reset()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vm.showResetWarningDialog(false)
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
    val lineWidth = 6.dp
    val dragHandleRadius = 20.dp
    var circleRadius by remember { mutableFloatStateOf(0f) }
    var circleCenter by remember { mutableStateOf(Offset.Zero) }
    var viewBounds by remember { mutableStateOf(Rect.Zero) }
    val degrees = remember(angle) { angle.getDegrees() }
    val arcDegrees = remember(angle) { angle.getArcDegrees() }
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    Canvas(
        modifier = modifier.let {
            // 两个拖拽触点间最少需要维持的夹角p值
            val density = LocalDensity.current
            val minGapP by remember(density) {
                derivedStateOf {
                    val x = density.run { dragHandleRadius.toPx() }
                    val y = circleRadius
                    val sinVal = x.toDouble() / y
                    val radians = sin(sinVal)
                    Math.toDegrees(radians) / GESTURE_ANGLE_BASE
                }
            }
            val curOnAngleChange by rememberUpdatedState(newValue = onAngleChange)
            val curAngle by rememberUpdatedState(newValue = angle)
            val curPosition by rememberUpdatedState(newValue = position)
            it.pointerInput(Unit) {
                var dragOffset = Offset.Zero
                var property: KProperty0<Float>? = null
                detectDragGestures(
                    onDragStart = { offset ->
                        dragOffset = offset
                        val p = curAngle.ps.find { p ->
                            val index = curAngle.ps.indexOf(p)
                            val degree = curAngle.getDegree(index)
                            val pOffset = calcXY(curPosition, circleCenter, circleRadius, degree)
                            val bounds = Rect(center = pOffset, radius = dragHandleRadius.toPx())
                            bounds.contains(offset)
                        }
                        property = curAngle.getKProperty(p)
                    },
                    onDrag = onDrag@{ _, dragAmount ->
                        dragOffset += dragAmount
                        if (!viewBounds.contains(dragOffset)) {
                            return@onDrag
                        }
                        val _property = property ?: return@onDrag
                        val x = when (curPosition) {
                            LEFT -> dragOffset.x
                            RIGHT -> circleCenter.x - dragOffset.x
                            else -> error("Unknown position: $curPosition")
                        }
                        val y = circleCenter.y - dragOffset.y
                        val tanVal = x / y
                        val radians = atan(tanVal)
                        var newDegree = Math.toDegrees(radians.toDouble())
                        if (newDegree < 0f) {
                            newDegree = 90f + (newDegree + 90f)
                        }
                        val newDegreeToP = newDegree / GESTURE_ANGLE_BASE
                        val newAngle = curAngle.copyNew(
                            fieldName = _property.name,
                            newP = newDegreeToP.toFloat(),
                            minGapP = minGapP.toFloat()
                        )
                        curOnAngleChange(newAngle)
                    },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        property = null
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        property = null
                    }
                )
            }
        }
    ) {
        val radius = size.minDimension / 2f
        val myCenter = when (position) {
            LEFT -> center.copy(x = 0f)
            else -> center.copy(x = size.width)
        }
        circleRadius = radius
        circleCenter = myCenter
        viewBounds = Rect(offset = Offset.Zero, size = size)

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

        degrees.fastForEach { degree ->
            val lineWidthPx = lineWidth.toPx()
            val pointRadiusPx = dragHandleRadius.toPx()
            val offset = calcXY(position, myCenter, radius, degree)
            drawLine(
                color = color,
                start = myCenter,
                end = offset,
                strokeWidth = lineWidthPx
            )
            drawCircle(
                color = color,
                radius = pointRadiusPx,
                center = offset
            )
        }

        arcDegrees.fastForEachIndexed { index, arcDegree ->
            val degree = degrees.getOrNull(index) ?: GESTURE_ANGLE_BASE
            val (textX, textY) = calcXY(
                position = position,
                center = myCenter,
                radius = radius + 40.dp.toPx(),
                degree = degree - (arcDegree / 2f)
            )
            val displayArcDegree = "${arcDegree.roundToInt()}"
            val hint = when (index) {
                1 -> when (position) {
                    LEFT -> context.getString(R.string.gesture_to_right_top)
                    RIGHT -> context.getString(R.string.gesture_to_left_top)
                    else -> error("Unknown position: $position")
                }
                2 -> when (position) {
                    LEFT -> context.getString(R.string.gesture_to_right)
                    RIGHT -> context.getString(R.string.gesture_to_left)
                    else -> error("Unknown position: $position")
                }
                3 -> when (position) {
                    LEFT -> context.getString(R.string.gesture_to_right_bottom)
                    RIGHT -> context.getString(R.string.gesture_to_left_bottom)
                    else -> error("Unknown position: $position")
                }
                else -> ""
            }
            val displayText = when (position) {
                LEFT -> "$displayArcDegree $hint"
                RIGHT -> "$hint $displayArcDegree"
                else -> error("Unknown position: $position")
            }
            drawText(
                textMeasurer = textMeasurer,
                text = displayText,
                topLeft = Offset(
                    x = when (position) {
                        LEFT -> textX - textMeasurer.measure(displayText).size.width / 2f
                        RIGHT -> textX - textMeasurer.measure(displayText).size.width
                        else -> error("Unknown position: $position")
                    },
                    y = textY - textMeasurer.measure(displayText).size.height / 2f
                ),
                maxLines = 1,
                style = TextStyle.Default.copy(
                    color = when (index) {
                        0, arcDegrees.lastIndex -> inactiveColor
                        else -> color
                    },
                    fontSize = when (index) {
                        0, arcDegrees.lastIndex -> 16.sp
                        else -> 18.sp
                    },
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
    position: Int,
    center: Offset,
    radius: Float,
    degree: Float
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
        LEFT -> center.x + x.toFloat()
        else -> center.x - x.toFloat()
    }
    val finalY = when (degree > 90f) {
        true -> center.y + y.toFloat()
        else -> center.y - y.toFloat()
    }
    return Offset(x = finalX, y = finalY)
}