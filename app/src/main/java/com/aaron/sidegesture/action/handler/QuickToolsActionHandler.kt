package com.aaron.sidegesture.action.handler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ActionRequestProducer
import com.aaron.sidegesture.action.OverlayActionHandler
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.global.QuickToolsSettings
import com.aaron.sidegesture.feature.quicktools.QuickToolsControlCenter
import com.aaron.sidegesture.feature.quicktools.QuickToolsControlCenterState
import com.aaron.sidegesture.feature.quicktools.QuickToolsExecutor
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ui.theme.WallpaperAwareSideGestureTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class QuickToolsActionHandler(
    private val service: SideGestureService,
    private val settingsStore: ServiceSettingsStore
) : OverlayActionHandler, ActionRequestProducer {

    override val supportedActions = setOf(GlobalActions.QUICK_TOOLS)

    private val requests = Channel<ActionRequest>(Channel.UNLIMITED)
    override val flow: Flow<ActionRequest> = requests.receiveAsFlow()

    private val state = QuickToolsControlCenterState(
        QuickToolsExecutor.brightnessGateway(service)
    )
    private var settings by mutableStateOf<QuickToolsSettings?>(null)
    override val touchEnabled: Flow<Boolean> = snapshotFlow { state.visible }

    override suspend fun handle(request: ActionRequest) {
        val context = request.actionContext ?: return
        val anchor = context.anchor ?: return
        val edge = context.button?.position ?: return
        val snapshot = settingsStore.currentSnapshotOrNull() ?: return
        settings = snapshot.actionSettings.quickTools
        state.show(anchor, edge)
    }

    override fun onDismiss() {
        state.hide()
    }

    @Composable
    override fun Content() {
        WallpaperAwareSideGestureTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                val currentSettings = settings
                if (currentSettings != null) {
                    QuickToolsControlCenter(
                        modifier = Modifier.fillMaxSize(),
                        service = service,
                        settings = currentSettings,
                        state = state,
                        onAction = { action ->
                            requests.trySend(ActionRequest(action))
                        }
                    )
                }
            }
        }
    }
}
