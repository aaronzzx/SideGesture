package com.aaron.sidegesture.ui.widget

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.constant.GlobalActions
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
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.ktx.GESTURE_ANGLE_BASE
import com.aaron.sidegesture.ktx.actionsBy
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getTriggerDirection
import com.aaron.sidegesture.ktx.takeScreenshot
import com.aaron.sidegesture.ktx.tryVibrateForLongPress
import com.aaron.sidegesture.ktx.tryVibrateForPress
import com.aaron.sidegesture.utils.DragGestureHandler
import com.aaron.sidegesture.utils.showVersionTooLowToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    imePadding: Int = 0,
    animationStyle: AnimationStyle? = WaveStyle(),
    actionPanelStyle: ActionPanelStyle = ArcStyle(),
    actionSettings: ActionSettings = ActionSettings()
) {
    val context = LocalContext.current
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val sideGestureState = rememberSideGestureState(buttons)
    val actionPanelState = rememberActionPanelState()
    val moveScreenState = rememberMoveScreenState(actionSettings.moveScreen.rate)
    DragGestureHandler(
        onDragStart = onDragStart@{ offset ->
            sideGestureState.onDragStart(offset, imePadding)
        },
        onDrag = onDrag@{ dragAmount ->
            if (actionPanelState.visible) {
                actionPanelState.onDrag(dragAmount)
                return@onDrag
            }
            if (moveScreenState.visible) {
                moveScreenState.onDrag(dragAmount)
                return@onDrag
            }
            if (sideGestureState.isCanceled) {
                return@onDrag
            }
            val actions = sideGestureState.onDrag(dragAmount)
            if (actions != null) {
                val button = sideGestureState.button
                if (button != null && actions.size > 1) {
                    actionPanelState.onDragStart(sideGestureState.finger)
                    actionPanelState.ready(button.position, actions)
                    sideGestureState.cancel()
                } else if (actions.isNotEmpty()) {
                    if (actions.first().value == GlobalActions.MOVE_SCREEN) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            showVersionTooLowToast(context, R.string.action_move_screen)
                            sideGestureState.cancel()
                            return@onDrag
                        }
                        moveScreenState.onDragStart(sideGestureState.finger)
                        sideGestureState.cancel()
                    } else {
                        curOnAction(actions.first())
                        sideGestureState.cancel()
                    }
                }
            } else {
                sideGestureState.cancel()
            }
        },
        onDragEnd = onDragEnd@{
            if (actionPanelState.visible) {
                val action = actionPanelState.done()
                actionPanelState.onDragEnd()
                curOnAction(action)
            }
            if (sideGestureState.isCanceled) {
                sideGestureState.reset()
            } else {
                val action = sideGestureState.onDragEnd()
                curOnAction(action)
            }
            if (moveScreenState.visible) {
                val action = moveScreenState.done()
                moveScreenState.onDragEnd()
                curOnAction(action)
            }
        },
        onDragCancel = onDragCancel@{
            if (actionPanelState.visible) {
                actionPanelState.onDragCancel()
            }
            sideGestureState.onDragCancel()
            if (moveScreenState.visible) {
                moveScreenState.onDragCancel()
            }
        }
    )
    Box(modifier = modifier) {
        ActionPanel(
            modifier = Modifier.matchParentSize(),
            actionPanelStyle = actionPanelStyle,
            actionPanelState = actionPanelState,
            vibrations = sideGestureState.button?.vibrations
        )

        if (!moveScreenState.visible && animationStyle != null) {
            GestureAnimation(
                modifier = Modifier.matchParentSize(),
                animationStyle = animationStyle,
                sideGestureState = sideGestureState
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && moveScreenState.visible) {
            val screenshotState: State<Bitmap?> = produceState<Bitmap?>(null) {
                // 16ms为屏幕一帧，等待一帧防止截到手势
                delay(20)
                val service = context as SideGestureService
                value = service.takeScreenshot()
            }
            val screenshot = screenshotState.value
            if (screenshot != null) {
                MoveScreen(
                    modifier = Modifier.matchParentSize(),
                    screenshot = screenshot,
                    state = moveScreenState
                )
            }
        }
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

    val originXAnimVal: Float get() = originXAnim.value
    val originYAnimVal: Float get() = originYAnim.value
    val fingerXAnimVal: Float get() = fingerXAnim.value
    val fingerYAnimVal: Float get() = fingerYAnim.value
    private val originXAnim = Animatable(Float.NaN)
    private val originYAnim = Animatable(Float.NaN)
    private val fingerXAnim = Animatable(Float.NaN)
    private val fingerYAnim = Animatable(Float.NaN)

    var origin = Offset.Unspecified
        private set
    var finger = Offset.Unspecified
        private set
    private var buttonBounds: Rect? = null

    private var longSlideFirstTriggerMs = 0L
    private var longSlideTriggerFlags = false
    private var slideTriggerFlags = false

    private val animationSpec = spring<Float>(stiffness = 3000f)

    fun onDragStart(offset: Offset, imePadding: Int) {
        origin = offset
        finger = offset
        button = buttons.find(offset, imePadding)
        buttonBounds = button?.bounds(imePadding)

        val button = button ?: return
        coroutineScope.launch {
            originXAnim.snapTo(offset.x)
            originYAnim.snapTo(offset.y)
            when (button.position) {
                Position.Left, Position.Right -> {
                    fingerXAnim.snapTo(0f)
                    fingerYAnim.snapTo(offset.y)
                }
                Position.Bottom -> {
                    fingerXAnim.snapTo(offset.x)
                    fingerYAnim.snapTo(0f)
                }
            }
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
            fingerXAnim.snapTo(fingerXAnimVal + dragAmount.x)
            fingerYAnim.snapTo(fingerYAnimVal + dragAmount.y)
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
        val button = button ?: return Action.NONE
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
            val position = button?.position ?: return@launch
            coroutineScope {
                when (position) {
                    Position.Left, Position.Right -> {
                        launch { fingerXAnim.animateTo(0f, animationSpec) }
                        launch { fingerYAnim.animateTo(originYAnimVal, animationSpec) }
                    }
                    Position.Bottom -> {
                        launch { fingerYAnim.animateTo(0f, animationSpec) }
                        launch { fingerXAnim.animateTo(originXAnimVal, animationSpec) }
                    }
                }
            }
        }
    }

    /**
     * 手指划过的距离是否足够触发动作
     */
    private fun canDistanceTrigger(button: GestureButton, isLongSlide: Boolean): Boolean {
        val slideAction = button.slideActions
        val longSlideAction = button.longSlideActions
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x
        val fingerY = finger.y
        val slideDistance = when (button.position) {
            Position.Left -> fingerX - originX
            Position.Right -> originX - fingerX
            Position.Bottom -> originY - fingerY
        }
        // 解决触钮往回滑还能触发的问题
        if (slideDistance < 0) {
            return false
        }
        val triggerDirection = triggerDirection
        if (triggerDirection == Center) {
            if (isLongSlide) {
                return slideDistance >= button.longSlideTriggerDistance &&
                        longSlideAction.center.isNotEmpty()
            }
            return slideDistance >= button.slideTriggerDistance &&
                    slideAction.center.isNotEmpty()
        } else if (triggerDirection == Up || triggerDirection == Down) {
            // 需要计算斜边
            val edge1 = slideDistance
            val edge2 = when (button.position) {
                Position.Left, Position.Right -> abs(fingerY - originY)
                Position.Bottom -> abs(fingerX - originX)
            }
            val hypot = hypot(edge1, edge2)
            if (isLongSlide) {
                val canTrigger = hypot >= button.longSlideTriggerDistance
                if (triggerDirection == Up) {
                    return canTrigger && longSlideAction.up.isNotEmpty()
                }
                return canTrigger && longSlideAction.down.isNotEmpty()
            }
            val canTrigger = hypot >= button.slideTriggerDistance
            if (triggerDirection == Up) {
                return canTrigger && slideAction.up.isNotEmpty()
            }
            return canTrigger && slideAction.down.isNotEmpty()
        }
        return false
    }

    private fun calcDirection(button: GestureButton): TriggerDirection? {
        val buttonBounds = buttonBounds ?: return null
        val origin = origin
        val finger = finger
        val opposite = when (button.position) {
            Position.Left -> finger.x - buttonBounds.left
            Position.Right -> buttonBounds.right - finger.x
            Position.Bottom -> buttonBounds.bottom - finger.y
        }
        val neighbor = when (button.position) {
            Position.Left, Position.Right -> abs(finger.y - origin.y)
            Position.Bottom -> abs(finger.x - origin.x)
        }
        val tanVal = opposite / neighbor
        val radians = atan(tanVal)
        val isPreviousArea = when (button.position) {
            Position.Left, Position.Right -> finger.y < origin.y
            Position.Bottom -> finger.x < origin.x
        }
        val degree = if (isPreviousArea) {
            // 上半区
            Math.toDegrees(radians.toDouble())
        } else {
            // 下半区
            GESTURE_ANGLE_BASE - Math.toDegrees(radians.toDouble())
        }
        return button.angle.getTriggerDirection(degree.toFloat())
    }
}

abstract class LongSlideState {

    var origin: Offset by mutableStateOf(Offset.Unspecified)
        protected set
    var finger: Offset by mutableStateOf(Offset.Unspecified)
        protected set

    open fun onDragStart(offset: Offset) {
        origin = offset
        finger = offset
    }

    open fun onDrag(dragAmount: Offset) {
        finger += dragAmount
    }

    open fun onDragEnd() {
        reset()
    }

    open fun onDragCancel() {
        reset()
    }

    protected open fun reset() {
        origin = Offset.Unspecified
        finger = Offset.Unspecified
    }
}