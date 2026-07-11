package com.aaron.sidegesture.feature.gesture

import com.aaron.sidegesture.feature.actionpanel.ActionPanel
import com.aaron.sidegesture.feature.actionpanel.rememberActionPanelState
import com.aaron.sidegesture.feature.gesture.animation.GestureAnimation
import com.aaron.sidegesture.feature.movescreen.CrosshairScreen
import com.aaron.sidegesture.feature.movescreen.MoveScreen
import com.aaron.sidegesture.feature.movescreen.rememberMoveScreenState
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.constant.GlobalActions
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
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.ktx.GESTURE_ANGLE_BASE
import com.aaron.sidegesture.ktx.actionsBy
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.find
import com.aaron.sidegesture.ktx.getTriggerDirection
import com.aaron.sidegesture.ktx.isEmptyOrNone
import com.aaron.sidegesture.ktx.launchAppInfo
import com.aaron.sidegesture.ktx.launchShortcutInfo
import com.aaron.sidegesture.ktx.quickLauncherActionData
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ktx.takeScreenshot
import com.aaron.sidegesture.ktx.tryVibrateForLongSlide
import com.aaron.sidegesture.ktx.tryVibrateForSlide
import com.aaron.sidegesture.feature.quicklauncher.QuickLauncherPanel
import com.aaron.sidegesture.feature.quicklauncher.rememberQuickLauncherPanelState
import com.aaron.sidegesture.feature.quicktools.QuickToolsControlCenter
import com.aaron.sidegesture.feature.quicktools.rememberQuickToolsControlCenterState
import com.aaron.sidegesture.feature.screenshot.ScreenshotCropper
import com.aaron.sidegesture.feature.screenshot.ScreenshotStorage
import com.aaron.sidegesture.feature.screenshot.SmartScreenshotEditor
import com.aaron.sidegesture.feature.screenshot.SmartScreenshotState
import com.aaron.sidegesture.feature.taskswitcher.TaskSwitcherPanel
import com.aaron.sidegesture.feature.taskswitcher.rememberTaskSwitcherPanelState
import com.aaron.sidegesture.utils.DragGestureHandler
import com.aaron.sidegesture.utils.showToast
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    actionPanelStyle: ActionPanelStyle = FolderStyle(),
    actionSettings: ActionSettings = ActionSettings(),
    advancedSettings: AdvancedSettings = AdvancedSettings(),
    gestureSettings: GestureSettings = GestureSettings(),
    taskSwitcherLockedPackages: Set<String> = emptySet(),
    onOverlayTouchChange: (Boolean) -> Unit = {},
    overlaysDismissSignal: Int = 0
) {
    val context = LocalContext.current
    val curOnAction by rememberUpdatedState(newValue = onAction)
    val sideGestureState = rememberSideGestureState(buttons, gestureSettings)
    val actionPanelState = rememberActionPanelState(
        windowModeSwitchDelayMs = advancedSettings.actionPanelAppSwitchWindowModeDelayMs
    )
    val moveScreenState = rememberMoveScreenState(gestureSettings, actionSettings.moveScreen)
    // 有效样式：用户选了准星，或系统低于 Android 11 无法截屏 → 一律走准星(低版本兜底)
    val moveScreenCrosshair = actionSettings.moveScreen.style == ActionSettings.MoveScreen.Style.Crosshair ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    val quickToolsState = rememberQuickToolsControlCenterState()
    val quickLauncherState = rememberQuickLauncherPanelState()
    val taskSwitcherState = rememberTaskSwitcherPanelState()
    val smartScreenshotState = remember { SmartScreenshotState() }
    val coroutineScope = rememberCoroutineScope()
    var taskSwitcherQueryJob by remember { mutableStateOf<Job?>(null) }


    LaunchedEffect(overlaysDismissSignal) {
        if (overlaysDismissSignal != 0) {
            //region 快速工具
            quickToolsState.hide()
            //endregion

            //region 快速启动器
            quickLauncherState.hide()
            //endregion

            //region 任务切换器
            taskSwitcherQueryJob?.cancel()
            taskSwitcherState.hide()
            //endregion

            //region 智能截图
            smartScreenshotState.dismiss()
            smartScreenshotState.cancelCapture()
            onOverlayTouchChange(false)
            //endregion
        }
    }

    fun startSmartScreenshotCapture() {
        quickToolsState.hide()
        smartScreenshotState.startCapture()
    }

    fun handleAction(
        action: Action,
        finger: Offset,
        position: Position?,
        button: GestureButton? = null
    ) {
        if (action == Action.NONE) {
            return
        }
        if (action.value != GlobalActions.TASK_SWITCHER) {
            taskSwitcherQueryJob?.cancel()
            taskSwitcherState.hide()
        }
        if (action.value == GlobalActions.HIDE_GESTURE_BUTTON) {
            (context as SideGestureService).hideGestureButton(button)
            return
        }
        if (action.value == GlobalActions.SMART_SCREENSHOT) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                showVersionTooLowToast(context, R.string.action_smart_screenshot)
                sideGestureState.cancel()
                return
            }
            startSmartScreenshotCapture()
            return
        }
        if (action.value == GlobalActions.TASK_SWITCHER) {
            taskSwitcherQueryJob?.cancel()
            taskSwitcherState.hide()
            taskSwitcherQueryJob = coroutineScope.launch {
                val tasks = (context as SideGestureService).queryRecentTasks()
                if (tasks.isNotEmpty()) {
                    taskSwitcherState.show(tasks, finger, position ?: Position.Left)
                }
            }
            return
        }
        if (action.value == GlobalActions.QUICK_TOOLS && position != null) {
            quickToolsState.show(finger, position)
            return
        }
        if (action.value == GlobalActions.QUICK_LAUNCHER) {
            val data = action.quickLauncherActionData
            if (data != null && data.items.isNotEmpty()) {
                quickLauncherState.show(data.items, finger, position ?: Position.Left)
            }
            return
        }
        curOnAction(action)
    }

    SideEffect {
        sideGestureState.onLongPress = { action ->
            handleAction(
                action = action,
                finger = sideGestureState.finger,
                position = sideGestureState.button?.position,
                button = sideGestureState.button
            )
            sideGestureState.cancel()
        }
    }

    DragGestureHandler(
        onDragStart = onDragStart@{ offset ->
            quickLauncherState.hide()
            taskSwitcherQueryJob?.cancel()
            taskSwitcherState.hide()
            quickToolsState.hide()
            if (smartScreenshotState.visible) {
                smartScreenshotState.dismiss()
                onOverlayTouchChange(false)
            }
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
                val button = sideGestureState.button
                if (button != null && actions != null) {
                    if (actions.size > 1) {
                        actionPanelState.onDragStart(sideGestureState.finger)
                        actionPanelState.ready(button, actions)
                        sideGestureState.cancel()
                    } else if (actions.isNotEmpty()) {
                        if (actions.first().value == GlobalActions.MOVE_SCREEN) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                showVersionTooLowToast(context, R.string.action_move_screen)
                                sideGestureState.cancel()
                                return@onDrag
                            }
                            moveScreenState.onDragStart(sideGestureState.finger)
                            sideGestureState.cancel()
                        } else {
                            handleAction(
                                actions.first(),
                                sideGestureState.finger,
                                button.position,
                                button = sideGestureState.button
                            )
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
                val actionPanelFinger = actionPanelState.finger
                val actionPanelPosition = actionPanelState.position
                val actionPanelButton = actionPanelState.button
                val action = actionPanelState.done(
                    advancedSettings.actionPanelAppLongPressLaunchPopup
                )
                actionPanelState.onDragEnd()
                handleAction(
                    action = action,
                    finger = actionPanelFinger,
                    position = actionPanelPosition,
                    button = actionPanelButton
                )
            }
            if (moveScreenState.visible) {
                val action = moveScreenState.done()
                moveScreenState.onDragEnd()
                curOnAction(action)
            }

            if (!sideGestureState.isCanceled) {
                val sideGestureButton = sideGestureState.button
                val sideGestureFinger = sideGestureState.finger
                val sideGesturePosition = sideGestureButton?.position
                val action = sideGestureState.onDragEnd()
                handleAction(
                    action = action,
                    finger = sideGestureFinger,
                    position = sideGesturePosition,
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
            if (moveScreenState.visible) {
                moveScreenState.onDragCancel()
            }
            sideGestureState.onDragCancel()
        }
    )
    Box(modifier = modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && smartScreenshotState.isCapturing) {
            LaunchedEffect(smartScreenshotState.isCapturing) {
                val service = context as SideGestureService
                delay(500)
                val screenshot = service.takeScreenshot()
                if (screenshot == null) {
                    smartScreenshotState.cancelCapture()
                    showToast(R.string.screenshot_capture_failed)
                } else {
                    onOverlayTouchChange(true)
                    smartScreenshotState.show(screenshot, ConvertUtils.dp2px(96f))
                }
            }
        }

        if (moveScreenState.visible) {
            if (moveScreenCrosshair) {
                CrosshairScreen(
                    modifier = Modifier.matchParentSize(),
                    state = moveScreenState
                )
            } else {
                // 放大镜样式(需 Android 11+ 截屏)
                val moveScreenScreenshotState = produceState<Bitmap?>(null) {
                    val service = context as SideGestureService
                    value = service.takeScreenshot()
                }
                val screenshot = moveScreenScreenshotState.value
                if (screenshot != null) {
                    MoveScreen(
                        modifier = Modifier.matchParentSize(),
                        screenshot = screenshot,
                        state = moveScreenState
                    )
                }
            }
        }

        val screenshot = smartScreenshotState.screenshot
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            smartScreenshotState.visible &&
            screenshot != null
        ) {
            SmartScreenshotEditor(
                modifier = Modifier.matchParentSize(),
                bitmap = screenshot,
                state = smartScreenshotState,
                onCancel = {
                    smartScreenshotState.dismiss()
                    onOverlayTouchChange(false)
                },
                onSave = {
                    coroutineScope.launch {
                        val output = withContext(Dispatchers.Default) {
                            ScreenshotCropper.crop(
                                bitmap = screenshot,
                                selectionRect = smartScreenshotState.selectionRect,
                                shape = smartScreenshotState.shape
                            )
                        }
                        val saved = withContext(Dispatchers.IO) {
                            ScreenshotStorage.saveToGallery(context, output)
                        }
                        if (saved != null) {
                            showToast(R.string.screenshot_save_success)
                        } else {
                            showToast(R.string.screenshot_save_failed)
                        }
                        output.recycle()
                    }
                },
                onCopy = {
                    coroutineScope.launch {
                        val output = withContext(Dispatchers.Default) {
                            ScreenshotCropper.crop(
                                bitmap = screenshot,
                                selectionRect = smartScreenshotState.selectionRect,
                                shape = smartScreenshotState.shape
                            )
                        }
                        val uri = withContext(Dispatchers.IO) {
                            ScreenshotStorage.createClipboardUri(context, output)
                        }
                        val copied = if (uri == null) {
                            false
                        } else {
                            withContext(Dispatchers.Main) {
                                ScreenshotStorage.copyToClipboard(context, uri)
                            }
                        }
                        if (copied) {
                            showToast(R.string.screenshot_copy_success)
                        } else {
                            showToast(R.string.screenshot_copy_failed)
                        }
                        output.recycle()
                    }
                },
                onShare = {
                    coroutineScope.launch {
                        val output = withContext(Dispatchers.Default) {
                            ScreenshotCropper.crop(
                                bitmap = screenshot,
                                selectionRect = smartScreenshotState.selectionRect,
                                shape = smartScreenshotState.shape
                            )
                        }
                        smartScreenshotState.dismiss()
                        onOverlayTouchChange(false)
                        val uri = withContext(Dispatchers.IO) {
                            ScreenshotStorage.createShareUri(context, output)
                        }
                        val shared = uri != null && ScreenshotStorage.share(context, uri)
                        if (!shared) {
                            showToast(R.string.screenshot_share_failed)
                        }
                        output.recycle()
                    }
                },
                onPin = {
                    val output = ScreenshotCropper.crop(
                        bitmap = screenshot,
                        selectionRect = smartScreenshotState.selectionRect,
                        shape = smartScreenshotState.shape
                    )
                    (context as SideGestureService).pinnedScreenshotManager.pin(
                        bitmap = output,
                        buttons = buttons,
                        sourceRect = smartScreenshotState.selectionRect
                    )
                    smartScreenshotState.dismiss()
                    onOverlayTouchChange(false)
                }
            )
        }

        QuickToolsControlCenter(
            modifier = Modifier.matchParentSize(),
            service = context as SideGestureService,
            settings = actionSettings.quickTools,
            state = quickToolsState,
            onOverlayTouchChange = onOverlayTouchChange
        )

        QuickLauncherPanel(
            modifier = Modifier.matchParentSize(),
            state = quickLauncherState,
            onOverlayTouchChange = onOverlayTouchChange,
            onLaunch = { action, miniWindow ->
                val appInfo = action.appInfo
                val shortcutInfo = action.shortcutInfo
                if (appInfo != null) {
                    (context as SideGestureService).launchAppInfo(appInfo, miniWindow, actionSettings.miniWindow)
                } else if (shortcutInfo != null) {
                    (context as SideGestureService).launchShortcutInfo(shortcutInfo, miniWindow, actionSettings.miniWindow)
                }
            }
        )

        TaskSwitcherPanel(
            modifier = Modifier.matchParentSize(),
            state = taskSwitcherState,
            lockedPackageNames = taskSwitcherLockedPackages,
            onOverlayTouchChange = onOverlayTouchChange,
            onLaunch = { task ->
                (context as SideGestureService).switchToRecentTask(task.packageName)
                taskSwitcherState.hide()
            },
            onClose = { task ->
                coroutineScope.launch {
                    val success = (context as SideGestureService).closeRecentTask(task.packageName)
                    if (success) {
                        taskSwitcherState.remove(task)
                    }
                }
            },
            onToggleLock = { packageName ->
                (context as SideGestureService).toggleTaskSwitcherLock(packageName)
            },
            onCloseAll = { tasks ->
                coroutineScope.launch {
                    val closedPackages = tasks
                        .map { it.packageName }
                        .distinct()
                        .filter { packageName ->
                            (context as SideGestureService).closeRecentTask(packageName)
                        }
                        .toSet()
                    taskSwitcherState.removePackages(closedPackages)
                }
            }
        )

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
    return remember(coroutineScope, buttons, gestureSettings) {
        SideGestureState(coroutineScope, buttons, gestureSettings)
    }
}

