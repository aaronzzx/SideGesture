package com.aaron.sidegesture.ui

import android.os.SystemClock
import androidx.annotation.Keep
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment.TopLeft
import androidx.compose.ui.AbsoluteAlignment.TopRight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toDp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.config.GestureAction
import com.aaron.sidegesture.config.GestureActions
import com.aaron.sidegesture.config.GestureAngles
import com.aaron.sidegesture.config.Vibrators
import com.aaron.sidegesture.ui.GestureButton.Companion.LEFT
import com.aaron.sidegesture.ui.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ui.TriggerDirection.Center
import com.aaron.sidegesture.ui.TriggerDirection.Down
import com.aaron.sidegesture.ui.TriggerDirection.Up
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

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
    val actionListener = remember {
        object : ActionListener {
            override fun onTrigger(action: Int) {
                curOnAction(action)
            }
        }
    }
    Box(
        modifier = modifier.onSizeChanged {
            rootSize = it.toSize()
        }
    ) {
        buttons.fastForEach { button ->
            key(button) {
                val multipleActionHandler = rememberMultipleActionHandler(
                    rootSize = rootSize,
                    button = button,
                    listener = actionListener
                )
                val stateHolder = rememberStateHolder(
                    coroutineScope = coroutineScope,
                    rootSize = rootSize,
                    button = button,
                    angles = angles,
                    listener = actionListener
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
                            animationStyle.draw(this, stateHolder, defaultIcon)
                        }
                        .offset {
                            IntOffset(x = 0, y = (rootSize.height * button.start).toInt())
                        }
                        .width(button.width.toDp())
                        .fillMaxHeight(button.fraction)
                        .background(color = Color.Blue.copy(alpha = 0.1f))
                        .pointerInput(multipleActionHandler, stateHolder) {
                            var isCancelGesture = false
                            var isMultipleAction = false
                            detectDragGestures(
                                onDragStart = onDragStart@{ offset ->
                                    if (obj != null && obj != stateHolder) {
                                        return@onDragStart
                                    }
                                    multipleActionHandler.onDragStart(
                                        offset = offset,
                                        buttonSize = size.toSize(),
                                        position = button.position
                                    )
                                    obj = stateHolder
                                    stateHolder.onStartDrag(offset)
                                },
                                onDrag = onDrag@{ _, dragAmount ->
                                    if (obj != stateHolder || isCancelGesture) {
                                        return@onDrag
                                    }
                                    multipleActionHandler.onDrag(dragAmount)
                                    if (isMultipleAction) {
                                        // 已经触发长动作
                                        return@onDrag
                                    }
                                    val actions = stateHolder.onDrag(dragAmount)
                                    if (actions != null) {
                                        if (actions.size > 1) {
                                            // 触发长动作，拦截当前拖拽
                                            isMultipleAction = true
                                            stateHolder.onDragCancel()
                                            multipleActionHandler.onExpanded(
                                                button.position,
                                                actions
                                            )
                                        } else if (actions.size == 1) {
                                            isCancelGesture = true
                                            stateHolder.onDragCancel()
                                        }
                                    } else {
                                        isCancelGesture = true
                                        stateHolder.onDragCancel()
                                    }
                                },
                                onDragEnd = onDragEnd@{
                                    if (obj != stateHolder) {
                                        return@onDragEnd
                                    }
                                    if (isMultipleAction) {
                                        multipleActionHandler.onDragEnd()
                                    } else if (!isCancelGesture) {
                                        stateHolder.onDragEnd()
                                    }
                                    obj = null
                                    isCancelGesture = false
                                    isMultipleAction = false
                                },
                                onDragCancel = onDragCancel@{
                                    obj = null
                                    isCancelGesture = false
                                    isMultipleAction = false
                                    stateHolder.onDragCancel()
                                    multipleActionHandler.onDragCancel()
                                }
                            )
                        }
                )
                multipleActionHandler.ActionPanel()
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
    listener: ActionListener
): StateHolder = remember(coroutineScope, rootSize, button, angles, listener) {
    StateHolder(coroutineScope, rootSize, button, angles, listener)
}

class StateHolder(
    val coroutineScope: CoroutineScope,
    val rootSize: Size,
    val button: GestureButton,
    val angles: GestureAngles,
    private val listener: ActionListener
) {

    val originY = Animatable(Float.NaN)
    val fingerX = Animatable(Float.NaN)
    val fingerY = Animatable(Float.NaN)

    var triggerDirection = Center

    var origin = Offset.Zero
    var finger = Offset.Zero

    private var longPressFirstTriggerMs = 0L
    private var longPressTriggerFlags = false
    private var pressTriggerFlags = false

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
     * @return 返回null表示不识别任何手势，空列表表示还没触发动作，单列表表示触发一个动作，长列表表示触发长动作
     */
    fun onDrag(dragAmount: Offset): List<Int>? {
        finger += dragAmount
        // 没触发方向，这一轮不再识别手势
        triggerDirection = calcDirection() ?: return null
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
                if (button.vibrators.forLongPress && !longPressTriggerFlags) {
                    longPressTriggerFlags = true
                    button.vibrators.vibrate()
                }
                val listener = listener
                val actions = button.longPressAction.select(triggerDirection)
                if (actions.size > 1) {
                    return actions
                } else if (actions.size == 1) {
                    val action = actions.first()
                    if (action != GestureActions.NONE) {
                        listener.onTrigger(action)
                        return listOf(action)
                    }
                }
            }
        } else if (canDistanceTrigger(false)) {
            if (button.vibrators.forPress && !pressTriggerFlags) {
                pressTriggerFlags = true
                button.vibrators.vibrate()
            }
        } else {
            longPressTriggerFlags = false
            pressTriggerFlags = false
        }
        return emptyList()
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
            val actions = button.longPressAction.select(triggerDirection)
            if (actions.isNotEmpty()) {
                val action = actions.first()
                if (action != GestureActions.NONE) {
                    listener.onTrigger(action)
                }
            }
        } else if (canDistanceTrigger(false)) {
            val action = button.pressAction.select(triggerDirection)
            if (action != GestureActions.NONE) {
                listener.onTrigger(action)
            }
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
        val pressAction = button.pressAction
        val longPressAction = button.longPressAction
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x
        val fingerY = finger.y
        val triggerDirection = triggerDirection
        if (triggerDirection == Center) {
            val distance = abs(fingerX - originX)
            if (isLongPress) {
                return distance >= button.longPressTriggerDistance &&
                        longPressAction.center.isNotEmpty()
            }
            return distance >= button.pressTriggerDistance &&
                    pressAction.center != GestureActions.NONE
        } else if (triggerDirection == Up || triggerDirection == Down) {
            val edge1 = abs(fingerX - originX)
            val edge2 = abs(fingerY - originY)
            val edge3 = hypot(edge1, edge2)
            if (isLongPress) {
                val canTrigger = edge3 >= button.longPressTriggerDistance
                if (triggerDirection == Up) {
                    return canTrigger && longPressAction.up.isNotEmpty()
                }
                return canTrigger && longPressAction.down.isNotEmpty()
            }
            val canTrigger = edge3 >= button.pressTriggerDistance
            if (triggerDirection == Up) {
                return canTrigger && pressAction.up != GestureActions.NONE
            }
            return canTrigger && pressAction.down != GestureActions.NONE
        }
        return false
    }

    private fun calcDirection(): TriggerDirection? {
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
}

