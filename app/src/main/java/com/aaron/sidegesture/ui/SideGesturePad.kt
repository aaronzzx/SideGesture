package com.aaron.sidegesture.ui

import android.os.SystemClock
import androidx.annotation.Keep
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEachIndexed
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.config.Actions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.actionBy
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getTriggerDirection
import com.aaron.sidegesture.ktx.vibrate
import com.aaron.sidegesture.ui.TriggerDirection.Center
import com.aaron.sidegesture.ui.TriggerDirection.Down
import com.aaron.sidegesture.ui.TriggerDirection.Up
import com.aaron.sidegesture.utils.GestureHandler
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
    animationStyle: AnimationStyle = AnimationStyle.Wave()
) {
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val coroutineScope = rememberCoroutineScope()
    var rootSize by remember { mutableStateOf(Size.Zero) }
    var lock: Any? by remember { mutableStateOf(null) }
    val actionListener = remember {
        object : ActionListener {
            override fun onTrigger(action: Int) {
                curOnAction(action)
            }
        }
    }

    val multipleActionHandler = rememberMultipleActionHandler(listener = actionListener)
    val stateHolder = rememberStateHolder(
        coroutineScope = coroutineScope,
        buttons = buttons,
        listener = actionListener
    )
    val arrowBack = rememberVectorPainter(Icons.Default.ArrowBack)
    val arrowForward = rememberVectorPainter(Icons.Default.ArrowForward)
    val defaultIcons = remember(arrowForward, arrowBack) { arrowForward to arrowBack }

    var isCancelGesture by remember { mutableStateOf(false) }
    var isMultipleAction by remember { mutableStateOf(false) }
    GestureHandler(
        onDragStart = onDragStart@{ offset ->
            if (lock != null && lock != stateHolder) {
                return@onDragStart
            }
            lock = stateHolder
            stateHolder.onDragStart(rootSize, offset)
            multipleActionHandler.onDragStart(offset)
        },
        onDrag = onDrag@{ dragAmount ->
            if (lock != stateHolder || isCancelGesture) {
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
                    val button = stateHolder.button
                    if (button != null) {
                        multipleActionHandler.onExpanded(actions, button.position)
                    }
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
            if (lock != stateHolder) {
                return@onDragEnd
            }
            if (isMultipleAction) {
                multipleActionHandler.onDragEnd()
            } else if (!isCancelGesture) {
                stateHolder.onDragEnd()
            }
            lock = null
            isCancelGesture = false
            isMultipleAction = false
        },
        onDragCancel = onDragCancel@{
            lock = null
            isCancelGesture = false
            isMultipleAction = false
            stateHolder.onDragCancel()
            multipleActionHandler.onDragCancel()
        }
    )
    Box(
        modifier = modifier
            .onSizeChanged {
                rootSize = it.toSize()
            }
//            .background(color = Color.Red.copy(alpha = 0.1f))
//            .drawBehind {
//                buttons.fastForEach { button ->
//                    val bounds = button.bounds(rootSize)
//                    drawRect(
//                        color = Color(button.color),
//                        topLeft = bounds.topLeft,
//                        size = bounds.size
//                    )
//                }
//            }
            .drawWithCache {
                animationStyle.draw(this, stateHolder, defaultIcons)
            }
    ) {
        val button = stateHolder.button
        if (button != null) {
            multipleActionHandler.ActionPanel(button)
        }
    }
}

@Composable
private fun rememberStateHolder(
    coroutineScope: CoroutineScope,
    buttons: List<GestureButton>,
    listener: ActionListener
): StateHolder = remember(coroutineScope, buttons, listener) {
    StateHolder(coroutineScope, buttons, listener)
}

class StateHolder(
    private val coroutineScope: CoroutineScope,
    private val buttons: List<GestureButton>,
    private val listener: ActionListener
) {

    var button: GestureButton? by mutableStateOf(null)
        private set
    var triggerDirection by mutableStateOf(Center)
        private set

    val originY = Animatable(Float.NaN)
    val fingerX = Animatable(Float.NaN)
    val fingerY = Animatable(Float.NaN)

    private var origin = Offset.Unspecified
    private var finger = Offset.Unspecified

    private var longPressFirstTriggerMs = 0L
    private var longPressTriggerFlags = false
    private var pressTriggerFlags = false

    private val animationSpec = spring<Float>(stiffness = 3000f)

    fun onDragStart(rootSize: Size, offset: Offset) {
        origin = offset
        finger = offset
        button = buttons.find(rootSize, offset)

        coroutineScope.launch {
            val curY = offset.y
            originY.snapTo(curY)
            fingerY.snapTo(curY)
            fingerX.snapTo(0f)
        }
    }

    /**
     * @return 返回null表示不识别任何手势，空列表表示还没触发动作，单列表表示触发一个动作，长列表表示触发长动作
     */
    fun onDrag(dragAmount: Offset): List<Int>? {
        val button = button ?: return null

        finger += dragAmount
        // 没触发方向，这一轮不再识别手势
        triggerDirection = calcDirection(button) ?: return null
        coroutineScope.launch {
            val fingerX = fingerX
            val fingerY = fingerY
            fingerX.snapTo(fingerX.value + dragAmount.x)
            fingerY.snapTo(fingerY.value + dragAmount.y)
        }

        val longPressDelayMs = button.longPressTriggerDelayMs
        if (canDistanceTrigger(button, true)) {
            val timeMs = SystemClock.uptimeMillis()
            if (longPressFirstTriggerMs == 0L) {
                longPressFirstTriggerMs = timeMs
            } else if (!button.longPressNeedFingerUp &&
                timeMs - longPressFirstTriggerMs >= longPressDelayMs
            ) {
                if (button.vibrations.forLongPress && !longPressTriggerFlags) {
                    longPressTriggerFlags = true
                    button.vibrations.vibrate()
                }
                val listener = listener
                val actions = button.longPressAction.actionBy(triggerDirection)
                if (actions.size > 1) {
                    return actions
                } else if (actions.size == 1) {
                    val action = actions.first()
                    if (action != Actions.NONE) {
                        listener.onTrigger(action)
                        return listOf(action)
                    }
                }
            }
        } else if (canDistanceTrigger(button, false)) {
            if (button.vibrations.forPress && !pressTriggerFlags) {
                pressTriggerFlags = true
                button.vibrations.vibrate()
            }
        } else {
            longPressTriggerFlags = false
            pressTriggerFlags = false
        }
        return emptyList()
    }

    fun onDragEnd() {
        val button = checkNotNull(button)
        val listener = listener
        val longPressDelayMs = button.longPressTriggerDelayMs
        val triggerDirection = triggerDirection
        if (button.longPressNeedFingerUp &&
            canDistanceTrigger(button, true) &&
            SystemClock.uptimeMillis() - longPressFirstTriggerMs >= longPressDelayMs
        ) {
            val actions = button.longPressAction.actionBy(triggerDirection)
            if (actions.isNotEmpty()) {
                val action = actions.first()
                if (action != Actions.NONE) {
                    listener.onTrigger(action)
                }
            }
        } else if (canDistanceTrigger(button, false)) {
            val action = button.pressAction.actionBy(triggerDirection)
            if (action != Actions.NONE) {
                listener.onTrigger(action)
            }
        }
        reset()
    }

    fun onDragCancel() {
        reset()
    }

    private fun reset() {
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        longPressFirstTriggerMs = 0L
        longPressTriggerFlags = false
        pressTriggerFlags = false
        coroutineScope.launch {
            val originY = originY
            val fingerX = fingerX
            val fingerY = fingerY
            val job = launch {
                fingerY.animateTo(originY.value, animationSpec)
            }
            fingerX.animateTo(0f, animationSpec)
            job.join()
            originY.snapTo(Float.NaN)
            fingerX.snapTo(Float.NaN)
            fingerY.snapTo(Float.NaN)
        }
    }

    /**
     * 手指划过的距离是否足够触发动作，上和下的动作需要按斜线距离计算
     */
    private fun canDistanceTrigger(button: GestureButton, isLongPress: Boolean): Boolean {
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
                    pressAction.center != Actions.NONE
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
                return canTrigger && pressAction.up != Actions.NONE
            }
            return canTrigger && pressAction.down != Actions.NONE
        }
        return false
    }

    private fun calcDirection(button: GestureButton): TriggerDirection? {
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
        return button.angles.getTriggerDirection(degree.toFloat())
    }
}

