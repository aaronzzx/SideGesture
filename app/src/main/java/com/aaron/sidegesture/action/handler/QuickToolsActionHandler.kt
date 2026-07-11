package com.aaron.sidegesture.action.handler

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ActionRequestProducer
import com.aaron.sidegesture.action.OverlayDismissAware
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.feature.quicktools.QuickToolsControlCenter
import com.aaron.sidegesture.feature.quicktools.QuickToolsControlCenterState
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class QuickToolsActionHandler(
    private val service: SideGestureService
) : ActionHandler, ActionRequestProducer, OverlayDismissAware {

    override val supportedActions = setOf(GlobalActions.QUICK_TOOLS)

    private val requests = Channel<ActionRequest>(Channel.UNLIMITED)
    override val flow: Flow<ActionRequest> = requests.receiveAsFlow()

    private val state = QuickToolsControlCenterState()
    private var settings by mutableStateOf(
        com.aaron.sidegesture.entity.global.ActionSettings().quickTools
    )
    private var window: View? = null

    override suspend fun handle(request: ActionRequest) {
        val context = request.actionContext ?: return
        val anchor = context.anchor ?: return
        val edge = context.button?.position ?: return
        settings = service.actionSettings?.quickTools
            ?: com.aaron.sidegesture.entity.global.ActionSettings().quickTools
        ensureWindow()
        state.show(anchor, edge)
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
                    QuickToolsControlCenter(
                        modifier = Modifier.fillMaxSize(),
                        service = service,
                        settings = settings,
                        state = state,
                        onOverlayTouchChange = ::setTouchEnabled,
                        onAction = { action ->
                            requests.trySend(ActionRequest(action))
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
