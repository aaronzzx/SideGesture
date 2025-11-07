package com.aaron.sidegesture.ui.widget

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.ktx.GESTURE_ANGLE_BASE
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getParallelTriggerDirection
import com.aaron.sidegesture.ktx.getTriggerDirection
import com.aaron.sidegesture.ktx.ohoActionsBy
import com.aaron.sidegesture.ktx.parallelActionsBy
import com.aaron.sidegesture.ktx.takeScreenshot
import com.aaron.sidegesture.ktx.tryVibrateForLongSlide
import com.aaron.sidegesture.ktx.tryVibrateForSlide
import com.aaron.sidegesture.utils.DragGestureHandler
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
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
    actionSettings: ActionSettings = ActionSettings(),
    advancedSettings: AdvancedSettings = AdvancedSettings()
) {
    val context = LocalContext.current
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val ohoGestureState = rememberOHOGestureState(buttons, advancedSettings)
    val parallelGestureState = rememberParallelGestureState(buttons = buttons)
    val actionPanelState = rememberActionPanelState()
    val moveScreenState = rememberMoveScreenState(actionSettings.moveScreen.rate)

    val onParallelActionBlock: (List<Action>?) -> Unit = onParallelActionBlock@{ actions ->
        val button = parallelGestureState.button
        if (actions != null) {
            if (button != null && actions.size > 1) {
                actionPanelState.onDragStart(parallelGestureState.finger)
                actionPanelState.ready(button.position, actions)
                ohoGestureState.cancel()
                parallelGestureState.cancel()
            } else if (actions.isNotEmpty()) {
                ohoGestureState.cancel()
                if (actions.first().value == GlobalActions.MOVE_SCREEN) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        showVersionTooLowToast(context, R.string.action_move_screen)
                        parallelGestureState.cancel()
                        return@onParallelActionBlock
                    }
                    moveScreenState.onDragStart(parallelGestureState.finger)
                    parallelGestureState.cancel()
                } else {
                    curOnAction(actions.first())
                    parallelGestureState.cancel()
                }
            }
        } else {
            parallelGestureState.cancel()
        }
    }
    SideEffect {
        parallelGestureState.onLongPress = onParallelActionBlock
    }

    DragGestureHandler(
        onDragStart = onDragStart@{ offset ->
            ohoGestureState.onDragStart(offset, imePadding)
            parallelGestureState.onDragStart(offset, imePadding)
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
            if (!ohoGestureState.isCanceled) {
                val actions = ohoGestureState.onDrag(dragAmount)
                if (actions != null) {
                    val button = ohoGestureState.button
                    if (button != null && actions.size > 1) {
                        actionPanelState.onDragStart(ohoGestureState.finger)
                        actionPanelState.ready(button.position, actions)
                        parallelGestureState.cancel()
                        ohoGestureState.cancel()
                    } else if (actions.isNotEmpty()) {
                        parallelGestureState.cancel()
                        if (actions.first().value == GlobalActions.MOVE_SCREEN) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                showVersionTooLowToast(context, R.string.action_move_screen)
                                ohoGestureState.cancel()
                                return@onDrag
                            }
                            moveScreenState.onDragStart(ohoGestureState.finger)
                            ohoGestureState.cancel()
                        } else {
                            curOnAction(actions.first())
                            ohoGestureState.cancel()
                        }
                    }
                } else {
                    ohoGestureState.cancel()
                }
            }
            if (!parallelGestureState.isCanceled) {
                val actions = parallelGestureState.onDrag(dragAmount)
                onParallelActionBlock(actions)
            }
        },
        onDragEnd = onDragEnd@{
            if (actionPanelState.visible) {
                val action = actionPanelState.done()
                actionPanelState.onDragEnd()
                curOnAction(action)
            }
            if (moveScreenState.visible) {
                val action = moveScreenState.done()
                moveScreenState.onDragEnd()
                curOnAction(action)
            }

            if (!ohoGestureState.isCanceled) {
                parallelGestureState.reset()
                val action = ohoGestureState.onDragEnd()
                curOnAction(action)
            } else if (!parallelGestureState.isCanceled) {
                ohoGestureState.reset()
                val action = parallelGestureState.onDragEnd()
                curOnAction(action)
            } else {
                ohoGestureState.reset()
                parallelGestureState.reset()
            }
        },
        onDragCancel = onDragCancel@{
            if (actionPanelState.visible) {
                actionPanelState.onDragCancel()
            }
            if (moveScreenState.visible) {
                moveScreenState.onDragCancel()
            }
            parallelGestureState.onDragCancel()
            ohoGestureState.onDragCancel()
        }
    )
    Box(modifier = modifier) {
        ActionPanel(
            actionPanelStyle = actionPanelStyle,
            actionPanelState = actionPanelState,
            modifier = Modifier.matchParentSize(),
            longPressLaunchPopup = advancedSettings.actionPanelAppLongPressLaunchPopup,
            vibrations = ohoGestureState.button?.vibrations
        )

        if (!moveScreenState.visible && animationStyle != null) {
            GestureAnimation(
                modifier = Modifier.matchParentSize(),
                animationStyle = animationStyle,
                OHOGestureState = ohoGestureState
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
private fun rememberOHOGestureState(
    buttons: List<GestureButton>,
    advancedSettings: AdvancedSettings = AdvancedSettings()
): OHOGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope, buttons, advancedSettings) {
        OHOGestureState(coroutineScope, buttons, advancedSettings)
    }
}

class OHOGestureState(
    private val coroutineScope: CoroutineScope,
    buttons: List<GestureButton>,
    private val advancedSettings: AdvancedSettings = AdvancedSettings()
) : BaseGestureState(buttons) {

    val originXAnimVal: Float get() = originXAnim.value
    val originYAnimVal: Float get() = originYAnim.value
    val fingerXAnimVal: Float get() = fingerXAnim.value
    val fingerYAnimVal: Float get() = fingerYAnim.value
    private val originXAnim = Animatable(Float.NaN)
    private val originYAnim = Animatable(Float.NaN)
    private val fingerXAnim = Animatable(Float.NaN)
    private val fingerYAnim = Animatable(Float.NaN)

    private var longSlideFirstTriggerMs = 0L

    private val animationSpec = spring<Float>(stiffness = 3000f)

    private val stickySlideValue = run {
        val waveStyle = advancedSettings.animationStyles.value as? WaveStyle
        if (waveStyle?.stickySlideEnabled == true) {
            ConvertUtils.dp2px(36f) .toFloat()
        } else 0f
    }

    override fun onDragStart(offset: Offset, imePadding: Int) {
        super.onDragStart(offset, imePadding)
        val button = button ?: return
        coroutineScope.launch {
            originXAnim.snapTo(offset.x)
            originYAnim.snapTo(offset.y)

            when (button.position) {
                Position.Left, Position.Right -> {
                    fingerXAnim.snapTo(getStickySlideValue(button, true))
                    fingerYAnim.snapTo(offset.y)
                }
                Position.Bottom -> {
                    fingerXAnim.snapTo(offset.x)
                    fingerYAnim.snapTo(getStickySlideValue(button, false))
                }
            }
        }
    }

    /**
     * @return 返回null表示不识别任何手势，emptyList()表示还没触发动作，
     * 长列表表示触发长动作，否则表示触发一个动作
     */
    override fun onDrag(dragAmount: Offset): List<Action>? {
        super.onDrag(dragAmount)

        val button = button ?: return null
        val triggerDirection = triggerDirection ?: return null
        coroutineScope.launch {
            fingerXAnim.snapTo(fingerXAnimVal + dragAmount.x)
            fingerYAnim.snapTo(fingerYAnimVal + dragAmount.y)
        }
        if (canDistanceTrigger(button, true)) {
            val longSlideDelayMs = button.longSlideTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (longSlideFirstTriggerMs == 0L) {
                longSlideFirstTriggerMs = timeMs
            } else if (timeMs - longSlideFirstTriggerMs >= longSlideDelayMs) {
                if (button.longSlideTriggerImmediately) {
                    button.vibrations.tryVibrateForLongSlide()
                    // 要触发ActionPanel，longPressTriggerImmediately必须为true
                    return button.longSlideActions.ohoActionsBy(triggerDirection)
                }
            }
        } else {
            longSlideFirstTriggerMs = 0L
        }

        return emptyList()
    }

    override fun onDragEnd(): Action {
        super.onDragEnd()
        val button = button ?: return Action.NONE
        val triggerDirection = triggerDirection ?: return Action.NONE
        val longSlideDelayMs = button.longSlideTriggerDelayMs
        var returnAction = Action.NONE
        if (!button.longSlideTriggerImmediately &&
            canDistanceTrigger(button, true) &&
            SystemClock.uptimeMillis() - longSlideFirstTriggerMs >= longSlideDelayMs
        ) {
            button.vibrations.tryVibrateForLongSlide()
            val actions = button.longSlideActions.ohoActionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null) {
                returnAction = action
            }
        } else if (canDistanceTrigger(button, false)) {
            button.vibrations.tryVibrateForSlide()
            val actions = button.slideActions.ohoActionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null) {
                returnAction = action
            }
        }
        reset()
        return returnAction
    }

    override fun reset() {
        val position = button?.position
        super.reset()
        longSlideFirstTriggerMs = 0L
        position ?: return
        coroutineScope.launch {
            val fingerXAnim = fingerXAnim
            val fingerYAnim = fingerYAnim
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
    public override fun canDistanceTrigger(button: GestureButton, isLongSlide: Boolean): Boolean {
        val slideAction = button.slideActions
        val longSlideAction = button.longSlideActions
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x + getStickySlideValue(button, true)
        val fingerY = finger.y + getStickySlideValue(button, false)
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

    override fun calcDirection(button: GestureButton): TriggerDirection? {
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

    private fun getStickySlideValue(button: GestureButton, isX: Boolean): Float {
        val stickySlideValue = stickySlideValue
        if (isX) {
            return when (button.position) {
                Position.Left -> -stickySlideValue
                else -> stickySlideValue
            }
        }
        return stickySlideValue
    }
}

@Composable
private fun rememberParallelGestureState(
    buttons: List<GestureButton>
): ParallelGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope, buttons) {
        ParallelGestureState(coroutineScope, buttons)
    }
}

class ParallelGestureState(
    private val coroutineScope: CoroutineScope,
    buttons: List<GestureButton>
) : BaseGestureState(buttons) {

    var onLongPress: (List<Action>?) -> Unit = {}

    private var longSlideFirstTriggerMs = 0L
    private var calcLongPressJob: Job? = null

    override fun onDragStart(offset: Offset, imePadding: Int) {
        super.onDragStart(offset, imePadding)
        val button = button ?: return
        val actions = button.slideActions.center2
        if (actions.isEmpty()) return
        calcLongPressJob = coroutineScope.launch {
            delay(ViewConfiguration.getLongPressTimeout().toLong())
            button.vibrations.tryVibrateForSlide()
            onLongPress(button.slideActions.center2)
        }
    }

    override fun onDrag(dragAmount: Offset): List<Action>? {
        calcLongPressJob?.cancel()
        super.onDrag(dragAmount)
        val triggerDirection = triggerDirection ?: return emptyList()
        if (triggerDirection == Center) return null

        val button = button ?: return null
        if (canDistanceTrigger(button, true)) {
            val longSlideDelayMs = button.longSlideTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (longSlideFirstTriggerMs == 0L) {
                longSlideFirstTriggerMs = timeMs
            } else if (timeMs - longSlideFirstTriggerMs >= longSlideDelayMs) {
                if (button.longSlideTriggerImmediately) {
                    button.vibrations.tryVibrateForLongSlide()
                    // 要触发ActionPanel，longPressTriggerImmediately必须为true
                    return button.longSlideActions.parallelActionsBy(triggerDirection)
                }
            }
        } else {
            longSlideFirstTriggerMs = 0L
        }

        return emptyList()
    }

    override fun onDragEnd(): Action {
        super.onDragEnd()
        val button = button ?: return Action.NONE
        val triggerDirection = triggerDirection ?: return Action.NONE
        if (triggerDirection == Center) return Action.NONE

        val longSlideDelayMs = button.longSlideTriggerDelayMs
        var returnAction = Action.NONE
        if (!button.longSlideTriggerImmediately &&
            canDistanceTrigger(button, true) &&
            SystemClock.uptimeMillis() - longSlideFirstTriggerMs >= longSlideDelayMs
        ) {
            button.vibrations.tryVibrateForLongSlide()
            val actions = button.longSlideActions.parallelActionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null) {
                returnAction = action
            }
        } else if (canDistanceTrigger(button, false)) {
            button.vibrations.tryVibrateForSlide()
            val actions = button.slideActions.parallelActionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null) {
                returnAction = action
            }
        }
        reset()
        return returnAction
    }

    override fun reset() {
        super.reset()
        longSlideFirstTriggerMs = 0L
        calcLongPressJob?.cancel()
        calcLongPressJob = null
    }

    override fun canDistanceTrigger(button: GestureButton, isLongSlide: Boolean): Boolean {
        val slideAction = button.slideActions
        val longSlideAction = button.longSlideActions
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x
        val fingerY = finger.y
        val slideDistance = when (button.position) {
            Position.Left, Position.Right -> originY - fingerY
            Position.Bottom -> fingerX - originX
        }
        val triggerDirection = triggerDirection
        if (triggerDirection == Center) {
            return true
        } else if (triggerDirection == Up || triggerDirection == Down) {
            val absDistance = slideDistance.absoluteValue
            if (isLongSlide) {
                val canTrigger = absDistance >= button.longSlideTriggerDistance
                if (triggerDirection == Up) {
                    return canTrigger && longSlideAction.up.isNotEmpty()
                }
                return canTrigger && longSlideAction.down.isNotEmpty()
            }
            val canTrigger = absDistance >= button.slideTriggerDistance
            if (triggerDirection == Up) {
                return canTrigger && slideAction.up.isNotEmpty()
            }
            return canTrigger && slideAction.down.isNotEmpty()
        }
        return false
    }

    override fun calcDirection(button: GestureButton): TriggerDirection? {
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
        return button.angle.getParallelTriggerDirection(degree.toFloat())
    }
}

