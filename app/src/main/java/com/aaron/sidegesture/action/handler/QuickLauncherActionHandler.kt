package com.aaron.sidegesture.action.handler

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ActionRequestProducer
import com.aaron.sidegesture.action.OverlayDismissAware
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.feature.quicklauncher.QuickLauncherPanel
import com.aaron.sidegesture.feature.quicklauncher.QuickLauncherPanelState
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.quickLauncherActionData
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class QuickLauncherActionHandler(
    private val service: SideGestureService
) : ActionHandler, ActionRequestProducer, OverlayDismissAware {

    override val supportedActions = setOf(GlobalActions.QUICK_LAUNCHER)

    private val requests = Channel<ActionRequest>(Channel.UNLIMITED)
    override val flow: Flow<ActionRequest> = requests.receiveAsFlow()

    private val state = QuickLauncherPanelState()
    private var window: View? = null

    override suspend fun handle(request: ActionRequest) {
        val data = request.action.quickLauncherActionData ?: return
        if (data.items.isEmpty()) return
        val context = request.actionContext ?: return
        val anchor = context.anchor ?: return
        val edge = context.button?.position ?: Position.Left
        ensureWindow()
        state.show(data.items, anchor, edge)
    }

    override fun onDismiss() {
        state.hide()
        window?.let(service::removeWindow)
        window = null
    }

    private fun ensureWindow() {
        if (window != null) return
        window = service.attachComposeOverlay {
            SideGestureTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    QuickLauncherPanel(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        onOverlayTouchChange = ::setTouchEnabled,
                        onLaunch = { action, miniWindow ->
                            val nextAction = when {
                                action.appInfo != null -> action.copy(data =
                                    com.aaron.sidegesture.utils.JsonHelper.encodeToString(
                                        action.appInfo!!.copy(miniWindow = miniWindow)
                                    )
                                )
                                action.shortcutInfo != null -> action.copy(data =
                                    com.aaron.sidegesture.utils.JsonHelper.encodeToString(
                                        action.shortcutInfo!!.copy(miniWindow = miniWindow)
                                    )
                                )
                                else -> action
                            }
                            requests.trySend(ActionRequest(nextAction))
                        }
                    )
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
}
