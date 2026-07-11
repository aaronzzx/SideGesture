package com.aaron.sidegesture.action.handler

import android.graphics.Bitmap
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ActionRequestProducer
import com.aaron.sidegesture.action.OverlayDismissAware
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.MoveScreenData
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.feature.movescreen.CrosshairScreen
import com.aaron.sidegesture.feature.movescreen.MoveScreen
import com.aaron.sidegesture.feature.movescreen.MoveScreenState
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.takeScreenshot
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.utils.AccessibilityUtils
import com.aaron.sidegesture.utils.JsonHelper
import com.aaron.sidegesture.utils.MotionEventDispatcher
import com.aaron.sidegesture.utils.OnMotionEventListener
import com.aaron.sidegesture.utils.showToast
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MoveScreenActionHandler internal constructor(
    private val service: SideGestureService,
    private val settingsStore: ServiceSettingsStore
) : ActionHandler, ActionRequestProducer, OverlayDismissAware {

    override val supportedActions = setOf(GlobalActions.MOVE_SCREEN)

    private val requests = Channel<ActionRequest>(Channel.UNLIMITED)
    override val flow: Flow<ActionRequest> = requests.receiveAsFlow()

    private var state: MoveScreenState? by mutableStateOf(null)
    private var screenshot: Bitmap? by mutableStateOf(null)
    private var useCrosshair by mutableStateOf(true)
    private var window: View? = null
    private var lastRawPosition = Offset.Unspecified

    private val motionListener = OnMotionEventListener { event ->
        val current = state ?: return@OnMotionEventListener
        val currentPosition = Offset(event.rawX, event.rawY)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (lastRawPosition != Offset.Unspecified) {
                    current.onDrag(currentPosition - lastRawPosition)
                }
                lastRawPosition = currentPosition
            }
            MotionEvent.ACTION_UP -> {
                requests.trySend(ActionRequest(current.done()))
                onDismiss()
            }
            MotionEvent.ACTION_CANCEL -> onDismiss()
        }
    }

    override suspend fun handle(request: ActionRequest) {
        if (request.action.data.isNotBlank()) {
            performMoveScreenAction(request.action.data)
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            showVersionTooLowToast(service, R.string.action_move_screen)
            return
        }
        if (!settingsStore.gestureSettings.value.longSlideTriggerImmediately) {
            showToast(R.string.move_screen_disabled_cause_long_slide_trigger_immediately)
            return
        }
        val anchor = request.actionContext?.anchor ?: return
        val gestureSettings = settingsStore.gestureSettings.value
        val moveScreenSettings = settingsStore.actionSettings.value.moveScreen
        onDismiss()
        val newState = MoveScreenState(gestureSettings, moveScreenSettings, service.coroutineScope)
        state = newState
        lastRawPosition = anchor
        useCrosshair = moveScreenSettings.style == ActionSettings.MoveScreen.Style.Crosshair ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        ensureWindow()
        setTouchEnabled(false)
        MotionEventDispatcher.addOnMotionEventListener(motionListener)
        newState.onDragStart(anchor)
        if (!useCrosshair) {
            val captured = service.takeScreenshot()
            if (state === newState) {
                screenshot = captured
            } else {
                captured?.recycle()
            }
        }
    }

    override fun onDismiss() {
        MotionEventDispatcher.removeOnMotionEventListener(motionListener)
        state?.onDragCancel()
        state = null
        screenshot?.recycle()
        screenshot = null
        useCrosshair = true
        lastRawPosition = Offset.Unspecified
        window?.let(service::removeWindow)
        window = null
    }

    private fun ensureWindow() {
        if (window != null) return
        window = service.attachComposeOverlay {
            SideGestureTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val currentState = state
                    if (currentState != null) {
                        val bitmap = screenshot
                        if (useCrosshair) {
                            CrosshairScreen(currentState, Modifier.fillMaxSize())
                        } else if (bitmap != null) {
                            MoveScreen(bitmap, currentState, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }

    private fun setTouchEnabled(enabled: Boolean) {
        val view = window ?: return
        val params = (view.layoutParams as WindowManager.LayoutParams).apply {
            setFlags(enabled)
        }
        service.updateLayout(view, params)
    }

    private fun performMoveScreenAction(data: String) {
        val moveScreenData = runCatching {
            JsonHelper.decodeFromString<MoveScreenData>(data)
        }.getOrNull() ?: return
        if (moveScreenData.x !in 0..ScreenUtils.getScreenWidth() ||
            moveScreenData.y !in 0..ScreenUtils.getScreenHeight()
        ) {
            return
        }
        when (moveScreenData.action) {
            ActionSettings.MoveScreen.Action.LongPress -> AccessibilityUtils.longPress(
                service,
                moveScreenData.x,
                moveScreenData.y
            )
            ActionSettings.MoveScreen.Action.DoubleTap -> AccessibilityUtils.doubleTap(
                service,
                moveScreenData.x,
                moveScreenData.y
            )
            ActionSettings.MoveScreen.Action.Tap -> AccessibilityUtils.click(
                service,
                moveScreenData.x,
                moveScreenData.y
            )
            else -> Unit
        }
    }
}
