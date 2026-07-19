package com.aaron.sidegesture.action.handler

import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ActionRequestProducer
import com.aaron.sidegesture.action.ConfigurationAware
import com.aaron.sidegesture.action.OverlayActionHandler
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.MoveScreenData
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.feature.movescreen.CrosshairScreen
import com.aaron.sidegesture.feature.movescreen.MoveScreenState
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ktx.tryVibrateForMoveScreen
import com.aaron.sidegesture.ui.theme.WallpaperAwareSideGestureTheme
import com.aaron.sidegesture.utils.AccessibilityUtils
import com.aaron.sidegesture.utils.JsonHelper
import com.aaron.sidegesture.utils.MotionEventDispatcher
import com.aaron.sidegesture.utils.OnMotionEventListener
import com.aaron.sidegesture.utils.showToast
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow

class MoveScreenActionHandler(
    private val service: SideGestureService,
    private val settingsStore: ServiceSettingsStore,
    private val scope: CoroutineScope
) : OverlayActionHandler, ActionRequestProducer, ConfigurationAware {

    override val supportedActions = setOf(GlobalActions.MOVE_SCREEN)
    override val touchEnabled: Flow<Boolean> = flowOf(false)

    private val requests = Channel<ActionRequest>(Channel.UNLIMITED)
    override val flow: Flow<ActionRequest> = requests.receiveAsFlow()

    private var state: MoveScreenState? by mutableStateOf(null)
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
                if (lastRawPosition != Offset.Unspecified) {
                    val finalDragAmount = currentPosition - lastRawPosition
                    if (finalDragAmount != Offset.Zero) {
                        current.onDrag(finalDragAmount)
                    }
                }
                requests.trySend(ActionRequest(current.done()))
                onDismiss()
            }
            MotionEvent.ACTION_CANCEL -> onDismiss()
        }
    }

    override suspend fun handle(request: ActionRequest) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            showVersionTooLowToast(service, R.string.action_move_screen)
            return
        }
        if (request.action.data.isNotBlank()) {
            performMoveScreenAction(request.action.data)
            return
        }
        val settings = settingsStore.currentSnapshotOrNull() ?: return
        if (!settings.gestureSettings.longSlideTriggerImmediately) {
            showToast(R.string.move_screen_disabled_cause_long_slide_trigger_immediately)
            return
        }
        val anchor = request.actionContext?.anchor ?: return
        val gestureSettings = settings.gestureSettings
        val moveScreenSettings = settings.actionSettings.moveScreen
        onDismiss()
        val newState = MoveScreenState(
            actionSettings = moveScreenSettings,
            coroutineScope = scope,
            onActionSelected = {
                gestureSettings.vibrations.tryVibrateForMoveScreen()
            }
        )
        state = newState
        lastRawPosition = anchor
        MotionEventDispatcher.addOnMotionEventListener(motionListener)
        newState.onDragStart(anchor)
    }

    override fun onDismiss() {
        MotionEventDispatcher.removeOnMotionEventListener(motionListener)
        state?.onDragCancel()
        state = null
        lastRawPosition = Offset.Unspecified
    }

    override fun onConfigurationChanged() {
        onDismiss()
    }

    @Composable
    override fun Content() {
        WallpaperAwareSideGestureTheme {
            val currentState = state
            if (currentState != null) {
                CrosshairScreen(currentState, Modifier.fillMaxSize())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
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
