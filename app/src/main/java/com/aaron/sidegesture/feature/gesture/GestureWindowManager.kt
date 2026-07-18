package com.aaron.sidegesture.feature.gesture

import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.postDelayed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.feature.environment.ServiceEnvironmentMonitor
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.updateGestureButton
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import com.aaron.sidegesture.ui.theme.WallpaperAwareSideGestureTheme
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GestureWindowManager(
    private val service: SideGestureService,
    private val scope: CoroutineScope,
    private val settingsStore: ServiceSettingsStore,
    private val environmentMonitor: ServiceEnvironmentMonitor,
    private val actionOverlayTouchEnabled: StateFlow<Boolean>,
    private val onActionRequest: (ActionRequest) -> Unit,
    private val onDismissActionOverlays: () -> Unit,
    private val onButtonsChanged: (List<GestureButton>) -> Unit
) {

    private companion object {
        const val HIDE_GESTURE_BUTTON_DURATION_MS = 1000L
    }

    private var mainView: View? = null
    private var buttonViews: List<View> = emptyList()
    private val hiddenButtonUntil = mutableMapOf<String, Long>()

    private var buttonsJob: Job? = null
    private var visibilityJob: Job? = null
    private var visibilityRefreshScheduled = false
    private val visibilityRefreshFrameCallback = Choreographer.FrameCallback {
        visibilityRefreshScheduled = false
        if (mainView != null) refreshVisibility()
    }

    fun startOrReattach() {
        cancelPendingVisibilityRefresh()
        mainView?.let(service::removeWindow)
        mainView = attachMainWindow()
        if (buttonsJob == null) {
            buttonsJob = scope.launch(Dispatchers.Main.immediate) {
                settingsStore.snapshot
                    .filterNotNull()
                    .map { it.buttons }
                    .distinctUntilChanged()
                    .collectLatest(::replaceButtons)
            }
        }
        if (visibilityJob == null) {
            visibilityJob = scope.launch(Dispatchers.Main.immediate) {
                combine(
                    settingsStore.snapshot
                        .filterNotNull()
                        .map { it.initialSettings to it.advancedSettings }
                        .distinctUntilChanged(),
                    environmentMonitor.imePadding,
                    actionOverlayTouchEnabled
                ) { _, _, _ -> Unit }.collectLatest {
                    refreshVisibility()
                }
            }
        }
    }

    fun requestVisibilityRefresh() {
        if (mainView == null || visibilityRefreshScheduled) return
        visibilityRefreshScheduled = true
        Choreographer.getInstance().postFrameCallback(visibilityRefreshFrameCallback)
    }

    fun refreshVisibility() {
        val settings = settingsStore.currentSnapshotOrNull() ?: return
        val initialSettings = settings.initialSettings
        val advancedSettings = settings.advancedSettings
        buttonViews.forEach { view ->
            val button = view.tag as? GestureButton ?: return@forEach
            val params = (view.layoutParams as WindowManager.LayoutParams).apply {
                updateGestureButton(button)
                if (button.position != Position.Bottom) {
                    y -= environmentMonitor.imePadding.value
                }
                val touchEnabled = when {
                    actionOverlayTouchEnabled.value -> false
                    isButtonHidden(button) -> false
                    !initialSettings.gestureEnabled -> false
                    advancedSettings.hideLandscape && ScreenUtils.isLandscape() -> false
                    advancedSettings.hideHomeScreen && environmentMonitor.isLauncherForeground() -> false
                    advancedSettings.hideScreenLock && environmentMonitor.isScreenLocked -> false
                    environmentMonitor.currentPackageName() in advancedSettings.excludeApps -> false
                    else -> button.enabled
                }
                setFlags(touchEnabled)
            }
            service.updateLayout(view, params)
        }
    }

    fun onConfigurationChanged() {
        mainView?.let { view ->
            val params = (view.layoutParams as WindowManager.LayoutParams).apply {
                updateMainView()
            }
            service.updateLayout(view, params)
        }
        refreshVisibility()
    }

    fun setTemporarilyHidden(hidden: Boolean) {
        mainView?.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
    }

    fun hide(button: GestureButton?) {
        val until = SystemClock.uptimeMillis() + HIDE_GESTURE_BUTTON_DURATION_MS
        buttonViews.forEach { view ->
            val tag = view.tag as? GestureButton ?: return@forEach
            val matched = button == null || tag.id == button.id && tag.position == button.position
            if (!matched) return@forEach

            hiddenButtonUntil[buttonKey(tag)] = until
            val params = view.layoutParams as WindowManager.LayoutParams
            params.setFlags(false)
            service.updateLayout(view, params)
            view.postDelayed(HIDE_GESTURE_BUTTON_DURATION_MS) {
                refreshVisibility()
            }
        }
    }

    fun release() {
        cancelPendingVisibilityRefresh()
        buttonsJob?.cancel()
        buttonsJob = null
        visibilityJob?.cancel()
        visibilityJob = null
        mainView?.let(service::removeWindow)
        mainView = null
        service.removeWindows(buttonViews)
        buttonViews = emptyList()
        hiddenButtonUntil.clear()
    }

    private fun attachMainWindow(): View = service.attachComposeOverlay {
        WallpaperAwareSideGestureTheme {
            val settings by settingsStore.snapshot.collectAsStateWithLifecycle()
            val currentSettings = settings ?: return@WallpaperAwareSideGestureTheme
            Box(modifier = Modifier.fillMaxSize()) {
                val currentImePadding by environmentMonitor.imePadding.collectAsStateWithLifecycle()
                SideGestureContainer(
                    modifier = Modifier.matchParentSize(),
                    buttons = currentSettings.buttons,
                    imePadding = currentImePadding,
                    animationStyle = if (currentSettings.advancedSettings.animationStyles.isAnimationEnabled) {
                        currentSettings.advancedSettings.animationStyles.value
                    } else {
                        null
                    },
                    actionPanelStyle = currentSettings.advancedSettings.actionPanelStyles.value,
                    onActionRequest = onActionRequest,
                    onDismissOverlays = onDismissActionOverlays,
                    advancedSettings = currentSettings.advancedSettings,
                    gestureSettings = currentSettings.gestureSettings
                )
            }
        }
    }

    private fun replaceButtons(buttons: List<GestureButton>) {
        service.removeWindows(buttonViews)
        buttonViews = service.attachGestureButtons(buttons)
        onButtonsChanged(buttons)
        refreshVisibility()
    }

    private fun cancelPendingVisibilityRefresh() {
        if (!visibilityRefreshScheduled) return
        Choreographer.getInstance().removeFrameCallback(visibilityRefreshFrameCallback)
        visibilityRefreshScheduled = false
    }

    private fun buttonKey(button: GestureButton): String = "${button.id}|${button.position}"

    private fun isButtonHidden(button: GestureButton): Boolean {
        val key = buttonKey(button)
        val until = hiddenButtonUntil[key] ?: return false
        if (SystemClock.uptimeMillis() < until) return true
        hiddenButtonUntil.remove(key)
        return false
    }
}
