package com.aaron.sidegesture.ui.widget

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEach
import com.aaron.sidegesture.constant.GlobalSettings.GestureButtonColorAlpha
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.AnimationStyle
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.entity.TriggerDirection.Center
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Up
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ktx.actionsBy
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getTriggerDirection
import com.aaron.sidegesture.ktx.tryVibrateForLongPress
import com.aaron.sidegesture.ktx.tryVibrateForPress
import com.aaron.sidegesture.utils.DragGestureHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/15
 */

@Composable
fun SideGestureContainer(
    onAction: (Action) -> Unit,
    buttons: List<GestureButton>,
    modifier: Modifier = Modifier,
    drawButtonBounds: Boolean = false,
    animationStyle: AnimationStyle = WaveStyle(),
    actionPanelStyle: ActionPanelStyle = ArcStyle()
) {
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val sideGestureState = rememberSideGestureState(buttons)
    val actionPanelState = rememberActionPanelState()
    DragGestureHandler(
        onDragStart = onDragStart@{ offset ->
            sideGestureState.onDragStart(offset)
        },
        onDrag = onDrag@{ dragAmount ->
            if (actionPanelState.visible) {
                actionPanelState.onDrag(dragAmount)
                return@onDrag
            }
            if (sideGestureState.isCanceled) {
                return@onDrag
            }
            val actions = sideGestureState.onDrag(dragAmount)
            if (actions != null) {
                val button = sideGestureState.button
                if (button != null && actions.size > 1) {
                    actionPanelState.onDragStart(button.position, sideGestureState.finger, actions)
                    sideGestureState.cancel()
                } else if (actions.isNotEmpty()) {
                    curOnAction(actions.first())
                    sideGestureState.cancel()
                }
            } else {
                sideGestureState.cancel()
            }
        },
        onDragEnd = onDragEnd@{
            if (actionPanelState.visible) {
                val action = actionPanelState.onDragEnd()
                curOnAction(action)
            }
            if (sideGestureState.isCanceled) {
                sideGestureState.reset()
            } else {
                val action = sideGestureState.onDragEnd()
                curOnAction(action)
            }
        },
        onDragCancel = onDragCancel@{
            if (actionPanelState.visible) {
                actionPanelState.onDragCancel()
            }
            sideGestureState.onDragCancel()
        }
    )
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.drawBehind {
            if (drawButtonBounds) {
                buttons.fastForEach { button ->
                    if (!button.enabled) {
                        return@fastForEach
                    }
                    val bounds = button.bounds()
                    drawRect(
                        color = when (button.isDefault) {
                            true -> colorScheme.primary.copy(alpha = GestureButtonColorAlpha)
                            else -> Color(button.color)
                        },
                        topLeft = bounds.topLeft,
                        size = bounds.size
                    )
                }
            }
        }
    ) {
        ActionPanel(
            modifier = Modifier.matchParentSize(),
            actionPanelStyle = actionPanelStyle,
            actionPanelState = actionPanelState,
            vibrations = sideGestureState.button?.vibrations
        )

        GestureAnimation(
            modifier = Modifier.matchParentSize(),
            animationStyle = animationStyle,
            sideGestureState = sideGestureState
        )
    }
}

@Composable
private fun rememberSideGestureState(buttons: List<GestureButton>): SideGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope, buttons) {
        SideGestureState(coroutineScope, buttons)
    }
}