class SideGestureState(
    private val coroutineScope: CoroutineScope,
    private val buttons: List<GestureButton>,
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
    private var downTime = 0L
    private var hasMovedBeyondSlop = false

    private val animationSpec = spring<Float>(stiffness = 3000f)

    /**
     * 区分上下滑和侧滑，当可以触发侧滑时，即使后面触发方向变成上下滑也需要取消手势
     */
    private var isOhoGestureEverCanTriggered = false

    private var slideVibrationFlags = false

    private val viewConfiguration = ViewConfiguration.get(App.getContext())

    fun onDragStart(offset: Offset, imePadding: Int) {
        downTime = SystemClock.uptimeMillis()
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

        val touchSlop = viewConfiguration.scaledTouchSlop
        val minus = finger - origin
        if (minus.x.absoluteValue > touchSlop || minus.y.absoluteValue > touchSlop) {
            hasMovedBeyondSlop = true
            if (calcLongPressJob?.isActive == true) {
                calcLongPressJob?.cancel()
            }
        }

        // 理论上能到这里button不应该为空
        val button = button ?: return null
        // 没触发方向，这一轮不再识别手势
        val newDirection = calcDirection(button) ?: return null
        triggerDirection = newDirection

        val gestureSettings = gestureSettings
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
        if (returnAction == Action.NONE &&
            !hasMovedBeyondSlop &&
            SystemClock.uptimeMillis() - downTime <= gestureSettings.longPressTriggerDelayMs
        ) {
            val clickAction = button.slideActions.click.firstOrNull()
            if (clickAction != null && clickAction != Action.NONE) {
                gestureSettings.vibrations.tryVibrateForSlide()
                returnAction = clickAction
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
        hasMovedBeyondSlop = false
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
                canDistanceTriggered && longSlideAction.center.isEmptyOrNone().not()
            } else {
                canDistanceTriggered && slideAction.center.isEmptyOrNone().not()
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
