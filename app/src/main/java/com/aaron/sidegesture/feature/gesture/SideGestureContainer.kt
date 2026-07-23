package com.aaron.sidegesture.feature.gesture

import com.aaron.sidegesture.feature.actionpanel.ActionPanel
import com.aaron.sidegesture.feature.actionpanel.rememberActionPanelState
import com.aaron.sidegesture.feature.gesture.animation.GestureAnimation
import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.aaron.sidegesture.App
import com.aaron.sidegesture.action.ActionContext
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.AnimationStyle
import com.aaron.sidegesture.entity.FolderStyle
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.entity.TriggerDirection.Center
import com.aaron.sidegesture.entity.TriggerDirection.Center2
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Down2
import com.aaron.sidegesture.entity.TriggerDirection.Up
import com.aaron.sidegesture.entity.TriggerDirection.Up2
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.ktx.GESTURE_ANGLE_BASE
import com.aaron.sidegesture.ktx.actionsBy
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getTriggerDirection
import com.aaron.sidegesture.ktx.isEmptyOrNone
import com.aaron.sidegesture.ktx.tryVibrateForLongSlide
import com.aaron.sidegesture.ktx.tryVibrateForSlide
import com.aaron.sidegesture.utils.DragGestureHandler
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
    onActionRequest: (ActionRequest) -> Unit,
    onDismissOverlays: () -> Unit,
    buttons: List<GestureButton>,
    modifier: Modifier = Modifier,
    imePadding: Int = 0,
    animationStyle: AnimationStyle? = WaveStyle(),
    actionPanelStyle: ActionPanelStyle = FolderStyle(),
    advancedSettings: AdvancedSettings = AdvancedSettings(),
    gestureSettings: GestureSettings = GestureSettings()
) {
    val curOnActionRequest by rememberUpdatedState(newValue = onActionRequest)
    val curOnDismissOverlays by rememberUpdatedState(newValue = onDismissOverlays)
    val sideGestureState = rememberSideGestureState(buttons, gestureSettings)
    val actionPanelState = rememberActionPanelState(
        windowModeSwitchDelayMs = advancedSettings.actionPanelAppSwitchWindowModeDelayMs
    )
    fun submitAction(
        action: Action,
        finger: Offset,
        button: GestureButton? = null
    ) {
        if (action == Action.NONE) return
        curOnActionRequest(
            ActionRequest(
                action = action,
                actionContext = ActionContext(anchor = finger, button = button)
            )
        )
    }

    SideEffect {
        sideGestureState.onTapDispatch = { action, finger, button ->
            submitAction(
                action = action,
                finger = finger,
                button = button
            )
        }
        sideGestureState.onLongPress = { action ->
            submitAction(
                action = action,
                finger = sideGestureState.finger,
                button = sideGestureState.button
            )
            sideGestureState.cancel()
        }
    }

    DragGestureHandler(
        onDragStart = onDragStart@{ offset ->
            curOnDismissOverlays()
            sideGestureState.onDragStart(offset, imePadding)
        },
        onDrag = onDrag@{ dragAmount ->
            if (actionPanelState.visible) {
                actionPanelState.onDrag(dragAmount)
                return@onDrag
            }
            if (!sideGestureState.isCanceled) {
                val actions = sideGestureState.onDrag(dragAmount)
                val button = sideGestureState.button
                if (button != null && actions != null) {
                    if (actions.size > 1) {
                        actionPanelState.onDragStart(sideGestureState.finger)
                        actionPanelState.ready(button, actions)
                        sideGestureState.cancel()
                    } else if (actions.isNotEmpty()) {
                        submitAction(
                            actions.first(),
                            sideGestureState.finger,
                            button = sideGestureState.button
                        )
                        sideGestureState.cancel()
                    }
                } else {
                    sideGestureState.cancel()
                }
            }
        },
        onDragEnd = onDragEnd@{
            if (actionPanelState.visible) {
                val actionPanelFinger = actionPanelState.finger
                val actionPanelButton = actionPanelState.button
                val action = actionPanelState.done(
                    advancedSettings.actionPanelAppLongPressLaunchPopup
                )
                actionPanelState.onDragEnd()
                submitAction(
                    action = action,
                    finger = actionPanelFinger,
                    button = actionPanelButton
                )
            }

            if (!sideGestureState.isCanceled) {
                val sideGestureButton = sideGestureState.button
                val sideGestureFinger = sideGestureState.finger
                val action = sideGestureState.onDragEnd()
                submitAction(
                    action = action,
                    finger = sideGestureFinger,
                    button = sideGestureButton
                )
            } else {
                sideGestureState.reset()
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
            actionPanelStyle = actionPanelStyle,
            actionPanelState = actionPanelState,
            modifier = Modifier.matchParentSize(),
            longPressLaunchPopup = advancedSettings.actionPanelAppLongPressLaunchPopup,
            vibrations = gestureSettings.vibrations
        )

        if (animationStyle != null) {
            GestureAnimation(
                modifier = Modifier.matchParentSize(),
                animationStyle = animationStyle,
                sideGestureState = sideGestureState
            )
        }
    }
}

@Composable
private fun rememberSideGestureState(
    buttons: List<GestureButton>,
    gestureSettings: GestureSettings = GestureSettings()
): SideGestureState {
    val coroutineScope = rememberCoroutineScope()
    val state = remember(coroutineScope) {
        SideGestureState(coroutineScope, buttons, gestureSettings)
    }
    SideEffect {
        state.updateConfiguration(buttons, gestureSettings)
    }
    DisposableEffect(state) {
        onDispose(state::release)
    }
    return state
}

class SideGestureState(
    private val coroutineScope: CoroutineScope,
    buttons: List<GestureButton>,
    gestureSettings: GestureSettings = GestureSettings()
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
    var onTapDispatch: (Action, Offset, GestureButton) -> Unit = { _, _, _ -> }

    private var latestButtons = buttons
    private var latestGestureSettings = gestureSettings
    private var activeGestureSettings = gestureSettings
    private var longSlideFirstTriggerMs = 0L
    private var calcLongPressJob: Job? = null
    private var doubleTapTimeoutJob: Job? = null
    private var downTime = 0L
    private var hasMovedBeyondSlop = false
    private var isSecondTapCandidate = false

    private val animationSpec = spring<Float>(stiffness = 3000f)

    /**
     * 区分上下滑和侧滑，当可以触发侧滑时，即使后面触发方向变成上下滑也需要取消手势
     */
    private var isOhoGestureEverCanTriggered = false

    private var slideVibrationFlags = false

    private val viewConfiguration = ViewConfiguration.get(App.getContext())
    private val doubleTapStateMachine = DoubleTapStateMachine<TapDispatch>(
        timeoutMillis = ViewConfiguration.getDoubleTapTimeout().toLong(),
        doubleTapSlop = viewConfiguration.scaledDoubleTapSlop.toFloat()
    )

    fun updateConfiguration(
        buttons: List<GestureButton>,
        gestureSettings: GestureSettings
    ) {
        latestButtons = buttons
        latestGestureSettings = gestureSettings
    }

    fun onDragStart(offset: Offset, imePadding: Int) {
        downTime = SystemClock.uptimeMillis()
        activeGestureSettings = latestGestureSettings
        origin = offset
        finger = offset
        button = latestButtons.find(offset, imePadding)
        buttonBounds = button?.bounds(imePadding)

        val downResult = doubleTapStateMachine.onDown(
            buttonKey = button?.let(::buttonKey),
            downX = offset.x,
            downY = offset.y,
            downTimeMillis = downTime
        )
        when (downResult.resolution) {
            DoubleTapStateMachine.DownResolution.NoPending -> Unit
            DoubleTapStateMachine.DownResolution.Matched -> {
                doubleTapTimeoutJob?.cancel()
                doubleTapTimeoutJob = null
                isSecondTapCandidate = true
            }
            DoubleTapStateMachine.DownResolution.Rejected -> {
                doubleTapTimeoutJob?.cancel()
                doubleTapTimeoutJob = null
                isSecondTapCandidate = false
            }
            DoubleTapStateMachine.DownResolution.Expired -> {
                doubleTapTimeoutJob?.cancel()
                doubleTapTimeoutJob = null
                isSecondTapCandidate = false
                downResult.expiredSingleTap?.let(::dispatchTap)
            }
        }

        val button = button ?: return
        val gestureSettings = activeGestureSettings

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
                    fingerXAnim.snapTo(0f)
                    fingerYAnim.snapTo(offset.y)
                }
                Position.Bottom, Position.Top -> {
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

        val touchSlop = viewConfiguration.scaledTouchSlop
        val minus = finger - origin
        if (minus.x.absoluteValue > touchSlop || minus.y.absoluteValue > touchSlop) {
            hasMovedBeyondSlop = true
            cancelPendingDoubleTap()
            if (calcLongPressJob?.isActive == true) {
                calcLongPressJob?.cancel()
            }
        }

        // 理论上能到这里button不应该为空
        val button = button ?: return null
        // 没触发方向，这一轮不再识别手势
        val newDirection = calcDirection(button) ?: return null
        triggerDirection = newDirection

        val gestureSettings = activeGestureSettings
        if (gestureSettings.isPreciseSlideType) {
            if (newDirection == Center) {
                if (!isOhoGestureEverCanTriggered) {
                    isOhoGestureEverCanTriggered = canDistanceTriggered(button, isLongSlide = false, judgeAction = false)
                }
            } else if (isOhoGestureEverCanTriggered &&
                (newDirection == Up2 || newDirection == Down2)
            ) {
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
        val button = button ?: run {
            cancelPendingDoubleTap()
            resetCurrent()
            return Action.NONE
        }
        val gestureSettings = activeGestureSettings
        val triggerDirection = triggerDirection
        val longSlideDelayMs = gestureSettings.longSlideTriggerDelayMs
        val upTime = SystemClock.uptimeMillis()
        var returnAction = Action.NONE
        if (!gestureSettings.longSlideTriggerImmediately &&
            canDistanceTriggered(button, true) &&
            upTime - longSlideFirstTriggerMs >= longSlideDelayMs
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
        if (returnAction == Action.NONE &&
            !hasMovedBeyondSlop &&
            upTime - downTime <= gestureSettings.longPressTriggerDelayMs
        ) {
            if (isSecondTapCandidate) {
                val doubleTapDispatch = doubleTapStateMachine.completeSecondTap(upTime)
                isSecondTapCandidate = false
                if (doubleTapDispatch != null) {
                    dispatchTap(doubleTapDispatch)
                    resetCurrent()
                    return Action.NONE
                }
            }
            val clickAction = button.slideActions.click.firstOrNull() ?: Action.NONE
            val doubleClickAction = button.slideActions.doubleClick.firstOrNull {
                it != Action.NONE
            }
            if (doubleClickAction != null) {
                beginDoubleTapWait(
                    button = button,
                    down = origin,
                    up = finger,
                    upTimeMillis = upTime,
                    singleTapAction = clickAction,
                    doubleTapAction = doubleClickAction,
                    vibrations = gestureSettings.vibrations
                )
                resetCurrent()
                return Action.NONE
            }
            if (clickAction != Action.NONE) {
                gestureSettings.vibrations.tryVibrateForSlide()
                returnAction = clickAction
            }
        }
        cancelPendingDoubleTap()
        resetCurrent()
        return returnAction
    }

    fun onDragCancel() {
        reset()
    }

    fun cancel() {
        if (isCanceled) return
        cancelPendingDoubleTap()
        resetCurrent()
        isCanceled = true
    }

    fun reset() {
        cancelPendingDoubleTap()
        resetCurrent()
    }

    fun release() {
        calcLongPressJob?.cancel()
        calcLongPressJob = null
        cancelPendingDoubleTap()
    }

    private fun beginDoubleTapWait(
        button: GestureButton,
        down: Offset,
        up: Offset,
        upTimeMillis: Long,
        singleTapAction: Action,
        doubleTapAction: Action,
        vibrations: Vibrations
    ) {
        doubleTapTimeoutJob?.cancel()
        val token = doubleTapStateMachine.begin(
            buttonKey = buttonKey(button),
            downX = down.x,
            downY = down.y,
            upTimeMillis = upTimeMillis,
            singleTap = TapDispatch(singleTapAction, up, button, vibrations),
            doubleTap = TapDispatch(doubleTapAction, up, button, vibrations)
        )
        doubleTapTimeoutJob = coroutineScope.launch {
            delay(doubleTapStateMachine.timeoutMillis)
            val tapDispatch = doubleTapStateMachine.consumeTimeout(token) ?: return@launch
            doubleTapTimeoutJob = null
            dispatchTap(tapDispatch)
        }
    }

    private fun cancelPendingDoubleTap() {
        doubleTapTimeoutJob?.cancel()
        doubleTapTimeoutJob = null
        doubleTapStateMachine.cancel()
        isSecondTapCandidate = false
    }

    private fun dispatchTap(tapDispatch: TapDispatch) {
        if (tapDispatch.action == Action.NONE) return
        tapDispatch.vibrations.tryVibrateForSlide()
        onTapDispatch(tapDispatch.action, tapDispatch.finger, tapDispatch.button)
    }

    private fun buttonKey(button: GestureButton): String {
        return "${button.id}|${button.position}"
    }

    private fun resetCurrent() {
        calcLongPressJob?.cancel()
        calcLongPressJob = null
        isCanceled = false
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        longSlideFirstTriggerMs = 0L
        hasMovedBeyondSlop = false
        isSecondTapCandidate = false
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
                    Position.Bottom, Position.Top -> {
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
        val gestureSettings = activeGestureSettings
        val slideAction = button.slideActions
        val longSlideAction = button.longSlideActions
        val originX = origin.x
        val originY = origin.y
        val fingerX = finger.x
        val fingerY = finger.y
        val triggerDirection = triggerDirection

        // 长按直接返回
        if (triggerDirection == Center2) {
            return false
        }

        val slideDistance = if (triggerDirection == Up2 || triggerDirection == Down2) {
            when (button.position) {
                Position.Left, Position.Right -> originY - fingerY
                Position.Bottom, Position.Top -> fingerX - originX
            }
        } else {
            when (button.position) {
                Position.Left -> fingerX - originX
                Position.Right -> originX - fingerX
                Position.Bottom -> originY - fingerY
                Position.Top -> fingerY - originY
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
                canDistanceTriggered && longSlideAction.center.isEmptyOrNone().not()
            } else {
                canDistanceTriggered && slideAction.center.isEmptyOrNone().not()
            }
        } else if (triggerDirection == Up || triggerDirection == Down) {
            // 需要计算斜边
            val edge1 = slideDistance
            val edge2 = when (button.position) {
                Position.Left, Position.Right -> abs(fingerY - originY)
                Position.Bottom, Position.Top -> abs(fingerX - originX)
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
                    canDistanceTriggered && longSlideAction.up.isEmptyOrNone().not()
                } else {
                    canDistanceTriggered && longSlideAction.down.isEmptyOrNone().not()
                }
            } else { // Slide
                if (triggerDirection == Up) {
                    canDistanceTriggered && slideAction.up.isEmptyOrNone().not()
                } else { // Down
                    canDistanceTriggered && slideAction.down.isEmptyOrNone().not()
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
                    canDistanceTriggered && longSlideAction.up2.isEmptyOrNone().not()
                } else { // Down2
                    canDistanceTriggered && longSlideAction.down2.isEmptyOrNone().not()
                }
            } else { // Slide
                if (triggerDirection == Up2) {
                    canDistanceTriggered && slideAction.up2.isEmptyOrNone().not()
                } else { // Down2
                    canDistanceTriggered && slideAction.down2.isEmptyOrNone().not()
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
            Position.Top -> finger.y - buttonBounds.top
        }
        val neighbor = when (button.position) {
            Position.Left, Position.Right -> abs(finger.y - origin.y)
            Position.Bottom, Position.Top -> abs(finger.x - origin.x)
        }
        val tanVal = opposite / neighbor
        val radians = atan(tanVal)
        val isPreviousArea = when (button.position) {
            Position.Left, Position.Right -> finger.y < origin.y
            Position.Bottom, Position.Top -> finger.x < origin.x
        }
        val degree = if (isPreviousArea) {
            // 上半区
            Math.toDegrees(radians.toDouble())
        } else {
            // 下半区
            GESTURE_ANGLE_BASE - Math.toDegrees(radians.toDouble())
        }
        val angle = when (button.position) {
            Position.Left -> activeGestureSettings.angles.left
            Position.Right -> activeGestureSettings.angles.right
            Position.Bottom -> activeGestureSettings.angles.bottom
            Position.Top -> activeGestureSettings.angles.top
        }
        return angle.getTriggerDirection(degree.toFloat())
    }

    private data class TapDispatch(
        val action: Action,
        val finger: Offset,
        val button: GestureButton,
        val vibrations: Vibrations
    )
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
