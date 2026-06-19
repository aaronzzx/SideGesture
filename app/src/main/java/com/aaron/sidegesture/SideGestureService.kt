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
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.RecentTask
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.entity.global.InitialSettings
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
import com.aaron.sidegesture.screenshot.PinnedScreenshotManager
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.ui.widget.SideGestureContainer
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.Events
import com.aaron.sidegesture.utils.update.UpdateNotifications
import com.aaron.sidegesture.utils.update.UpdateRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private val proxy = SideGestureServiceProxy(this)

    private val imeInsetObserver = ImeInsetObserver()
    private var mainView: View? = null
    private var buttonViews: List<View>? = null
    private var orientation = if (ScreenUtils.isLandscape()) 2 else 1
    private var screenWidthDp = Resources.getSystem().configuration.screenWidthDp
    private var screenHeightDp = Resources.getSystem().configuration.screenHeightDp

    private var isNowInLockScreenPage = false
    private var currentButtons: List<GestureButton> = emptyList()

    private var volumeButtonSwitchSongJob: Job? = null
    private val _overlaysDismissSignal = MutableStateFlow(0)
    private val _taskSwitcherLockedPackages = MutableStateFlow(emptySet<String>())

    val coroutineScope = MainScope()
    val overlaysDismissSignal: StateFlow<Int> = _overlaysDismissSignal.asStateFlow()
    val taskSwitcherLockedPackages: StateFlow<Set<String>> = _taskSwitcherLockedPackages.asStateFlow()
    val pinnedScreenshotManager: PinnedScreenshotManager by lazy { PinnedScreenshotManager(this) }

    var initialSettings: InitialSettings? = null
        private set
    var advancedSettings: AdvancedSettings? = null
        private set
    var gestureSettings: GestureSettings? = null
        private set
    var actionSettings: ActionSettings? = null
        private set

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
            updateGestureButtons()
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
            updateLayout()
            pinnedScreenshotManager.onEnvironmentChanged(currentButtons)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        proxy.onAccessibilityEvent(event)
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                imeInsetObserver.recompute()
                updateGestureButtons()
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                imeInsetObserver.recompute()
                updateGestureButtons()
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val keyCode = event?.keyCode
        if (advancedSettings?.volumeButtonSwitchSong == true &&
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
        super.onDestroy()
        coroutineScope.cancel()
        proxy.onRelease()
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

        val mainView = mainView
        if (mainView != null) {
            removeWindow(mainView)
        }
        this.mainView = attachComposeOverlay {
            var key by remember { mutableStateOf(Any()) }
            SubscribeEvent(eventClass = WallpaperChangedEvent::class) {
                key = Any()
            }
            key(key) {
                SideGestureTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val sideButtons by DataStoreHolder
                            .sideGestureButtons
                            .data
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        val bottomButtons by DataStoreHolder
                            .bottomGestureButtons
                            .data
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        val advancedSettings by DataStoreHolder
                            .advancedSettings
                            .data
                            .collectAsStateWithLifecycle(initialValue = AdvancedSettings())
                        val gestureSettings by DataStoreHolder
                            .gestureSettings
                            .data
                            .collectAsStateWithLifecycle(initialValue = GestureSettings())
                        val imePadding by imeInsetObserver
                            .flow
                            .collectAsStateWithLifecycle()
                        val actionSettings by DataStoreHolder
                            .actionSettings
                            .data
                            .collectAsStateWithLifecycle(initialValue = ActionSettings())
                        val taskSwitcherLockedPackages by this@SideGestureService
                            .taskSwitcherLockedPackages
                            .collectAsStateWithLifecycle(initialValue = emptySet())
                        val overlaysDismissSignal by this@SideGestureService.overlaysDismissSignal
                            .collectAsStateWithLifecycle()
                        SideGestureContainer(
                            modifier = Modifier.matchParentSize(),
                            buttons = sideButtons + bottomButtons,
                            imePadding = imePadding,
                            animationStyle = when (advancedSettings.animationStyles.isAnimationEnabled) {
                                true -> advancedSettings.animationStyles.value
                                else -> null
                            },
                            actionPanelStyle = advancedSettings.actionPanelStyles.value,
                            onAction = { action ->
                                proxy.onAction(action)
                            },
                            onOverlayTouchChange = ::setOverlayTouchEnabled,
                            actionSettings = actionSettings,
                            advancedSettings = advancedSettings,
                            gestureSettings = gestureSettings,
                            taskSwitcherLockedPackages = taskSwitcherLockedPackages,
                            overlaysDismissSignal = overlaysDismissSignal
                        )
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main.immediate) {
            // 监听全局配置修改
            launch {
                DataStoreHolder
                    .initialSettings
                    .data
                    .collectLatest {
                        initialSettings = it
                    }
            }
            launch {
                DataStoreHolder
                    .advancedSettings
                    .data
                    .collectLatest {
                        advancedSettings = it
                    }
            }
            launch {
                DataStoreHolder
                    .gestureSettings
                    .data
                    .collectLatest {
                        gestureSettings = it
                    }
            }
            launch {
                DataStoreHolder
                    .actionSettings
                    .data
                    .collectLatest {
                        actionSettings = it
                    }
            }

            // 监听触钮修改
            launch {
                DataStoreHolder
                    .sideGestureButtons
                    .data
                    .combine(DataStoreHolder.bottomGestureButtons.data) { l1, l2 ->
                        l1 + l2
                    }
                    .collectLatest { buttons ->
                        currentButtons = buttons
                        val buttonViews = buttonViews
                        if (buttonViews != null) {
                            removeWindows(buttonViews)
                        }
                        this@SideGestureService.buttonViews = attachGestureButtons(buttons)
                        pinnedScreenshotManager.onEnvironmentChanged(buttons)
                        updateGestureButtons()
                    }
            }
            // 监听手势开关
            launch {
                DataStoreHolder
                    .initialSettings
                    .data
                    .distinctUntilChangedBy {
                        it.gestureEnabled
                    }
                    .collectLatest {
                        updateGestureButtons()
                    }
            }
        }
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
                imeInsetObserver.flow.collectLatest {
                    updateGestureButtons()
                }
            }
            launch {
                DataStoreHolder
                    .advancedSettings
                    .data
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
                    val autoCheck = DataStoreHolder.advancedSettings.data.first().autoCheckUpdate
                    if (autoCheck && UpdateRepository.shouldCheck()) {
                        val result = UpdateRepository.checkAndCache(force = false)
                        if (result is UpdateRepository.CheckResult.NewVersion) {
                            val ignored = DataStoreHolder.initialSettings.data.first().ignoredUpdateVersion
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

    private fun updateLayout() {
        val mainView = mainView
        if (mainView != null) {
            val lp = (mainView.layoutParams as WindowManager.LayoutParams).apply {
                updateMainView()
            }
            updateLayout(mainView, lp)
        }
        updateGestureButtons()
    }

    private fun updateGestureButtons() {
        coroutineScope.launch {
            val advancedSettings = advancedSettings ?: return@launch
            val buttonViews = buttonViews
            buttonViews?.forEach { view ->
                val button = view.tag as? GestureButton ?: return@forEach
                val lp = (view.layoutParams as WindowManager.LayoutParams).apply {
                    updateGestureButton(button)
                    if (button.position != Position.Bottom) {
                        val imePadding = imeInsetObserver.flow.value
                        y += -imePadding
                    }

                    val initialSettings = DataStoreHolder.initialSettings.data.first()
                    if (!initialSettings.gestureEnabled) {
                        setFlags(false)
                    } else {
                        if (advancedSettings.hideLandscape && ScreenUtils.isLandscape()) {
                            setFlags(false)
                        } else if (advancedSettings.hideHomeScreen && nowInLauncher()) {
                            setFlags(false)
                        } else if (advancedSettings.hideScreenLock && isNowInLockScreenPage) {
                            setFlags(false)
                        } else if (getCurrentPackageName() in advancedSettings.excludeApps) {
                            setFlags(false)
                        } else {
                            setFlags(button.enabled)
                        }
                    }
                }
                updateLayout(view, lp)
            }
        }
    }

    fun hideGestureButton(button: GestureButton?) {
        buttonViews?.forEach { view ->
            val tag = view.tag as? GestureButton ?: return@forEach
            val matched = if (button != null) {
                tag.id == button.id && tag.position == button.position
            } else {
                true
            }
            if (!matched) return@forEach

            val lp = view.layoutParams as WindowManager.LayoutParams
            lp.setFlags(false)
            updateLayout(view, lp)
            view.postDelayed(1000) {
                updateGestureButtons()
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
        proxy.onAction(action)
    }

    suspend fun queryRecentTasks(): List<RecentTask> {
        return proxy.queryRecentTasks()
    }

    suspend fun closeRecentTask(packageName: String): Boolean {
        return proxy.closeRecentTask(packageName)
    }

    fun switchToRecentTask(packageName: String) {
        proxy.switchToRecentTask(packageName)
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
        _overlaysDismissSignal.value++
    }

    fun setOverlayTouchEnabled(enabled: Boolean) {
        val mainView = mainView ?: return
        val lp = (mainView.layoutParams as WindowManager.LayoutParams).apply {
            setFlags(enabled)
        }
        updateLayout(mainView, lp)
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