class SideGestureState(
    private val coroutineScope: CoroutineScope,
    private val buttons: List<GestureButton>
) {

    var isCanceled: Boolean by mutableStateOf(false)
        private set

    var button: GestureButton? by mutableStateOf(null)
        private set
    var triggerDirection by mutableStateOf(Center)
        private set

    val originY: Float get() = originYAnim.value
    val fingerX: Float get() = fingerXAnim.value
    val fingerY: Float get() = fingerYAnim.value
    private val originYAnim = Animatable(Float.NaN)
    private val fingerXAnim = Animatable(Float.NaN)
    private val fingerYAnim = Animatable(Float.NaN)

    var origin = Offset.Unspecified
        private set
    var finger = Offset.Unspecified
        private set

    private var longSlideFirstTriggerMs = 0L
    private var longSlideTriggerFlags = false
    private var slideTriggerFlags = false

    private val animationSpec = spring<Float>(stiffness = 3000f)

    fun onDragStart(offset: Offset) {
        origin = offset
        finger = offset
        button = buttons.find(offset)

        coroutineScope.launch {
            val curY = offset.y
            originYAnim.snapTo(curY)
            fingerXAnim.snapTo(0f)
            fingerYAnim.snapTo(curY)
        }
    }

    /**
     * @return 返回null表示不识别任何手势，emptyList()表示还没触发动作，
     * 长列表表示触发长动作，否则表示触发一个动作
     */
    fun onDrag(dragAmount: Offset): List<Action>? {
        finger += dragAmount
        // 理论上能到这里button不应该为空
        val button = button ?: return null
        // 没触发方向，这一轮不再识别手势
        val direction = calcDirection(button) ?: return null
        if (direction != triggerDirection) {
            slideTriggerFlags = false
            longSlideTriggerFlags = false
        }
        triggerDirection = direction
        coroutineScope.launch {
            fingerXAnim.snapTo(fingerX + dragAmount.x)
            fingerYAnim.snapTo(fingerY + dragAmount.y)
        }

        if (canDistanceTrigger(button, false)) {
            if (button.vibrations.vibrateImmediately && !slideTriggerFlags) {
                slideTriggerFlags = true
                button.vibrations.tryVibrateForPress()
            }
        } else {
            slideTriggerFlags = false
        }

        if (canDistanceTrigger(button, true)) {
            val longPressDelayMs = button.longSlideTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (longSlideFirstTriggerMs == 0L) {
                longSlideFirstTriggerMs = timeMs
            } else if (timeMs - longSlideFirstTriggerMs >= longPressDelayMs) {
                if (button.vibrations.vibrateImmediately && !longSlideTriggerFlags) {
                    longSlideTriggerFlags = true
                    button.vibrations.tryVibrateForLongPress()
                }
                if (button.longSlideTriggerImmediately) {
                    // 要触发ActionPanel，longPressTriggerImmediately必须为true
                    return button.longSlideActions.actionsBy(triggerDirection)
                }
            }
        } else {
            longSlideTriggerFlags = false
            longSlideFirstTriggerMs = 0L
        }

        return emptyList()
    }

    fun onDragEnd(): Action {
        val button = checkNotNull(button)
        val longPressDelayMs = button.longSlideTriggerDelayMs
        val triggerDirection = triggerDirection
        var returnAction = Action.NONE
        if (!button.longSlideTriggerImmediately &&
            canDistanceTrigger(button, true) &&
            SystemClock.uptimeMillis() - longSlideFirstTriggerMs >= longPressDelayMs
        ) {
            if (!button.vibrations.vibrateImmediately) {
                button.vibrations.tryVibrateForLongPress()
            }
            val actions = button.longSlideActions.actionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null) {
                returnAction = action
            }
        } else if (canDistanceTrigger(button, false)) {
            if (!button.vibrations.vibrateImmediately) {
                button.vibrations.tryVibrateForPress()
            }
            val actions = button.slideActions.actionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null) {
                returnAction = action
            }
        }
        reset()
        return returnAction
    }

    fun onDragCancel() {
        reset()
    }

    /**
     * 自己程序取消逻辑，非系统干预
     */
    fun cancel() {
        reset()
        isCanceled = true
    }

    fun reset() {
        isCanceled = false
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        longSlideFirstTriggerMs = 0L
        longSlideTriggerFlags = false
        slideTriggerFlags = false
        coroutineScope.launch {
            val fingerXAnim = fingerXAnim
            val fingerYAnim = fingerYAnim
            coroutineScope {
                launch { fingerXAnim.animateTo(0f, animationSpec) }
                launch { fingerYAnim.animateTo(originY, animationSpec) }
            }
        }
    }

    /**
     * 手指划过的距离是否足够触发动作，上和下的动作需要按斜线距离计算
     */
    private fun canDistanceTrigger(button: GestureButton, isLongPress: Boolean): Boolean {
        val slideAction = button.slideActions
        val longSlideAction = button.longSlideActions
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x
        val fingerY = finger.y
        val triggerDirection = triggerDirection
        if (triggerDirection == Center) {
            val distance = abs(fingerX - originX)
            if (isLongPress) {
                return distance >= button.longSlideTriggerDistance &&
                        longSlideAction.center.isNotEmpty()
            }
            return distance >= button.slideTriggerDistance &&
                    slideAction.center.isNotEmpty()
        } else if (triggerDirection == Up || triggerDirection == Down) {
            val edge1 = abs(fingerX - originX)
            val edge2 = abs(fingerY - originY)
            val edge3 = hypot(edge1, edge2)
            if (isLongPress) {
                val canTrigger = edge3 >= button.longSlideTriggerDistance
                if (triggerDirection == Up) {
                    return canTrigger && longSlideAction.up.isNotEmpty()
                }
                return canTrigger && longSlideAction.down.isNotEmpty()
            }
            val canTrigger = edge3 >= button.slideTriggerDistance
            if (triggerDirection == Up) {
                return canTrigger && slideAction.up.isNotEmpty()
            }
            return canTrigger && slideAction.down.isNotEmpty()
        }
        return false
    }

    private fun calcDirection(button: GestureButton): TriggerDirection? {
        val origin = origin
        val finger = finger
        val x = when (button.position) {
            Position.Left -> finger.x
            Position.Right -> origin.x - finger.x
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
        return button.angle.getTriggerDirection(degree.toFloat())
    }
}

abstract class QuickStartState {

    var visible: Boolean by mutableStateOf(false)
        private set
    var origin: Offset by mutableStateOf(Offset.Unspecified)
        private set
    var finger: Offset by mutableStateOf(Offset.Unspecified)
        private set
    var actions: List<Action> by mutableStateOf(emptyList())
        private set
    var position: Position by mutableStateOf(Position.Left)
        private set
    private val pendingActions: MutableMap<Int, Action> = mutableMapOf()

    fun onDragStart(position: Position, offset: Offset, actions: List<Action>) {
        visible = true
        this.position = position
        this.origin = offset
        this.finger = offset
        this.actions = actions
    }

    fun onDrag(dragAmount: Offset) {
        finger += dragAmount
    }

    fun onDragEnd(): Action {
        val pendingActions = pendingActions
        val action = pendingActions.values.find {
            it != Action.NONE
        } ?: Action.NONE
        reset()
        return action
    }

    fun onDragCancel() {
        reset()
    }

    fun isSelected(action: Action): Boolean {
        return pendingActions.values.find { it == action } != null
    }

    fun select(index: Int, action: Action) {
        pendingActions[index] = action
    }

    private fun reset() {
        visible = false
        pendingActions.clear()
        origin = Offset.Unspecified
        finger = Offset.Unspecified
    }
}