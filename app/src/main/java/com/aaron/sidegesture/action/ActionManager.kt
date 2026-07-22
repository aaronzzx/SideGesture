package com.aaron.sidegesture.action

import com.aaron.sidegesture.entity.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActionManager(
    private val handlers: List<ActionHandler>,
    private val coroutineScope: CoroutineScope
) {

    private val dispatcher = ActionDispatcher(handlers)
    private var started = false
    @Volatile
    private var blocked = false

    fun start() {
        if (started) return
        started = true
        handlers.filterIsInstance<ActionRequestProducer>().forEach { producer ->
            coroutineScope.launch {
                producer.flow.collect(::handle)
            }
        }
    }

    fun submit(request: ActionRequest) {
        if (blocked || request.action == Action.NONE) return
        coroutineScope.launch(Dispatchers.Main.immediate) {
            handle(request)
        }
    }

    suspend fun handle(request: ActionRequest): Boolean {
        if (blocked || request.action == Action.NONE) return false
        return dispatcher.dispatch(request)
    }

    fun setBlocked(blocked: Boolean) {
        this.blocked = blocked
        if (blocked) dismissOverlays()
    }

    fun onForegroundAppChanged(snapshot: ForegroundAppAware.Snapshot) {
        handlers.filterIsInstance<ForegroundAppAware>().forEach { handler ->
            handler.onChange(snapshot)
        }
    }

    fun dismissOverlays() {
        handlers.filterIsInstance<OverlayDismissAware>().forEach { handler ->
            handler.onDismiss()
        }
    }

    fun onConfigurationChanged() {
        handlers.filterIsInstance<ConfigurationAware>().forEach { handler ->
            handler.onConfigurationChanged()
        }
    }
}
