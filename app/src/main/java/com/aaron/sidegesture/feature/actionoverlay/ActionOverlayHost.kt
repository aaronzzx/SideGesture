package com.aaron.sidegesture.feature.actionoverlay

import android.view.View
import android.view.WindowManager
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.OverlayActionHandler
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActionOverlayHost(
    private val service: SideGestureService,
    private val scope: CoroutineScope
) {

    private val entries = mutableMapOf<OverlayActionHandler, Entry>()
    private val handlerTouchStates = mutableMapOf<OverlayActionHandler, Boolean>()
    private val _touchEnabled = MutableStateFlow(false)
    val touchEnabled: StateFlow<Boolean> = _touchEnabled.asStateFlow()
    private var temporarilyHidden = false

    fun attach(handlers: Collection<OverlayActionHandler>) {
        handlers.forEach { handler ->
            if (handler in entries) return@forEach

            val view = service.attachComposeOverlay {
                handler.Content()
            }.apply {
                visibility = if (temporarilyHidden) View.INVISIBLE else View.VISIBLE
            }
            val touchJob = scope.launch {
                handler.touchEnabled.collect { enabled ->
                    handlerTouchStates[handler] = enabled
                    _touchEnabled.value = handlerTouchStates.values.any { it }
                    val params = (view.layoutParams as WindowManager.LayoutParams).apply {
                        setFlags(enabled)
                    }
                    service.updateLayout(view, params)
                }
            }
            entries[handler] = Entry(view, touchJob)
        }
    }

    fun onConfigurationChanged() {
        entries.values.forEach { entry ->
            val params = (entry.view.layoutParams as WindowManager.LayoutParams).apply {
                updateMainView()
            }
            service.updateLayout(entry.view, params)
        }
    }

    fun setTemporarilyHidden(hidden: Boolean) {
        if (temporarilyHidden == hidden) return
        temporarilyHidden = hidden
        val visibility = if (hidden) View.INVISIBLE else View.VISIBLE
        entries.values.forEach { entry ->
            entry.view.visibility = visibility
        }
    }

    fun release() {
        entries.values.forEach { entry ->
            entry.touchJob.cancel()
            service.removeWindow(entry.view)
        }
        entries.clear()
        handlerTouchStates.clear()
        _touchEnabled.value = false
        temporarilyHidden = false
    }

    private data class Entry(
        val view: View,
        val touchJob: Job
    )
}
