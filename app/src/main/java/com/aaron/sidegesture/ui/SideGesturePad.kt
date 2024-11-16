package com.aaron.sidegesture.ui

import android.os.SystemClock
import androidx.annotation.Keep
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment.TopLeft
import androidx.compose.ui.AbsoluteAlignment.TopRight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import com.aaron.compose.ktx.toDp
import com.aaron.sidegesture.config.GestureActions
import com.aaron.sidegesture.config.GestureAngles
import com.aaron.sidegesture.ui.GestureButton.Companion.LEFT
import com.aaron.sidegesture.ui.StateHolder.TriggerListener
import com.aaron.sidegesture.ui.TriggerDirection.Center
import com.aaron.sidegesture.ui.TriggerDirection.Down
import com.aaron.sidegesture.ui.TriggerDirection.Up
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/15
 */

@Composable
fun SideGesturePad(
    onAction: (Int) -> Unit,
    buttons: List<GestureButton>,
    modifier: Modifier = Modifier,
    angles: GestureAngles = GestureAngles(),
    animationStyle: AnimationStyle = AnimationStyle.Wave()
) {
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val coroutineScope = rememberCoroutineScope()
    var rootSize by remember { mutableStateOf(Size.Zero) }
    var obj: Any? by remember { mutableStateOf(null) }
    Box(
        modifier = modifier.onSizeChanged {
            rootSize = it.toSize()
        }
    ) {
        buttons.fastForEach { button ->
            key(button) {
                val triggerListener = remember {
                    object : TriggerListener {
                        override fun onAction(action: Int) {
                            curOnAction(action)
                        }
                    }
                }
                val stateHolder = rememberStateHolder(
                    coroutineScope = coroutineScope,
                    rootSize = rootSize,
                    button = button,
                    angles = angles,
                    listener = triggerListener
                )
                val defaultIcon = rememberVectorPainter(
                    image = when (button.position) {
                        LEFT -> Icons.Default.ArrowForward
                        else -> Icons.Default.ArrowBack
                    }
                )
                Box(
                    modifier = Modifier
                        .align(TopLeft.takeIf { button.position == LEFT } ?: TopRight)
                        .drawWithCache {
                            if (stateHolder.fingerX.value.isNaN()) {
                                return@drawWithCache onDrawBehind {}
                            }
                            animationStyle.run {
                                draw(stateHolder, defaultIcon)
                            }
                        }
                        .offset {
                            IntOffset(x = 0, y = (rootSize.height * button.start).toInt())
                        }
                        .width(button.width.toDp())
                        .fillMaxHeight(button.fraction)
                        .background(color = Color.Blue.copy(alpha = 0.1f))
                        .pointerInput(Unit) {
                            var intercept = false
                            detectDragGestures(
                                onDragStart = onDragStart@{ offset ->
                                    if (obj != null && obj != stateHolder) {
                                        return@onDragStart
                                    }
                                    obj = stateHolder
                                    stateHolder.onStartDrag(offset)
                                },
                                onDrag = onDrag@{ _, dragAmount ->
                                    if (obj != stateHolder || intercept) {
                                        return@onDrag
                                    }
                                    if (stateHolder.onDrag(dragAmount)) {
                                        // 触发长按动作，拦截当前拖拽
                                        intercept = true
                                        stateHolder.onDragCancel()
                                    }
                                },
                                onDragEnd = onDragEnd@{
                                    if (obj != stateHolder) {
                                        return@onDragEnd
                                    }
                                    if (!intercept) {
                                        stateHolder.onDragEnd()
                                    }
                                    obj = null
                                    intercept = false
                                },
                                onDragCancel = onDragCancel@{
                                    obj = null
                                    intercept = false
                                    stateHolder.onDragCancel()
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun rememberStateHolder(
    coroutineScope: CoroutineScope,
    rootSize: Size,
    button: GestureButton,
    angles: GestureAngles,
    listener: TriggerListener
): StateHolder = remember(coroutineScope, rootSize, button, angles, listener) {
    StateHolder(coroutineScope, rootSize, button, angles, listener)
}

class StateHolder(
    val coroutineScope: CoroutineScope,
    val rootSize: Size,
    val button: GestureButton,
    val angles: GestureAngles,
    private val listener: TriggerListener
) {
    val originY = Animatable(Float.NaN)
    val fingerX = Animatable(Float.NaN)
    val fingerY = Animatable(Float.NaN)

    var triggerDirection = Center

    private var origin = Offset.Zero
    private var finger = Offset.Zero

    private var longPressFirstTriggerMs = 0L

    private val animationSpec = spring<Float>(stiffness = 3000f)

    fun onStartDrag(offset: Offset) {
        origin = offset
        finger = offset
        coroutineScope.launch {
            val initialY = rootSize.height * button.start
            val curY = initialY + offset.y
            originY.snapTo(curY)
            fingerY.snapTo(curY)
            fingerX.snapTo(0f)
        }
    }

    /**
     * @return 是否触发长距离动作
     */
    fun onDrag(dragAmount: Offset): Boolean {
        finger += dragAmount
        triggerDirection = calcDirection()
        coroutineScope.launch {
            val fingerX = fingerX
            val fingerY = fingerY
            fingerX.snapTo(fingerX.value + dragAmount.x)
            fingerY.snapTo(fingerY.value + dragAmount.y)
        }

        val button = button
        val longPressDelayMs = button.longPressTriggerDelayMs
        if (canDistanceTrigger(true)) {
            val timeMs = SystemClock.uptimeMillis()
            if (longPressFirstTriggerMs == 0L) {
                longPressFirstTriggerMs = timeMs
            } else if (!button.longPressNeedFingerUp &&
                timeMs - longPressFirstTriggerMs >= longPressDelayMs
            ) {
                listener.onAction(button.longPressActions.select(triggerDirection))
                return true
            }
        }
        return false
    }

    fun onDragEnd() {
        val button = button
        val listener = listener
        val longPressDelayMs = button.longPressTriggerDelayMs
        val triggerDirection = triggerDirection
        if (button.longPressNeedFingerUp &&
            canDistanceTrigger(true) &&
            SystemClock.uptimeMillis() - longPressFirstTriggerMs >= longPressDelayMs
        ) {
            listener.onAction(button.longPressActions.select(triggerDirection))
        } else if (canDistanceTrigger(false)) {
            listener.onAction(button.pressActions.select(triggerDirection))
        }
        reset()
    }

    fun onDragCancel() {
        reset()
    }

    private fun reset() {
        origin = Offset.Zero
        finger = Offset.Zero
        longPressFirstTriggerMs = 0L
        coroutineScope.launch {
            val originY = originY
            val fingerX = fingerX
            val fingerY = fingerY
            launch {
                fingerY.animateTo(originY.value, animationSpec)
            }
            fingerX.animateTo(0f, animationSpec)
            originY.snapTo(Float.NaN)
            fingerX.snapTo(Float.NaN)
            fingerY.snapTo(Float.NaN)
        }
    }

    /**
     * 手指划过的距离是否足够触发动作，上和下的动作需要按斜线距离计算
     */
    private fun canDistanceTrigger(isLongPress: Boolean): Boolean {
        val button = button
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x
        val fingerY = finger.y
        val triggerDirection = triggerDirection
        if (triggerDirection == Center) {
            val distance = abs(fingerX - originX)
            if (isLongPress) {
                return distance >= button.longPressTriggerDistance
            }
            return distance >= button.pressTriggerDistance
        } else if (triggerDirection == Up || triggerDirection == Down) {
            val edge1 = abs(fingerX - originX)
            val edge2 = abs(fingerY - originY)
            val edge3 = hypot(edge1, edge2)
            if (isLongPress) {
                return edge3 >= button.longPressTriggerDistance
            }
            return edge3 >= button.pressTriggerDistance
        }
        return false
    }

    private fun calcDirection(): TriggerDirection {
        val origin = origin
        val finger = finger
        val x = when (button.position == LEFT) {
            true -> finger.x
            else -> origin.x - finger.x
        }
        val tanVal = x / abs(finger.y - origin.y)
        val radians = atan(tanVal)
        val degree = if (finger.y < origin.y) {
            // 第一象限
            Math.toDegrees(radians.toDouble())
        } else {
            // 第四象限
            180f - Math.toDegrees(radians.toDouble())
        }
        return angles.getTriggerDirection(degree.toFloat())
    }

    interface TriggerListener {

        fun onAction(action: Int)
    }
}

@Keep
data class GestureButton(
    val position: Int = LEFT,
    val start: Float = 0f,
    val end: Float = 1f,
    val width: Int = ConvertUtils.dp2px(16f),
    val pressActions: GestureActions = GestureActions(),
    val longPressActions: GestureActions = GestureActions(),
    val pressTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longPressTriggerDistance: Int = ConvertUtils.dp2px(80f),
    val longPressTriggerDelayMs: Long = 100L,
    val longPressNeedFingerUp: Boolean = false
) {
    companion object {
        const val LEFT = 1
        const val RIGHT = 2
    }

    val fraction: Float get() = end - start
}

enum class TriggerDirection {

    Up, Center, Down
}

sealed interface AnimationStyle {

    fun CacheDrawScope.draw(stateHolder: StateHolder, defaultIcon: Painter): DrawResult

    @Keep
    data class Wave(
        val backgroundColor: Int = android.graphics.Color.BLACK,
        val strokeColor: Int = android.graphics.Color.TRANSPARENT,
        val strokeWidth: Int = 0,
        val iconColor: Int = android.graphics.Color.argb(0.8f, 1f, 1f, 1f),
        val iconUriString: String? = null
    ) : AnimationStyle {

        override fun CacheDrawScope.draw(
            stateHolder: StateHolder,
            defaultIcon: Painter
        ): DrawResult {
            val rootSize = stateHolder.rootSize
            val button = stateHolder.button
            val bezierPath = Path()
            // 贝塞尔间距
            val bezierSpacing = 60.dp.toPx()
            // 贝塞尔的最大宽度
            val bezierMaxWidth = 40.dp.toPx()
            // 贝塞尔长度的一半
            val halfBezierLength = 100.dp.toPx()
            // 贝塞尔变形限制
            val offsetYCoerce = 50.dp.toPx()

            return onDrawWithContent {
                drawContent()
                val originY = stateHolder.originY.value
                val fingerX = stateHolder.fingerX.value
                val fingerY = stateHolder.fingerY.value

                // 动画y轴偏移值
                val offsetY = (originY - fingerY).coerceIn(-offsetYCoerce, offsetYCoerce)
                // 能完整显示整个贝塞尔并且留有间距
                val safeOriginY = (originY - bezierSpacing).coerceIn(
                    minimumValue = halfBezierLength + bezierSpacing,
                    maximumValue = rootSize.height - halfBezierLength - bezierSpacing
                )
                bezierPath.also {
                    it.reset()
                    val moveToX = when (button.position == LEFT) {
                        true -> 0f
                        else -> size.width
                    }
                    val safeFingerX = when (button.position == LEFT) {
                        true -> fingerX.coerceAtMost(bezierMaxWidth)
                        else -> (size.width + fingerX).coerceAtLeast(size.width - bezierMaxWidth)
                    }
                    it.moveTo(moveToX, safeOriginY - halfBezierLength)
                    it.cubicTo(
                        x1 = when (button.position == LEFT) {
                            true -> -1f
                            else -> size.width + 1f
                        },
                        y1 = safeOriginY - halfBezierLength / 2.5f - offsetY,
                        x2 = safeFingerX,
                        y2 = safeOriginY - halfBezierLength / 2.5f - offsetY,
                        x3 = safeFingerX,
                        y3 = safeOriginY - offsetY
                    )
                    it.cubicTo(
                        x1 = safeFingerX,
                        y1 = safeOriginY + halfBezierLength / 2.5f - offsetY,
                        x2 = when (button.position == LEFT) {
                            true -> 0f
                            else -> size.width
                        },
                        y2 = safeOriginY + halfBezierLength / 2.5f - offsetY,
                        x3 = when (button.position == LEFT) {
                            true -> -1f
                            else -> size.width + 1f
                        },
                        y3 = safeOriginY + halfBezierLength
                    )

                    if (strokeWidth > 0) {
                        val offset = when (button.position == LEFT) {
                            true -> Offset(-strokeWidth.toFloat(), 0f)
                            else -> Offset(strokeWidth.toFloat(), 0f)
                        }
                        it.translate(offset)
                    }
                }
                // 绘制背景
                drawPath(path = bezierPath, color = Color(backgroundColor))
                if (strokeWidth > 0) {
                    // 绘制轮廓
                    drawPath(
                        path = bezierPath,
                        color = Color(strokeColor),
                        style = Stroke(strokeWidth.toFloat())
                    )
                }

                val bezierBounds = bezierPath.getBounds().translate(Offset(0f, -offsetY))
                if (iconUriString == null) {
                    // 默认图标
                    defaultIcon.run {
                        val degree = when (stateHolder.triggerDirection) {
                            Up -> if (button.position == LEFT) -45f else 45f
                            Center -> 0f
                            Down -> if (button.position == LEFT) 45f else -45f
                        }
                        rotate(degree, pivot = bezierBounds.center) {
                            val radius = bezierBounds.width * 0.6f
                            val left = when (button.position) {
                                LEFT -> bezierBounds.width * 0.2f - strokeWidth
                                else -> size.width - bezierBounds.width * 0.8f + strokeWidth
                            }
                            val top = bezierBounds.top + bezierBounds.height / 2f - radius / 2f
                            translate(left = left, top = top) {
                                draw(
                                    size = Size(radius, radius),
                                    colorFilter = ColorFilter.tint(Color(iconColor))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}