@Composable
private fun rememberMultipleActionHandler(
    listener: ActionListener
): MultipleActionHandler = remember(listener) {
    MultipleActionHandler(listener)
}

class MultipleActionHandler(private val listener: ActionListener) {

    private var origin: Origin by mutableStateOf(Origin())
    private var finger: Offset by mutableStateOf(Offset.Unspecified)
    private var actions: List<Int> by mutableStateOf(emptyList())
    private var pendingActions: MutableMap<Int, Int?> = mutableMapOf()

    @Composable
    fun ActionPanel(button: GestureButton) {
        val actions by rememberUpdatedState(newValue = actions)
        val origin by rememberUpdatedState(newValue = origin)
        val finger by rememberUpdatedState(newValue = finger)
        val itemSize = 60.dp
        val hypot = itemSize.toPx() * 2f
        if (!origin.isInvalid && finger != Offset.Unspecified && actions.isNotEmpty()) {
            Box(
                Modifier
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
                                            if (button.vibrations.forActionPanel) {
                                                button.vibrations.vibrate()
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

    fun onDragStart(offset: Offset) {
        origin = Origin()
        finger = offset
    }

    fun onDrag(dragAmount: Offset) {
        finger += dragAmount
    }

    fun onExpanded(actions: List<Int>, position: Int) {
        this.actions = actions
        if (origin.isInvalid) {
            origin = Origin(finger, position)
        }
    }

    fun onDragEnd() {
        val pendingActions = pendingActions
        val action = pendingActions.values.find { it != null }
        if (action != null && action != Actions.NONE) {
            listener.onTrigger(action)
        }
        reset()
    }

    fun onDragCancel() {
        reset()
    }

    private fun reset() {
        finger = Offset.Unspecified
        actions = emptyList()
        pendingActions.clear()
    }

    private data class Origin(
        val offset: Offset = Offset.Unspecified,
        val position: Int = -1
    ) {
        val isInvalid: Boolean get() = offset.isUnspecified || position == -1
    }
}

interface ActionListener {

    fun onTrigger(action: Int)
}

enum class TriggerDirection {

    Up, Center, Down
}

sealed interface AnimationStyle {

    fun draw(
        drawScope: CacheDrawScope,
        stateHolder: StateHolder,
        defaultIcons: Pair<Painter, Painter>
    ): DrawResult

    @Keep
    data class Wave(
        val backgroundColor: Int = android.graphics.Color.BLACK,
        val strokeColor: Int = android.graphics.Color.TRANSPARENT,
        val strokeWidth: Int = 0,
        val iconColor: Int = android.graphics.Color.argb(200, 255, 255, 255),
        val iconUriString: String? = null
    ) : AnimationStyle {

        override fun draw(
            drawScope: CacheDrawScope,
            stateHolder: StateHolder,
            defaultIcons: Pair<Painter, Painter>
        ): DrawResult = drawScope.run {
            val size = size
            val bezierPath = Path()
            // 贝塞尔偏移值，使贝塞尔显示在手指落点上方
            val bezierOffset = 70.dp.toPx()
            // 贝塞尔与边界间距
            val bezierSpacing = 40.dp.toPx()
            // 贝塞尔的最大宽度
            val bezierMaxWidth = 40.dp.toPx()
            // 贝塞尔长度的一半
            val halfBezierLength = 100.dp.toPx()
            // 贝塞尔变形限制
            val offsetYCoerce = 55.dp.toPx()

            onDrawWithContent {
                drawContent()

                val originY = stateHolder.originY.value
                val fingerX = stateHolder.fingerX.value
                val fingerY = stateHolder.fingerY.value
                if (originY.isNaN() || fingerX.isNaN() || fingerY.isNaN()) {
                    return@onDrawWithContent
                }
                val button = stateHolder.button ?: return@onDrawWithContent
                if (button.position == LEFT && fingerX < 0f) {
                    return@onDrawWithContent
                } else if (button.position == RIGHT && fingerX > 0f) {
                    return@onDrawWithContent
                }

                // 动画y轴偏移值
                val offsetY = (originY - fingerY).coerceIn(-offsetYCoerce, offsetYCoerce)
                // 能完整显示整个贝塞尔并且留有间距
                val safeOriginY = (originY - bezierOffset).coerceIn(
                    minimumValue = halfBezierLength + bezierSpacing,
                    maximumValue = size.height - halfBezierLength - bezierSpacing
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
                    val defaultIcon = defaultIcons.first.takeIf {
                        button.position == LEFT
                    } ?: defaultIcons.second
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