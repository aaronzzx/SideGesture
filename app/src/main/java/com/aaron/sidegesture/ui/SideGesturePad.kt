package com.aaron.sidegesture.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.config.Actions
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.AnimationStyle
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.entity.TriggerDirection.Center
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Up
import com.aaron.sidegesture.ktx.actionBy
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
fun SideGesturePad(
    onAction: (Int) -> Unit,
    buttons: List<GestureButton>,
    modifier: Modifier = Modifier,
    animationStyle: AnimationStyle = AnimationStyle.Wave(),
    actionPanelStyle: ActionPanelStyle = ActionPanelStyle.Arc
) {
    val sideGestureState = rememberSideGestureState(buttons, onAction)
    val actionPanelState = rememberActionPanelState(onAction)
    DragGestureHandler(
        onDragStart = onDragStart@{ offset ->
            sideGestureState.onDragStart(offset)
        },
        onDrag = onDrag@{ dragAmount ->
            if (actionPanelState.isExpanded) {
                actionPanelState.onDrag(dragAmount)
                return@onDrag
            }
            if (sideGestureState.isCanceled) {
                return@onDrag
            }
            val actions = sideGestureState.onDrag(dragAmount)
            if (actions != null) {
                if (actions.size > 1) {
                    actionPanelState.onDragStart(sideGestureState.finger, actions)
                    sideGestureState.cancel()
                } else if (actions.size == 1) {
                    sideGestureState.cancel()
                }
            } else {
                sideGestureState.cancel()
            }
        },
        onDragEnd = onDragEnd@{
            if (actionPanelState.isExpanded) {
                actionPanelState.onDragEnd()
            }
            if (sideGestureState.isCanceled) {
                sideGestureState.reset()
            } else {
                sideGestureState.onDragEnd()
            }
        },
        onDragCancel = onDragCancel@{
            if (actionPanelState.isExpanded) {
                actionPanelState.onDragCancel()
            }
            sideGestureState.onDragCancel()
        }
    )
    Box(modifier = modifier) {
        GestureAnimation(
            modifier = Modifier.matchParentSize(),
            animationStyle = animationStyle,
            sideGestureState = sideGestureState
        )

        val button = sideGestureState.button
        if (button != null) {
            ActionPanel(
                modifier = Modifier.matchParentSize(),
                actionPanelStyle = actionPanelStyle,
                actionPanelState = actionPanelState,
                position = button.position,
                vibrations = button.vibrations
            )
        }
    }
}

@Composable
private fun rememberSideGestureState(
    buttons: List<GestureButton>,
    onAction: (Int) -> Unit
): SideGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope, buttons, onAction) {
        SideGestureState(coroutineScope, buttons, onAction)
    }
}

class SideGestureState(
    private val coroutineScope: CoroutineScope,
    private val buttons: List<GestureButton>,
    private val onAction: (Int) -> Unit
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
     * @return 返回null表示不识别任何手势，空列表表示还没触发动作，单列表表示触发一个动作，长列表表示触发长动作
     */
    fun onDrag(dragAmount: Offset): List<Int>? {
        finger += dragAmount
        // 理论上能到这里button不应该为空
        val button = button ?: return null
        // 没触发方向，这一轮不再识别手势
        triggerDirection = calcDirection(button) ?: return null
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
            } else if (!button.longPressNeedFingerUp &&
                timeMs - longPressFirstTriggerMs >= longPressDelayMs
            ) {
                if (!longPressTriggerFlags) {
                    longPressTriggerFlags = true
                    button.vibrations.tryVibrateForLongPress()
                }
                val actions = button.longPressAction.actionBy(triggerDirection)
                if (actions.size > 1) {
                    return actions
                } else if (actions.size == 1) {
                    val action = actions.first()
                    if (action != Actions.NONE) {
                        onAction(action)
                        return listOf(action)
                    }
                }
            }
        } else {
            longPressTriggerFlags = false
        }

        return emptyList()
    }

    fun onDragEnd() {
        val button = checkNotNull(button)
        val onAction = onAction
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
                    onAction(action)
                }
            }
        } else if (canDistanceTrigger(button, false)) {
            val action = button.pressAction.actionBy(triggerDirection)
            if (action != Actions.NONE) {
                onAction(action)
            }
        }
        reset()
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
            val originYAnim = originYAnim
            val fingerXAnim = fingerXAnim
            val fingerYAnim = fingerYAnim
            coroutineScope {
                launch { fingerXAnim.animateTo(0f, animationSpec) }
                launch { fingerYAnim.animateTo(originY, animationSpec) }
            }
            originYAnim.snapTo(Float.NaN)
            fingerXAnim.snapTo(Float.NaN)
            fingerYAnim.snapTo(Float.NaN)
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
private fun rememberActionPanelState(onAction: (Int) -> Unit): ActionPanelState {
    return remember(onAction) {
        ActionPanelState(onAction)
    }
}

class ActionPanelState(private val onAction: (Int) -> Unit) {

    var isExpanded: Boolean by mutableStateOf(false)
        private set
    var origin: Offset by mutableStateOf(Offset.Unspecified)
        private set
    var finger: Offset by mutableStateOf(Offset.Unspecified)
        private set
    var actions: List<Int> by mutableStateOf(emptyList())
        private set
    private val pendingActions: MutableMap<Int, Int?> = mutableMapOf()

    fun onDragStart(offset: Offset, actions: List<Int>) {
        isExpanded = true
        origin = offset
        finger = offset
        this.actions = actions
    }

    fun onDrag(dragAmount: Offset) {
        finger += dragAmount
    }

    fun onDragEnd() {
        val pendingActions = pendingActions
        val action = pendingActions.values.find { it != null }
        if (action != null && action != Actions.NONE) {
            onAction(action)
        }
        reset()
    }

    fun onDragCancel() {
        reset()
    }

    fun isSelected(action: Int): Boolean {
        return pendingActions.values.find { it == action } != null
    }

    fun select(index: Int, action: Int?) {
        pendingActions[index] = action
    }

    private fun reset() {
        isExpanded = false
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        pendingActions.clear()
    }
}