@Composable
private fun rememberMultipleActionHandler(
    rootSize: Size,
    button: GestureButton,
    listener: ActionListener
): MultipleActionHandler = remember(rootSize, button, listener) {
    MultipleActionHandler(rootSize, button, listener)
}

class MultipleActionHandler(
    private val rootSize: Size,
    private val button: GestureButton,
    private val listener: ActionListener
) {

    private var origin: Origin by mutableStateOf(Origin())
    private var finger: Offset by mutableStateOf(Offset.Zero)
    private var actions: List<Int> by mutableStateOf(emptyList())
    private var pendingActions: MutableMap<Int, Int?> = mutableMapOf()

    @Composable
    fun ActionPanel() {
        val actions by rememberUpdatedState(newValue = actions)
        val origin by rememberUpdatedState(newValue = origin)
        val finger by rememberUpdatedState(newValue = finger)
        val itemSize = 60.dp
        val hypot = itemSize.toPx() * 2f
        if (!origin.isEmpty && finger != Offset.Zero && actions.isNotEmpty()) {
            Box(
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        val offsetX = when (origin.position) {
                            LEFT -> itemSize.toPx() / 3
                            else -> -itemSize.toPx() / 3
                        }
                        val offset = itemSize.toPx() / 2
                        translationX = origin.offset.x - offset - offsetX
                        translationY = origin.offset.y - offset
                    }
                    .size(itemSize)
            ) {
                actions.fastForEachIndexed { index, action ->
                    key(index) {
                        val animX = remember { Animatable(0f) }
                        val animY = remember { Animatable(0f) }
                        val animScale = remember { Animatable(0f) }
                        LaunchedEffect(key1 = Unit) {
                            val avgAngDeg = 35.0
                            val totalAngDeg = avgAngDeg * (actions.size - 1)
                            val angDeg = 90.0 - totalAngDeg / 2.0 + avgAngDeg * index
                            val radians = Math.toRadians(angDeg)
                            val dy = hypot * cos(radians)
                            val dx = sqrt(hypot.pow(2) - dy.pow(2)).let { value ->
                                if (origin.position == LEFT) value else -value
                            }
                            launch {
                                animX.animateTo(dx.toFloat())
                            }
                            launch {
                                animY.animateTo(dy.toFloat())
                            }
                            launch {
                                animScale.animateTo(1f)
                            }
                        }

                        var originBounds by remember { mutableStateOf(Rect.Zero) }
                        LaunchedEffect(animX, animY, animScale) {
                            snapshotFlow { finger }
                                .filter {
                                    animScale.value >= 1f
                                }
                                .collect {
                                    val offset = Offset(x = animX.value, y = animY.value)
                                    val transFinger = it - offset
                                    val pendingActions = pendingActions
                                    if (originBounds.contains(transFinger)) {
                                        val cache = pendingActions[index]
                                        if (cache != action) {
                                            animScale.animateTo(1.15f)
                                            pendingActions[index] = action
                                            val button = button
                                            if (button.vibrators.forActionPanel) {
                                                button.vibrators.vibrate()
                                            }
                                        }
                                    } else {
                                        val cache = pendingActions[index]
                                        if (cache != null) {
                                            animScale.animateTo(1f)
                                            pendingActions[index] = null
                                        }
                                    }
                                }
                        }
                        Image(
                            modifier = Modifier
                                .onGloballyPositioned {
                                    originBounds = it.boundsInRoot()
                                }
                                .graphicsLayer {
                                    scaleX = animScale.value
                                    scaleY = animScale.value
                                    translationX = animX.value
                                    translationY = animY.value
                                }
                                .matchParentSize()
                                .shadow(elevation = 16.dp, shape = CircleShape)
                                .clipToBackground(color = Color.White, shape = CircleShape),
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            contentScale = ContentScale.Inside
                        )
                    }
                }
            }
        }
    }

    fun onDragStart(offset: Offset, buttonSize: Size, position: Int) {
        origin = Origin()
        if (position == LEFT) {
            finger = offset
        } else {
            val offsetX = rootSize.width - buttonSize.width
            finger = offset.copy(x = offset.x + offsetX)
        }
    }

    fun onDrag(dragAmount: Offset) {
        finger += dragAmount
    }

    fun onExpanded(position: Int, actions: List<Int>) {
        this.actions = actions
        if (origin.isEmpty) {
            origin = Origin(finger, position)
        }
    }

    fun onDragEnd() {
        val pendingActions = pendingActions
        val action = pendingActions.values.find { it != null }
        if (action != null && action != GestureActions.NONE) {
            listener.onTrigger(action)
        }
        reset()
    }

    fun onDragCancel() {
        reset()
    }

    private fun reset() {
        finger = Offset.Zero
        actions = emptyList()
        pendingActions.clear()
    }

    private data class Origin(
        val offset: Offset = Offset.Zero,
        val position: Int = -1
    ) {
        val isEmpty: Boolean get() = offset == Offset.Zero || position == -1
    }
}

