package com.aaron.sidegesture.ui.widget

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.constant.TriggerDirection.Center
import com.aaron.sidegesture.constant.TriggerDirection.Down
import com.aaron.sidegesture.constant.TriggerDirection.Up
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.Actions
import com.aaron.sidegesture.entity.AnimationStyle
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ktx.actionsBy
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getTriggerDirection
import com.aaron.sidegesture.ktx.isNotEmpty
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
    onAction: (String) -> Unit,
    buttons: List<GestureButton>,
    modifier: Modifier = Modifier,
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
                if (actions.values.isNotEmpty() && button != null) {
                    actionPanelState.onDragStart(button.position, sideGestureState.finger, actions.values)
                    sideGestureState.cancel()
                } else if (actions.isNotEmpty()) {
                    curOnAction(actions.value)
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
    Box(modifier = modifier) {
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

    private var longPressFirstTriggerMs = 0L
    private var longPressTriggerFlags = false
    private var pressTriggerFlags = false

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
     * @return 返回null表示不识别任何手势，[Actions.NONE]表示还没触发动作，
     * [Actions.values]不为空表示触发长动作，否则表示触发一个动作
     */
    fun onDrag(dragAmount: Offset): Actions? {
        finger += dragAmount
        // 理论上能到这里button不应该为空
        val button = button ?: return null
        // 没触发方向，这一轮不再识别手势
        val direction = calcDirection(button) ?: return null
        if (direction != triggerDirection) {
            pressTriggerFlags = false
            longPressTriggerFlags = false
        }
        triggerDirection = direction
        coroutineScope.launch {
            fingerXAnim.snapTo(fingerX + dragAmount.x)
            fingerYAnim.snapTo(fingerY + dragAmount.y)
        }

        if (canDistanceTrigger(button, false)) {
            if (!pressTriggerFlags) {
                pressTriggerFlags = true
                button.vibrations.tryVibrateForPress()
            }
        } else {
            pressTriggerFlags = false
        }

        if (canDistanceTrigger(button, true)) {
            val longPressDelayMs = button.longPressTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (longPressFirstTriggerMs == 0L) {
                longPressFirstTriggerMs = timeMs
            } else if (timeMs - longPressFirstTriggerMs >= longPressDelayMs) {
                if (!longPressTriggerFlags) {
                    longPressTriggerFlags = true
                    button.vibrations.tryVibrateForLongPress()
                }
                if (!button.longPressNeedFingerUp) {
                    // 要触发ActionPanel，longPressNeedFingerUp必须为false
                    return button.longPressActions.actionsBy(triggerDirection)
                }
            }
        } else {
            longPressTriggerFlags = false
            longPressFirstTriggerMs = 0L
        }

        return Actions.NONE
    }

    fun onDragEnd(): String {
        val button = checkNotNull(button)
        val longPressDelayMs = button.longPressTriggerDelayMs
        val triggerDirection = triggerDirection
        var returnAction = GlobalActions.NONE
        if (button.longPressNeedFingerUp &&
            canDistanceTrigger(button, true) &&
            SystemClock.uptimeMillis() - longPressFirstTriggerMs >= longPressDelayMs
        ) {
            val actions = button.longPressActions.actionsBy(triggerDirection)
            returnAction = actions.value
        } else if (canDistanceTrigger(button, false)) {
            val actions = button.pressActions.actionsBy(triggerDirection)
            returnAction = actions.value
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
        longPressFirstTriggerMs = 0L
        longPressTriggerFlags = false
        pressTriggerFlags = false
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
        val pressAction = button.pressActions
        val longPressAction = button.longPressActions
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
                    pressAction.center.isNotEmpty()
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
                return canTrigger && pressAction.up.isNotEmpty()
            }
            return canTrigger && pressAction.down.isNotEmpty()
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

abstract class QuickStartState {

    var visible: Boolean by mutableStateOf(false)
        private set
    var origin: Offset by mutableStateOf(Offset.Unspecified)
        private set
    var finger: Offset by mutableStateOf(Offset.Unspecified)
        private set
    var actions: List<String> by mutableStateOf(emptyList())
        private set
    var position: Int by mutableIntStateOf(LEFT)
        private set
    private val pendingActions: MutableMap<Int, String> = mutableMapOf()

    fun onDragStart(position: Int, offset: Offset, actions: List<String>) {
        visible = true
        this.position = position
        this.origin = offset
        this.finger = offset
        this.actions = actions
    }

    fun onDrag(dragAmount: Offset) {
        finger += dragAmount
    }

    fun onDragEnd(): String {
        val pendingActions = pendingActions
        val action = pendingActions.values.find {
            it != GlobalActions.NONE
        } ?: GlobalActions.NONE
        reset()
        return action
    }

    fun onDragCancel() {
        reset()
    }

    fun isSelected(action: String): Boolean {
        return pendingActions.values.find { it == action } != null
    }

    fun select(index: Int, action: String) {
        pendingActions[index] = action
    }

    private fun reset() {
        visible = false
        pendingActions.clear()
        origin = Offset.Unspecified
        finger = Offset.Unspecified
    }
}