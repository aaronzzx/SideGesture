package com.aaron.sidegesture.ui.screen.gestureangles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
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
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.ktx.GESTURE_ANGLE_BASE
import com.aaron.sidegesture.ktx.copyNew
import com.aaron.sidegesture.ktx.getArcDegrees
import com.aaron.sidegesture.ktx.getDegree
import com.aaron.sidegesture.ktx.getDegrees
import com.aaron.sidegesture.ktx.getKProperty
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAnglesVM.UiState
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.textStyles
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.TopBar
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

@Composable
fun GestureAnglesScreen(
    onBack: () -> Unit,
    vm: GestureAnglesVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = { }) { uiState ->
        GestureAnglesContent(
            uiState = uiState,
            onBack = onBack,
            onShowResetWarningDialog = vm::showResetWarningDialog,
            onReset = vm::reset,
            onSave = vm::saveSettings,
            onSwitchPosition = vm::switchPosition,
            onAngleChange = vm::updateGestureAngle
        )
    }
}

@Composable
fun GestureAnglesContent(
    uiState: UiState,
    onBack: () -> Unit,
    onShowResetWarningDialog: (Boolean) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onSwitchPosition: (Position) -> Unit,
    onAngleChange: (GestureAngle) -> Unit
) {
    if (uiState.showResetWarningDialog) {
        MyAlertDialog(
            onDismissRequest = {
                onShowResetWarningDialog(false)
            },
            title = stringResource(id = R.string.reset_default_settings_warning),
            text = stringResource(id = R.string.reset_gesture_angles_warning_desc),
            onConfirmClick = onReset
        )
    }
    Scaffold(
        topBar = {
            TopBar(
                modifier = Modifier.zIndex(1f),
                onBack = onBack,
                title = stringResource(id = R.string.gesture_angles),
                actions = {
                    IconButton(onClick = { onShowResetWarningDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = stringResource(id = R.string.reset_settings)
                        )
                    }
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = stringResource(id = R.string.save_settings)
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.testTag("gesture_angle_content_${uiState.position.name}")) {
            AdjustAngle(
                modifier = Modifier
                    .let { thisModifier ->
                        when (uiState.position) {
                            Position.Bottom -> thisModifier.navigationBarsPadding()
                            Position.Top -> thisModifier.padding(
                                top = contentPadding.calculateTopPadding()
                            )
                            else -> thisModifier
                        }
                    }
                    .fillMaxSize()
                    .testTag("gesture_angle_canvas_${uiState.position.name}"),
                angle = uiState.angle,
                onAngleChange = onAngleChange,
                position = uiState.position
            )

            val iconBlock: @Composable BoxScope.(Position) -> Unit = @Composable { position ->
                Icon(
                    modifier = Modifier
                        .align(
                            alignment = when (position) {
                                Position.Left -> Alignment.CenterStart
                                Position.Right -> Alignment.CenterEnd
                                Position.Bottom -> Alignment.BottomCenter
                                Position.Top -> Alignment.TopCenter
                            }
                        )
                        .let {
                            when (position) {
                                Position.Bottom -> it.navigationBarsPadding()
                                Position.Top -> it.padding(
                                    top = contentPadding.calculateTopPadding()
                                )
                                else -> it
                            }
                        }
                        .padding(MaterialTheme.dimensions.listItem.contentGap)
                        .size(MaterialTheme.dimensions.listItem.minimumTouchTarget)
                        .graphicsLayer {
                            rotationZ = when (position) {
                                Position.Left -> 180f
                                Position.Right -> 0f
                                Position.Bottom -> 90f
                                Position.Top -> 270f
                            }
                        }
                        .clipToBackground(
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = MaterialTheme.alpha.gestureAngleGuide
                            ),
                            shape = CircleShape
                        )
                        .onClick {
                            onSwitchPosition(position)
                        }
                        .testTag("gesture_angle_position_${position.name}"),
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Position.entries.fastForEach { position ->
                if (position != uiState.position) {
                    iconBlock(position)
                }
            }
        }
    }
}

@Composable
private fun AdjustAngle(
    onAngleChange: (GestureAngle) -> Unit,
    angle: GestureAngle,
    modifier: Modifier = Modifier,
    position: Position = Position.Left,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val dimensions = MaterialTheme.dimensions.gestureAngles
    val alpha = MaterialTheme.alpha
    val labelStyle = MaterialTheme.textStyles.gestureAngleLabel.copy(
        fontWeight = FontWeight.Bold
    )
    val lineWidth = when (position == Position.Bottom || position == Position.Top) {
        true -> dimensions.topEdgeAnchorRadius
        else -> dimensions.sideEdgeAnchorRadius
    }
    // 触点半径
    val dragHandleRadius = when (position == Position.Bottom || position == Position.Top) {
        true -> dimensions.topEdgeIndicatorRadius
        else -> dimensions.sideEdgeIndicatorRadius
    }
    // 触点所在轨道半圆半径
    var circleRadius by remember { mutableFloatStateOf(0f) }
    // 触点所在轨道圆心
    var circleCenter by remember { mutableStateOf(Offset.Zero) }
    var viewBounds by remember { mutableStateOf(Rect.Zero) }
    // 所有p值代表的角度
    val degrees = remember(angle) { angle.getDegrees() }
    // 所有p值区间弧度代表的角度
    val arcDegrees = remember(angle) { angle.getArcDegrees() }
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    Canvas(
        modifier = modifier.let {
            val density = LocalDensity.current
            // 两个拖拽触点间最少需要维持的夹角p值
            val minGapP by remember(density, dragHandleRadius) {
                derivedStateOf {
                    // 对边
                    val opposite = density.run { dragHandleRadius.toPx() }
                    // 斜边
                    val hypotenuse = circleRadius
                    val sinVal = opposite.toDouble() / hypotenuse
                    val radians = sin(sinVal)
                    Math.toDegrees(radians) / GESTURE_ANGLE_BASE
                }
            }
            val curOnAngleChange by rememberUpdatedState(newValue = onAngleChange)
            val curAngle by rememberUpdatedState(newValue = angle)
            val curPosition by rememberUpdatedState(newValue = position)
            it.pointerInput(dragHandleRadius) {
                var dragOffset = Offset.Zero
                var property: KProperty0<Float>? = null
                detectDragGestures(
                    onDragStart = { offset ->
                        dragOffset = offset
                        // 找到手指按住的那个拖拽触点
                        val p = curAngle.ps.find { p ->
                            val index = curAngle.ps.indexOf(p)
                            val degree = curAngle.getDegree(index)
                            // 计算触点坐标
                            val pOffset = calcDragHandleOffset(curPosition, circleCenter, circleRadius, degree)
                            val bounds = Rect(center = pOffset, radius = dragHandleRadius.toPx())
                            bounds.contains(offset)
                        }
                        property = curAngle.getKProperty(p)
                    },
                    onDrag = onDrag@{ _, dragAmount ->
                        dragOffset += dragAmount
                        // 不在视图范围内
                        if (!viewBounds.contains(dragOffset)) {
                            return@onDrag
                        }
                        val _property = property ?: return@onDrag
                        val opposite = when (curPosition) {
                            Position.Left -> dragOffset.x
                            Position.Right -> circleCenter.x - dragOffset.x
                            Position.Bottom -> circleCenter.y - dragOffset.y
                            Position.Top -> dragOffset.y - circleCenter.y
                        }
                        val neighbor = when (curPosition) {
                            Position.Left, Position.Right -> circleCenter.y - dragOffset.y
                            Position.Bottom, Position.Top -> circleCenter.x - dragOffset.x
                        }
                        val tanVal = opposite / neighbor
                        val radians = atan(tanVal)
                        var newDegree = Math.toDegrees(radians.toDouble())
                        // 如果小于0表示处于下半区
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
        val radius = when (position) {
            Position.Left, Position.Right -> size.minDimension / 2f
            Position.Bottom, Position.Top -> size.minDimension / 4f
        }
        val myCenter = when (position) {
            Position.Left -> center.copy(x = 0f)
            Position.Right -> center.copy(x = size.width)
            Position.Bottom -> center.copy(y = size.height)
            Position.Top -> center.copy(y = 0f)
        }
        circleRadius = radius
        circleCenter = myCenter
        viewBounds = Rect(offset = Offset.Zero, size = size)
        val lineWidthPx = lineWidth.toPx()
        val pointRadiusPx = dragHandleRadius.toPx()

        clipRect {
            drawCircle(
                color = color,
                radius = radius,
                center = myCenter,
                alpha = alpha.gestureAngleGuide
            )
            drawCircle(
                color = color,
                radius = radius,
                center = myCenter,
                alpha = alpha.subtleBorder,
                style = Stroke(width = dimensions.guideStrokeWidth.toPx())
            )
        }

        degrees.fastForEach { degree ->
            val offset = calcDragHandleOffset(position, myCenter, radius, degree)
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

        drawCircle(
            color = color,
            radius = lineWidthPx,
            center = myCenter
        )

        arcDegrees.fastForEachIndexed { index, arcDegree ->
            val degree = degrees.getOrNull(index) ?: GESTURE_ANGLE_BASE
            val (textX, textY) = calcDragHandleOffset(
                position = position,
                circleCenter = myCenter,
                circleRadius = radius + dimensions.arcTouchExpansion.toPx(),
                pDegree = degree - (arcDegree / 2f)
            )

            val displayArcDegree = "${arcDegree.roundToInt()}"
            val hint = when (index) {
                0 -> when (position) {
                    Position.Left, Position.Right -> context.getString(R.string.gesture_to_top)
                    Position.Bottom -> context.getString(R.string.gesture_to_left)
                    Position.Top -> context.getString(R.string.gesture_to_left)
                }
                1 -> when (position) {
                    Position.Left -> context.getString(R.string.gesture_to_right_top)
                    Position.Right -> context.getString(R.string.gesture_to_left_top)
                    Position.Bottom -> context.getString(R.string.gesture_to_top_left)
                    Position.Top -> context.getString(R.string.gesture_to_left_bottom)
                }
                2 -> when (position) {
                    Position.Left -> context.getString(R.string.gesture_to_right)
                    Position.Right -> context.getString(R.string.gesture_to_left)
                    Position.Bottom -> context.getString(R.string.gesture_to_top)
                    Position.Top -> context.getString(R.string.gesture_to_bottom)
                }
                3 -> when (position) {
                    Position.Left -> context.getString(R.string.gesture_to_right_bottom)
                    Position.Right -> context.getString(R.string.gesture_to_left_bottom)
                    Position.Bottom -> context.getString(R.string.gesture_to_top_right)
                    Position.Top -> context.getString(R.string.gesture_to_right_bottom)
                }
                4 -> when (position) {
                    Position.Left, Position.Right -> context.getString(R.string.gesture_to_bottom)
                    Position.Bottom -> context.getString(R.string.gesture_to_right)
                    Position.Top -> context.getString(R.string.gesture_to_right)
                }
                else -> ""
            }
            val displayText = when (position) {
                Position.Left -> "$hint $displayArcDegree"
                Position.Right -> "$displayArcDegree $hint"
                Position.Bottom -> "$displayArcDegree\n$hint"
                Position.Top -> "$hint\n$displayArcDegree"
            }
            val x = when (position) {
                Position.Left, Position.Bottom, Position.Top -> textX - textMeasurer.measure(displayText).size.width / 2f
                Position.Right -> textX - textMeasurer.measure(displayText).size.width
            }.coerceIn(0f, size.width)
            val y = when (position) {
                Position.Left, Position.Right -> textY - textMeasurer.measure(displayText).size.height / 2f
                Position.Bottom -> textY - textMeasurer.measure(displayText).size.height
                Position.Top -> textY
            }.coerceIn(0f, size.height)
            drawText(
                textMeasurer = textMeasurer,
                text = displayText,
                topLeft = Offset(x = x, y = y),
                style = labelStyle.copy(color = color)
            )
        }
    }
}

private fun calcDragHandleOffset(
    position: Position,
    circleCenter: Offset,
    circleRadius: Float,
    pDegree: Float
): Offset {
    val transformedDegree = when (pDegree > 90f) {
        // 当触点位于circle下半区需要转换一下，以保持和上半区角度一致
        true -> GESTURE_ANGLE_BASE - pDegree
        else -> pDegree
    }
    val radians = Math.toRadians(transformedDegree.toDouble())
    val sin = sin(radians)
    // 对边
    val opposite = circleRadius * sin
    // 邻边
    val neighbor = sqrt(circleRadius.pow(2) - opposite.pow(2))
    // 实际x坐标
    val x = when (position) {
        Position.Left -> circleCenter.x + opposite.toFloat()
        Position.Right -> circleCenter.x - opposite.toFloat()
        Position.Bottom, Position.Top -> when (pDegree > 90f) {
            true -> circleCenter.x + neighbor.toFloat()
            else -> circleCenter.x - neighbor.toFloat()
        }
    }
    // 实际y坐标
    val y = when (position) {
        Position.Left, Position.Right -> when (pDegree > 90f) {
            true -> circleCenter.y + neighbor.toFloat()
            else -> circleCenter.y - neighbor.toFloat()
        }
        Position.Bottom -> circleCenter.y - opposite.toFloat()
        Position.Top -> circleCenter.y + opposite.toFloat()
    }
    return Offset(x = x, y = y)
}