interface ActionListener {

    fun onTrigger(action: Int)
}

@Keep
data class GestureButton(
    val position: Int,
    val start: Float = 0f,
    val end: Float = 1f,
    val width: Int = ConvertUtils.dp2px(20f),
    val pressAction: GestureAction<Int> = GestureAction.Single(),
    val longPressAction: GestureAction<List<Int>> = GestureAction.Multiple(),
    val pressTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longPressTriggerDistance: Int = ConvertUtils.dp2px(100f),
    val longPressTriggerDelayMs: Long = 100L,
    val longPressNeedFingerUp: Boolean = false,
    val vibrators: Vibrators = Vibrators()
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

    fun draw(drawScope: CacheDrawScope, stateHolder: StateHolder, defaultIcon: Painter): DrawResult

    @Keep
    data class Wave(
        val backgroundColor: Int = android.graphics.Color.BLACK,
        val strokeColor: Int = android.graphics.Color.TRANSPARENT,
        val strokeWidth: Int = 0,
        val iconColor: Int = android.graphics.Color.argb(0.8f, 1f, 1f, 1f),
        val iconUriString: String? = null
    ) : AnimationStyle {

        override fun draw(
            drawScope: CacheDrawScope,
            stateHolder: StateHolder,
            defaultIcon: Painter
        ): DrawResult = drawScope.run {
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

            onDrawWithContent {
                drawContent()

                val originY = stateHolder.originY.value
                val fingerX = stateHolder.fingerX.value
                val fingerY = stateHolder.fingerY.value
                if (originY.isNaN() || fingerX.isNaN() || fingerY.isNaN()) {
                    return@onDrawWithContent
                }
                if (button.position == LEFT && fingerX < 0f) {
                    return@onDrawWithContent
                } else if (button.position == RIGHT && fingerX > 0f) {
                    return@onDrawWithContent
                }

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