abstract class BaseGestureState(protected val buttons: List<GestureButton>) {

    var isCanceled: Boolean by mutableStateOf(false)
        protected set

    var button: GestureButton? by mutableStateOf(null)
        protected set
    var triggerDirection: TriggerDirection? by mutableStateOf(null)
        protected set

    var origin = Offset.Unspecified
        protected set
    var finger = Offset.Unspecified
        protected set
    protected var buttonBounds: Rect? = null

    open fun onDragStart(offset: Offset, imePadding: Int) {
        origin = offset
        finger = offset
        button = buttons.find(offset, imePadding)
        buttonBounds = button?.bounds(imePadding)
    }

    open fun onDrag(dragAmount: Offset): List<Action>? {
        finger += dragAmount
        // 理论上能到这里button不应该为空
        val button = button ?: return null
        // 没触发方向，这一轮不再识别手势
        val direction = calcDirection(button)
        triggerDirection = direction
        if (direction == null) {
            return null
        }
        return emptyList()
    }

    open fun onDragEnd(): Action {
        return Action.NONE
    }

    open fun onDragCancel() {
        reset()
    }

    open fun cancel() {
        if (isCanceled) return
        reset()
        isCanceled = true
    }

    open fun reset() {
        isCanceled = false
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        triggerDirection = null
    }

    protected abstract fun canDistanceTrigger(button: GestureButton, isLongSlide: Boolean): Boolean

    protected abstract fun calcDirection(button: GestureButton): TriggerDirection?
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