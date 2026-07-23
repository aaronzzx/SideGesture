package com.aaron.sidegesture.feature.toast

import android.view.View
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ui.theme.WallpaperAwareSideGestureTheme
import com.aaron.sidegesture.ui.widget.ComposeToast
import com.aaron.sidegesture.ui.widget.ToastMessage
import com.aaron.sidegesture.ui.widget.observeComposeToastMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ServiceToastOverlayHost(
    private val service: SideGestureService,
    private val scope: CoroutineScope
) {

    private var toastView: View? = null
    private var temporarilyHidden = false
    private val lifecycle = ToastOverlayLifecycle(
        scope = scope,
        messages = observeComposeToastMessages(),
        onShow = ::show,
        onHide = ::hide
    )

    fun start() {
        lifecycle.start()
    }

    fun release() {
        lifecycle.release()
        hide()
        temporarilyHidden = false
    }

    fun setTemporarilyHidden(hidden: Boolean) {
        temporarilyHidden = hidden
        toastView?.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
    }

    private fun show(message: ToastMessage) {
        hide()
        toastView = service.attachComposeOverlay {
            WallpaperAwareSideGestureTheme {
                ComposeToast(messages = flowOf(message))
            }
        }.apply {
            visibility = if (temporarilyHidden) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun hide() {
        toastView?.let(service::removeWindow)
        toastView = null
    }
}

class ToastOverlayLifecycle(
    private val scope: CoroutineScope,
    private val messages: Flow<ToastMessage>,
    private val onShow: (ToastMessage) -> Unit,
    private val onHide: () -> Unit
) {

    private var collectJob: Job? = null

    fun start() {
        if (collectJob != null) return
        collectJob = scope.launch {
            messages.collectLatest { message ->
                onShow(message)
                try {
                    delay(message.durationMillis)
                } finally {
                    onHide()
                }
            }
        }
    }

    fun release() {
        collectJob?.cancel()
        collectJob = null
    }
}
