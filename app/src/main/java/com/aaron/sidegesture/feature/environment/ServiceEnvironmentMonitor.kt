package com.aaron.sidegesture.feature.environment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.event.WallpaperChangedEvent
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ktx.queryIntentActivitiesCompat
import com.aaron.sidegesture.utils.Events
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class ImeWindowState(
    val visible: Boolean = false,
    val padding: Int = 0
)

fun shouldObserveIme(fitSoftKeyboard: Boolean, hideGestureOnIme: Boolean): Boolean {
    return fitSoftKeyboard || hideGestureOnIme
}

fun resolveImeWindowState(
    windowPresent: Boolean,
    boundsTop: Int,
    boundsHeight: Int,
    screenHeight: Int
): ImeWindowState {
    if (!windowPresent) return ImeWindowState()
    val padding = if (boundsHeight > 0 && boundsTop < screenHeight) {
        (screenHeight - boundsTop).coerceAtLeast(0)
    } else {
        0
    }
    return ImeWindowState(visible = true, padding = padding)
}

class ServiceEnvironmentMonitor(
    private val service: SideGestureService,
    private val scope: CoroutineScope,
    private val settingsStore: ServiceSettingsStore,
    private val onScreenLockChanged: (Boolean) -> Unit
) {

    private val imeInsetObserver = ImeInsetObserver()
    val imeWindowState: StateFlow<ImeWindowState> = imeInsetObserver.state

    var isScreenLocked: Boolean = false
        private set

    private var orientation = service.resources.configuration.orientation
    private var screenWidthDp = service.resources.configuration.screenWidthDp
    private var screenHeightDp = service.resources.configuration.screenHeightDp
    private var started = false
    private var imeObservationJob: Job? = null

    private val wallpaperChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Events.post(WallpaperChangedEvent())
        }
    }

    private val screenLockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenLocked = true
                    onScreenLockChanged(true)
                }
                Intent.ACTION_USER_PRESENT -> {
                    isScreenLocked = false
                    onScreenLockChanged(false)
                }
            }
        }
    }

    fun start() {
        if (started) return
        started = true
        service.registerReceiver(
            screenLockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        )
        service.registerReceiver(
            wallpaperChangedReceiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
        )
        imeObservationJob = scope.launch(Dispatchers.Main.immediate) {
            settingsStore.snapshot
                .filterNotNull()
                .map { settings ->
                    shouldObserveIme(
                        fitSoftKeyboard = settings.advancedSettings.fitSoftKeyboard,
                        hideGestureOnIme = settings.advancedSettings.hideGestureOnIme
                    )
                }
                .distinctUntilChanged()
                .collectLatest { observeIme ->
                    if (observeIme) {
                        imeInsetObserver.register()
                    } else {
                        imeInsetObserver.unregister()
                    }
                }
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent): Boolean {
        val affectsWindows = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        if (affectsWindows) imeInsetObserver.recompute()
        return affectsWindows
    }

    fun onConfigurationChanged(config: Configuration): Boolean {
        val changed = orientation != config.orientation ||
            screenWidthDp != config.screenWidthDp ||
            screenHeightDp != config.screenHeightDp
        orientation = config.orientation
        screenWidthDp = config.screenWidthDp
        screenHeightDp = config.screenHeightDp
        if (changed) imeInsetObserver.recompute()
        return changed
    }

    fun currentPackageName(): String {
        return service.rootInActiveWindow?.packageName?.toString().orEmpty()
    }

    fun isLauncherForeground(): Boolean {
        val packageName = currentPackageName()
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return service.packageManager
            .queryIntentActivitiesCompat(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter {
                service.packageManager.getLaunchIntentForPackage(it.activityInfo.packageName.orEmpty()) == null
            }
            .any { it.activityInfo?.packageName == packageName }
    }

    fun stop() {
        imeObservationJob?.cancel()
        imeObservationJob = null
        imeInsetObserver.unregister()
        if (!started) return
        started = false
        runCatching { service.unregisterReceiver(screenLockReceiver) }
        runCatching { service.unregisterReceiver(wallpaperChangedReceiver) }
    }

    private inner class ImeInsetObserver {

        private val _state = MutableStateFlow(ImeWindowState())
        val state: StateFlow<ImeWindowState> = _state.asStateFlow()

        private var enabled = false

        fun register() {
            enabled = true
            recompute()
        }

        fun unregister() {
            enabled = false
            _state.value = ImeWindowState()
        }

        fun recompute() {
            if (!enabled) {
                _state.value = ImeWindowState()
                return
            }
            try {
                val imeWindow = service.windows
                    ?.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                if (imeWindow == null) {
                    _state.value = ImeWindowState()
                    return
                }
                val bounds = Rect()
                imeWindow.getBoundsInScreen(bounds)
                val screenHeight = ScreenUtils.getScreenHeight()
                _state.value = resolveImeWindowState(
                    windowPresent = true,
                    boundsTop = bounds.top,
                    boundsHeight = bounds.height(),
                    screenHeight = screenHeight
                )
            } catch (e: Exception) {
                _state.value = ImeWindowState()
            }
        }
    }
}
