package com.aaron.sidegesture.feature.gesture

import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.postDelayed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.event.WallpaperChangedEvent
import com.aaron.sidegesture.feature.environment.ServiceEnvironmentMonitor
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ktx.SubscribeEvent
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.updateGestureButton
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class GestureWindowManager(
    private val service: SideGestureService,
    private val scope: CoroutineScope,
    private val settingsStore: ServiceSettingsStore,
    private val environmentMonitor: ServiceEnvironmentMonitor,
    private val onActionRequest: (ActionRequest) -> Unit,
    private val onDismissActionOverlays: () -> Unit,
    private val onButtonsChanged: (List<GestureButton>) -> Unit
) {

    private var mainView: View? = null
    private var buttonViews: List<View> = emptyList()
    private var currentButtons: List<GestureButton> = emptyList()
    private val hiddenButtonUntil = mutableMapOf<String, Long>()

    private var buttonsJob: Job? = null
    private var visibilityJob: Job? = null

    fun startOrReattach() {
        mainView?.let(service::removeWindow)
        mainView = attachMainWindow()
        if (buttonsJob == null) {
            buttonsJob = scope.launch(Dispatchers.Main.immediate) {
                settingsStore.buttons.collectLatest(::replaceButtons)
            }
        }
        if (visibilityJob == null) {
            visibilityJob = scope.launch(Dispatchers.Main.immediate) {
                combine(
                    settingsStore.initialSettings,
                    settingsStore.advancedSettings,
                    environmentMonitor.imePadding
                ) { _, _, _ -> Unit }.collectLatest {
                    refreshVisibility()
                }
            }
        }
    }

    fun refreshVisibility() {
        val initialSettings = settingsStore.initialSettings.value
        val advancedSettings = settingsStore.advancedSettings.value
        buttonViews.forEach { view ->
            val button = view.tag as? GestureButton ?: return@forEach
            val params = (view.layoutParams as WindowManager.LayoutParams).apply {
                updateGestureButton(button)
                if (button.position != Position.Bottom) {
                    y -= environmentMonitor.imePadding.value
                }
                val touchEnabled = when {
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
        buttonsJob?.cancel()
        buttonsJob = null
        visibilityJob?.cancel()
        visibilityJob = null
        mainView?.let(service::removeWindow)
        mainView = null
        service.removeWindows(buttonViews)
        buttonViews = emptyList()
        currentButtons = emptyList()
        hiddenButtonUntil.clear()
    }

    private fun attachMainWindow(): View = service.attachComposeOverlay {
        var key by remember { mutableStateOf(Any()) }
        SubscribeEvent(eventClass = WallpaperChangedEvent::class) {
            key = Any()
        }
        key(key) {
            SideGestureTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val buttons by settingsStore.buttons.collectAsStateWithLifecycle()
                    val advancedSettings by settingsStore.advancedSettings.collectAsStateWithLifecycle()
                    val gestureSettings by settingsStore.gestureSettings.collectAsStateWithLifecycle()
                    val currentImePadding by environmentMonitor.imePadding.collectAsStateWithLifecycle()
                    SideGestureContainer(
                        modifier = Modifier.matchParentSize(),
                        buttons = buttons,
                        imePadding = currentImePadding,
                        animationStyle = if (advancedSettings.animationStyles.isAnimationEnabled) {
                            advancedSettings.animationStyles.value
                        } else {
                            null
                        },
                        actionPanelStyle = advancedSettings.actionPanelStyles.value,
                        onActionRequest = onActionRequest,
                        onDismissOverlays = onDismissActionOverlays,
                        advancedSettings = advancedSettings,
                        gestureSettings = gestureSettings
                    )
                }
            }
        }
    }

    private fun replaceButtons(buttons: List<GestureButton>) {
        currentButtons = buttons
        service.removeWindows(buttonViews)
        buttonViews = service.attachGestureButtons(buttons)
        onButtonsChanged(buttons)
        refreshVisibility()
    }

    private fun buttonKey(button: GestureButton): String = "${button.id}|${button.position}"

    private fun isButtonHidden(button: GestureButton): Boolean {
        val key = buttonKey(button)
        val until = hiddenButtonUntil[key] ?: return false
        if (SystemClock.uptimeMillis() < until) return true
        hiddenButtonUntil.remove(key)
        return false
    }

    private companion object {
        const val HIDE_GESTURE_BUTTON_DURATION_MS = 1000L
    }
}
