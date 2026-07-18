package com.aaron.sidegesture.action.handler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ActionRequestProducer
import com.aaron.sidegesture.action.OverlayActionHandler
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.feature.quicklauncher.QuickLauncherPanel
import com.aaron.sidegesture.feature.quicklauncher.QuickLauncherPanelState
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.quickLauncherActionData
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ui.theme.WallpaperAwareSideGestureTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class QuickLauncherActionHandler : OverlayActionHandler, ActionRequestProducer {

    override val supportedActions = setOf(GlobalActions.QUICK_LAUNCHER)

    private val requests = Channel<ActionRequest>(Channel.UNLIMITED)
    override val flow: Flow<ActionRequest> = requests.receiveAsFlow()

    private val state = QuickLauncherPanelState()
    override val touchEnabled: Flow<Boolean> = snapshotFlow { state.visible }

    override suspend fun handle(request: ActionRequest) {
        val data = request.action.quickLauncherActionData ?: return
        if (data.items.isEmpty()) return
        val context = request.actionContext ?: return
        val anchor = context.anchor ?: return
        val edge = context.button?.position ?: Position.Left
        state.show(data.items, anchor, edge)
    }

    override fun onDismiss() {
        state.hide()
    }

    @Composable
    override fun Content() {
        WallpaperAwareSideGestureTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                QuickLauncherPanel(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
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
