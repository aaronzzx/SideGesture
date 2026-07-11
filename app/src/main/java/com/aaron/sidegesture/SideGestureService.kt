package com.aaron.sidegesture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MEDIA_NEXT
import android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
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
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.action.ActionManager
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ForegroundAppAware
import com.aaron.sidegesture.action.handler.AppActionHandler
import com.aaron.sidegesture.action.handler.DeviceActionHandler
import com.aaron.sidegesture.action.handler.HideGestureButtonActionHandler
import com.aaron.sidegesture.action.handler.MediaActionHandler
import com.aaron.sidegesture.action.handler.MoveScreenActionHandler
import com.aaron.sidegesture.action.handler.PaymentActionHandler
import com.aaron.sidegesture.action.handler.QuickLauncherActionHandler
import com.aaron.sidegesture.action.handler.QuickToolsActionHandler
import com.aaron.sidegesture.action.handler.ScrollActionHandler
import com.aaron.sidegesture.action.handler.ShellActionHandler
import com.aaron.sidegesture.action.handler.SmartScreenshotActionHandler
import com.aaron.sidegesture.action.handler.SystemActionHandler
import com.aaron.sidegesture.action.handler.TaskSwitcherActionHandler
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.event.WallpaperChangedEvent
import com.aaron.sidegesture.ktx.SubscribeEvent
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.dispatchMediaKeyEvent
import com.aaron.sidegesture.ktx.queryIntentActivitiesCompat
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.updateGestureButton
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import com.aaron.sidegesture.ktx.volumeDown
import com.aaron.sidegesture.ktx.volumeUp
import com.aaron.sidegesture.feature.screenshot.PinnedScreenshotManager
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.feature.gesture.GestureWindowManager
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.feature.gesture.SideGestureContainer
import com.aaron.sidegesture.utils.Events
import com.aaron.sidegesture.feature.update.UpdateNotifications
import com.aaron.sidegesture.feature.update.UpdateRepository
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private val imeInsetObserver = ImeInsetObserver()
    private var orientation = if (ScreenUtils.isLandscape()) 2 else 1
    private var screenWidthDp = Resources.getSystem().configuration.screenWidthDp
    private var screenHeightDp = Resources.getSystem().configuration.screenHeightDp

    private var isNowInLockScreenPage = false
    private var volumeButtonSwitchSongJob: Job? = null
    private val _taskSwitcherLockedPackages = MutableStateFlow(emptySet<String>())

    val coroutineScope = MainScope()
    private val settingsStore = ServiceSettingsStore(coroutineScope)
    val taskSwitcherLockedPackages: StateFlow<Set<String>> = _taskSwitcherLockedPackages.asStateFlow()
    val pinnedScreenshotManager: PinnedScreenshotManager by lazy { PinnedScreenshotManager(this) }

    private val gestureWindowManager: GestureWindowManager by lazy {
        GestureWindowManager(
            service = this,
            scope = coroutineScope,
            settingsStore = settingsStore,
            imePadding = imeInsetObserver.flow,
            isScreenLocked = { isNowInLockScreenPage },
            isLauncherForeground = ::nowInLauncher,
            currentPackageName = ::getCurrentPackageName,
            onActionRequest = { actionManager.submit(it) },
            onDismissActionOverlays = { actionManager.dismissOverlays() },
            onButtonsChanged = { pinnedScreenshotManager.onEnvironmentChanged(it) }
        )
    }

    private val actionManager: ActionManager by lazy {
        ActionManager(
            handlers = listOf(
                SystemActionHandler(this),
                MediaActionHandler(this),
                DeviceActionHandler(this),
                AppActionHandler(this, settingsStore),
                PaymentActionHandler(this),
                ScrollActionHandler(this, settingsStore),
                ShellActionHandler(this),
                HideGestureButtonActionHandler(gestureWindowManager),
                TaskSwitcherActionHandler(this),
                QuickLauncherActionHandler(this),
                QuickToolsActionHandler(this, settingsStore),
                SmartScreenshotActionHandler(this),
                MoveScreenActionHandler(this, settingsStore)
            ),
            coroutineScope = coroutineScope
        )
    }

    private val wallpaperChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Events.post(WallpaperChangedEvent())
        }
    }
    private val screenLockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                isNowInLockScreenPage = true
                dismissActionOverlays()
                pinnedScreenshotManager.setScreenLocked(true)
            } else if (intent?.action == Intent.ACTION_USER_PRESENT) {
                isNowInLockScreenPage = false
                pinnedScreenshotManager.setScreenLocked(false)
            }
            gestureWindowManager.refreshVisibility()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientationChanged = orientation != newConfig.orientation
        val screenSizeChanged = screenWidthDp != newConfig.screenWidthDp
                || screenHeightDp != newConfig.screenHeightDp
        if (orientationChanged || screenSizeChanged) {
            orientation = newConfig.orientation
            screenWidthDp = newConfig.screenWidthDp
            screenHeightDp = newConfig.screenHeightDp
            actionManager.dismissOverlays()
            gestureWindowManager.onConfigurationChanged()
            pinnedScreenshotManager.onEnvironmentChanged(settingsStore.buttons.value)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            actionManager.onForegroundAppChanged(
                ForegroundAppAware.Snapshot(
                    packageName = event.packageName?.toString(),
                    className = event.className?.toString()
                )
            )
        }
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                imeInsetObserver.recompute()
                gestureWindowManager.refreshVisibility()
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                imeInsetObserver.recompute()
                gestureWindowManager.refreshVisibility()
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val keyCode = event?.keyCode
        if (settingsStore.advancedSettings.value.volumeButtonSwitchSong &&
            audioManager.isMusicActive &&
            powerManager.isInteractive.not() &&
            (keyCode == KEYCODE_VOLUME_UP || keyCode == KEYCODE_VOLUME_DOWN)
        ) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    volumeButtonSwitchSongJob = coroutineScope.launch {
                        delay(ViewConfiguration.getLongPressTimeout().toLong())
                        when (keyCode) {
                            KEYCODE_VOLUME_UP -> dispatchMediaKeyEvent(KEYCODE_MEDIA_PREVIOUS)
                            KEYCODE_VOLUME_DOWN -> dispatchMediaKeyEvent(KEYCODE_MEDIA_NEXT)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val isCompleted = volumeButtonSwitchSongJob?.isCompleted == true
                    volumeButtonSwitchSongJob?.cancel()
                    volumeButtonSwitchSongJob = null
                    if (!isCompleted) {
                        when (keyCode) {
                            KEYCODE_VOLUME_UP -> volumeUp()
                            KEYCODE_VOLUME_DOWN -> volumeDown()
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    volumeButtonSwitchSongJob?.cancel()
                    volumeButtonSwitchSongJob = null
                }
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        actionManager.dismissOverlays()
        super.onDestroy()
        gestureWindowManager.release()
        coroutineScope.cancel()
        pinnedScreenshotManager.release()
        unregisterReceiver(screenLockReceiver)
        unregisterReceiver(wallpaperChangedReceiver)
        imeInsetObserver.unregister()
    }

    override fun onSetOverlay() {
        registerScreenLockReceiver()
        registerWallpaperChangedReceiver()
        registerImeInsetObserver()
        registerUpdateChecker()
        gestureWindowManager.startOrReattach()
    }

    private fun registerScreenLockReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenLockReceiver, intentFilter)
    }

    private fun registerWallpaperChangedReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_WALLPAPER_CHANGED)
        }
        registerReceiver(wallpaperChangedReceiver, intentFilter)
    }

    private fun registerImeInsetObserver() {
        coroutineScope.launch {
            launch {
                settingsStore.advancedSettings
                    .distinctUntilChangedBy {
                        it.fitSoftKeyboard
                    }
                    .collectLatest {
                        if (it.fitSoftKeyboard) {
                            imeInsetObserver.register()
                        } else {
                            imeInsetObserver.unregister()
                        }
                    }
            }
        }
    }

    /**
     * 后台低频检查更新 ticker：独立协程 + try-catch 全隔离，任何异常都不影响手势主流程。
     *
     * 仅「检查 + 写缓存 + 发现新版通知」，绝不下载（下载收口主进程 DownloadService）。
     */
    private fun registerUpdateChecker() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    val autoCheck = settingsStore.advancedSettings.value.autoCheckUpdate
                    if (autoCheck && UpdateRepository.shouldCheck()) {
                        val result = UpdateRepository.checkAndCache(force = false)
                        if (result is UpdateRepository.CheckResult.NewVersion) {
                            val ignored = settingsStore.initialSettings.value.ignoredUpdateVersion
                            val version = result.state.latestVersion
                            if (version.isNotBlank() && version != ignored) {
                                UpdateNotifications.showNewVersion(this@SideGestureService, version)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 检查异常完全吞掉，绝不波及无障碍手势
                }
                delay(UPDATE_CHECK_TICK_INTERVAL_MS)
            }
        }
    }

    fun getCurrentPackageName(): String {
        return rootInActiveWindow?.packageName?.toString() ?: ""
    }

    fun nowInLauncher(): Boolean {
        val pkgName = getCurrentPackageName()
        val launcherIntent = Intent().apply {
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolves = packageManager
            .queryIntentActivitiesCompat(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter {
                packageManager.getLaunchIntentForPackage(it.activityInfo.packageName ?: "") == null
            }
        return resolves.any { it.activityInfo?.packageName == pkgName }
    }

    fun performAction(action: Action) {
        actionManager.submit(ActionRequest(action))
    }

    fun toggleTaskSwitcherLock(packageName: String): Boolean {
        val packages = _taskSwitcherLockedPackages.value
        val locked = packageName !in packages
        _taskSwitcherLockedPackages.value = if (locked) {
            packages + packageName
        } else {
            packages - packageName
        }
        return locked
    }

    fun dismissActionOverlays() {
        actionManager.dismissOverlays()
    }

    private companion object {
        // 后台检查 ticker 醒来间隔；是否真正发起请求仍由 24h shouldCheck 决定
        const val UPDATE_CHECK_TICK_INTERVAL_MS = 30 * 60 * 1000L

    }

    private inner class ImeInsetObserver {

        private val _flow = MutableStateFlow(0)
        val flow: StateFlow<Int> = _flow.asStateFlow()

        private var enabled = false

        fun register() {
            enabled = true
            recompute()
        }

        fun unregister() {
            enabled = false
            _flow.value = 0
        }

        fun recompute() {
            if (!enabled) {
                _flow.value = 0
                return
            }
            try {
                val wins = windows
                if (wins.isNullOrEmpty()) {
                    _flow.value = 0
                    return
                }
                val imeWindow = wins.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                if (imeWindow == null) {
                    _flow.value = 0
                    return
                }
                val r = Rect()
                imeWindow.getBoundsInScreen(r)
                val screenHeight = ScreenUtils.getScreenHeight()
                if (r.height() <= 0 || r.top >= screenHeight) {
                    _flow.value = 0
                    return
                }
                val padding = screenHeight - r.top
                _flow.value = if (padding > 0) padding else 0
            } catch (e: Exception) {
                _flow.value = 0
            }
        }
    }
}
