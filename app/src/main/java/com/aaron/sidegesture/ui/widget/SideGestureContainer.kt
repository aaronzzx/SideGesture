package com.aaron.sidegesture.ui.widget

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
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
import com.aaron.sidegesture.entity.TriggerDirection.Center2
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Down2
import com.aaron.sidegesture.entity.TriggerDirection.Up
import com.aaron.sidegesture.entity.TriggerDirection.Up2
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.ktx.GESTURE_ANGLE_BASE
import com.aaron.sidegesture.ktx.actionsBy
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getTriggerDirection
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
    advancedSettings: AdvancedSettings = AdvancedSettings(),
    gestureSettings: GestureSettings = GestureSettings()
) {
    val context = LocalContext.current
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val sideGestureState = rememberSideGestureState(buttons, advancedSettings, gestureSettings)
    val actionPanelState = rememberActionPanelState()
    val moveScreenState = rememberMoveScreenState(actionSettings.moveScreen.rate)

    SideEffect {
        sideGestureState.onLongPress = { action ->
            curOnAction(action)
            sideGestureState.cancel()
        }
    }

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
            if (!sideGestureState.isCanceled) {
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

            if (!sideGestureState.isCanceled) {
                val action = sideGestureState.onDragEnd()
                curOnAction(action)
            } else {
                sideGestureState.reset()
            }
        },
        onDragCancel = onDragCancel@{
            if (actionPanelState.visible) {
                actionPanelState.onDragCancel()
            }
            if (moveScreenState.visible) {
                moveScreenState.onDragCancel()
            }
            sideGestureState.onDragCancel()
        }
    )
    Box(modifier = modifier) {
        ActionPanel(
            actionPanelStyle = actionPanelStyle,
            actionPanelState = actionPanelState,
            modifier = Modifier.matchParentSize(),
            longPressLaunchPopup = advancedSettings.actionPanelAppLongPressLaunchPopup,
            vibrations = gestureSettings.vibrations
        )

        if (!moveScreenState.visible && animationStyle != null) {
            GestureAnimation(
                modifier = Modifier.matchParentSize(),
                animationStyle = animationStyle,
                SideGestureState = sideGestureState
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
private fun rememberSideGestureState(
    buttons: List<GestureButton>,
    advancedSettings: AdvancedSettings = AdvancedSettings(),
    gestureSettings: GestureSettings = GestureSettings()
): SideGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope, buttons, advancedSettings, gestureSettings) {
        SideGestureState(coroutineScope, buttons, advancedSettings, gestureSettings)
    }
}

class SideGestureState(
    private val coroutineScope: CoroutineScope,
    private val buttons: List<GestureButton>,
    private val advancedSettings: AdvancedSettings = AdvancedSettings(),
    private val gestureSettings: GestureSettings = GestureSettings()
) {

    var isCanceled: Boolean by mutableStateOf(false)
        private set

    var button: GestureButton? by mutableStateOf(null)
        private set
    var triggerDirection: TriggerDirection by mutableStateOf(Center2)
        private set

    var origin = Offset.Unspecified
        private set
    var finger = Offset.Unspecified
        private set
    private var buttonBounds: Rect? = null

    val originXAnimVal: Float get() = originXAnim.value
    val originYAnimVal: Float get() = originYAnim.value
    val fingerXAnimVal: Float get() = fingerXAnim.value
    val fingerYAnimVal: Float get() = fingerYAnim.value
    private val originXAnim = Animatable(Float.NaN)
    private val originYAnim = Animatable(Float.NaN)
    private val fingerXAnim = Animatable(Float.NaN)
    private val fingerYAnim = Animatable(Float.NaN)

    var onLongPress: (Action) -> Unit = {}

    private var longSlideFirstTriggerMs = 0L
    private var calcLongPressJob: Job? = null

    private val animationSpec = spring<Float>(stiffness = 3000f)

    private val stickySlideValue = run {
        val waveStyle = advancedSettings.animationStyles.value as? WaveStyle
        if (waveStyle?.stickySlideEnabled == true) {
            ConvertUtils.dp2px(36f) .toFloat()
        } else 0f
    }

    /**
     * 区分上下滑和侧滑，当可以触发侧滑时，即使后面触发方向变成上下滑也需要取消手势
     */
    private var isOhoGestureEverCanTriggered = false

    private var slideVibrationFlags = false

    fun onDragStart(offset: Offset, imePadding: Int) {
        origin = offset
        finger = offset
        button = buttons.find(offset, imePadding)
        buttonBounds = button?.bounds(imePadding)

        val button = button ?: return
        val gestureSettings = gestureSettings

        val action = button.slideActions.center2.firstOrNull()
        if (action != null && action != Action.NONE) {
            calcLongPressJob = coroutineScope.launch {
                delay(gestureSettings.longPressTriggerDelayMs)
                gestureSettings.vibrations.tryVibrateForSlide()
                onLongPress(action)
            }
        }

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
    fun onDrag(dragAmount: Offset): List<Action>? {
        val gestureSettings = gestureSettings
        calcLongPressJob?.cancel()
        finger += dragAmount
        // 理论上能到这里button不应该为空
        val button = button ?: return null
        // 没触发方向，这一轮不再识别手势
        val newDirection = calcDirection(button) ?: return null
        triggerDirection = newDirection

        if (gestureSettings.isPreciseSlideType) {
            if (newDirection == Center || newDirection == Up || newDirection == Down) {
                if (!isOhoGestureEverCanTriggered) {
                    isOhoGestureEverCanTriggered = canDistanceTriggered(button, isLongSlide = false, judgeAction = false)
                }
            } else if (isOhoGestureEverCanTriggered) {
                return null
            }
        }

        coroutineScope.launch {
            fingerXAnim.snapTo(fingerXAnimVal + dragAmount.x)
            fingerYAnim.snapTo(fingerYAnimVal + dragAmount.y)
        }

        if (gestureSettings.vibrations.vibrateImmediately) {
            if (!slideVibrationFlags && canDistanceTriggered(button, false)) {
                slideVibrationFlags = true
                gestureSettings.vibrations.tryVibrateForSlide()
            }
        }
        if (canDistanceTriggered(button, true)) {
            val longSlideDelayMs = gestureSettings.longSlideTriggerDelayMs
            val timeMs = SystemClock.uptimeMillis()
            if (longSlideFirstTriggerMs == 0L) {
                longSlideFirstTriggerMs = timeMs
            } else if (timeMs - longSlideFirstTriggerMs >= longSlideDelayMs) {
                val actions = button.longSlideActions.actionsBy(newDirection)
                if (gestureSettings.longSlideTriggerImmediately) {
                    gestureSettings.vibrations.tryVibrateForLongSlide()
                    // 要触发ActionPanel，longPressTriggerImmediately必须为true
                    return actions
                }
            }
        } else {
            longSlideFirstTriggerMs = 0L
        }

        return emptyList()
    }

    fun onDragEnd(): Action {
        calcLongPressJob?.cancel()
        val button = button ?: return Action.NONE
        val gestureSettings = gestureSettings
        val triggerDirection = triggerDirection
        val longSlideDelayMs = gestureSettings.longSlideTriggerDelayMs
        var returnAction = Action.NONE
        if (!gestureSettings.longSlideTriggerImmediately &&
            canDistanceTriggered(button, true) &&
            SystemClock.uptimeMillis() - longSlideFirstTriggerMs >= longSlideDelayMs
        ) {
            val actions = button.longSlideActions.actionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null && action != Action.NONE) {
                gestureSettings.vibrations.tryVibrateForLongSlide()
                returnAction = action
            }
        } else if (canDistanceTriggered(button, false)) {
            val actions = button.slideActions.actionsBy(triggerDirection)
            val action = actions.firstOrNull()
            if (action != null && action != Action.NONE) {
                if (!slideVibrationFlags) {
                    gestureSettings.vibrations.tryVibrateForSlide()
                }
                returnAction = action
            }
        }
        reset()
        return returnAction
    }

    fun onDragCancel() {
        reset()
    }

    fun cancel() {
        if (isCanceled) return
        reset()
        isCanceled = true
    }

    fun reset() {
        calcLongPressJob?.cancel()
        calcLongPressJob = null
        isCanceled = false
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        longSlideFirstTriggerMs = 0L
        isOhoGestureEverCanTriggered = false
        slideVibrationFlags = false

        val position = button?.position ?: return
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
    fun canDistanceTriggered(
        button: GestureButton,
        isLongSlide: Boolean,
        judgeAction: Boolean = true
    ): Boolean {
        val gestureSettings = gestureSettings
        val slideAction = button.slideActions
        val longSlideAction = button.longSlideActions
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x + getStickySlideValue(button, true)
        val fingerY = finger.y + getStickySlideValue(button, false)
        val triggerDirection = triggerDirection

        // 长按直接返回
        if (triggerDirection == Center2) {
            return false
        }

        val slideDistance = if (triggerDirection == Up2 || triggerDirection == Down2) {
            when (button.position) {
                Position.Left, Position.Right -> originY - fingerY
                Position.Bottom -> fingerX - originX
            }
        } else {
            when (button.position) {
                Position.Left -> fingerX - originX
                Position.Right -> originX - fingerX
                Position.Bottom -> originY - fingerY
            }
        }
        // 解决触钮往回滑还能触发的问题
        if (slideDistance < 0 &&
            triggerDirection != Up2 &&
            triggerDirection != Down2
        ) {
            return false
        }

        var canDistanceTriggered = false
        if (triggerDirection == Center) {
            canDistanceTriggered = if (isLongSlide) {
                slideDistance >= gestureSettings.longSlideTriggerDistance
            } else {
                slideDistance >= gestureSettings.slideTriggerDistance
            }
            if (!judgeAction) {
                return canDistanceTriggered
            }
            return if (isLongSlide) {
                canDistanceTriggered && longSlideAction.center.isNotEmpty()
            } else {
                canDistanceTriggered && slideAction.center.isNotEmpty()
            }
        } else if (triggerDirection == Up || triggerDirection == Down) {
            // 需要计算斜边
            val edge1 = slideDistance
            val edge2 = when (button.position) {
                Position.Left, Position.Right -> abs(fingerY - originY)
                Position.Bottom -> abs(fingerX - originX)
            }
            val hypot = hypot(edge1, edge2)
            canDistanceTriggered = if (isLongSlide) {
                hypot >= gestureSettings.longSlideTriggerDistance
            } else {
                hypot >= gestureSettings.slideTriggerDistance
            }
            if (!judgeAction) {
                return canDistanceTriggered
            }
            return if (isLongSlide) {
                if (triggerDirection == Up) {
                    canDistanceTriggered && longSlideAction.up.isNotEmpty()
                } else {
                    canDistanceTriggered && longSlideAction.down.isNotEmpty()
                }
            } else { // Slide
                if (triggerDirection == Up) {
                    canDistanceTriggered && slideAction.up.isNotEmpty()
                } else { // Down
                    canDistanceTriggered && slideAction.down.isNotEmpty()
                }
            }
        } else if (triggerDirection == Up2 || triggerDirection == Down2) {
            val absDistance = slideDistance.absoluteValue
            canDistanceTriggered = if (isLongSlide) {
                absDistance >= gestureSettings.longSlideTriggerDistance
            } else {
                absDistance >= gestureSettings.slideTriggerDistance
            }
            if (!judgeAction) {
                return canDistanceTriggered
            }
            return if (isLongSlide) {
                if (triggerDirection == Up2) {
                    canDistanceTriggered && longSlideAction.up2.isNotEmpty()
                } else { // Down2
                    canDistanceTriggered && longSlideAction.down2.isNotEmpty()
                }
            } else { // Slide
                if (triggerDirection == Up2) {
                    canDistanceTriggered && slideAction.up2.isNotEmpty()
                } else { // Down2
                    canDistanceTriggered && slideAction.down2.isNotEmpty()
                }
            }
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
        val angle = when (button.position) {
            Position.Left -> gestureSettings.angles.left
            Position.Right -> gestureSettings.angles.right
            Position.Bottom -> gestureSettings.angles.bottom
        }
        return angle.getTriggerDirection(degree.toFloat())
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