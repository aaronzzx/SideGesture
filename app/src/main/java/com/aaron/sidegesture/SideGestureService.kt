package com.aaron.sidegesture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.postDelayed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.ActionSettings
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
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.ui.widget.SideGestureContainer
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.Events
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private val proxy = SideGestureServiceProxy(this)

    private val imeInsetObserver = ImeInsetObserver(this)
    private var mainView: View? = null
    private var buttonViews: List<View>? = null
    private var orientation = if (ScreenUtils.isLandscape()) 2 else 1

    private var isNowInLockScreenPage = false

    private var isVolumeButtonSwitchSongEnabled = false
    private var volumeButtonSwitchSongJob: Job? = null

    val coroutineScope = MainScope()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation
            updateLayout()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        proxy.onAccessibilityEvent(event)
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            updateGestureButtons()
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyCode = event?.keyCode
        if (isVolumeButtonSwitchSongEnabled &&
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
        proxy.onRelease()
        imeInsetObserver.unregister()
        coroutineScope.cancel()
    }

    override fun onSetOverlay() {
        registerScreenLockReceiver()
        registerWallpaperChangedReceiver()
        registerImeInsetObserver()

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
                        val animationStyles by DataStoreHolder
                            .advancedSettings
                            .data
                            .map { it.animationStyles }
                            .collectAsStateWithLifecycle(initialValue = AnimationStyles())
                        val imePadding by imeInsetObserver
                            .flow
                            .collectAsStateWithLifecycle()
                        val actionSettings by DataStoreHolder
                            .actionSettings
                            .data
                            .collectAsStateWithLifecycle(initialValue = ActionSettings())
                        SideGestureContainer(
                            modifier = Modifier.matchParentSize(),
                            buttons = sideButtons + bottomButtons,
                            imePadding = imePadding,
                            animationStyle = when (animationStyles.isAnimationEnabled) {
                                true -> animationStyles.value
                                else -> null
                            },
                            onAction = { action ->
                                proxy.onAction(action)
                            },
                            actionSettings = actionSettings
                        )
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main.immediate) {
            launch {
                DataStoreHolder
                    .sideGestureButtons
                    .data
                    .combine(DataStoreHolder.bottomGestureButtons.data) { l1, l2 ->
                        l1 + l2
                    }
                    .collectLatest { buttons ->
                        val buttonViews = buttonViews
                        if (buttonViews != null) {
                            removeWindows(buttonViews)
                        }
                        this@SideGestureService.buttonViews = attachGestureButtons(buttons)
                        updateGestureButtons()
                    }
            }
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
            launch {
                DataStoreHolder
                    .advancedSettings
                    .data
                    .distinctUntilChangedBy {
                        it.hideTemporary
                    }
                    .collectLatest {
                        updateGestureButtons()
                    }
            }
            launch {
                DataStoreHolder
                    .advancedSettings
                    .data
                    .distinctUntilChangedBy {
                        it.volumeButtonSwitchSong
                    }
                    .collectLatest {
                        isVolumeButtonSwitchSongEnabled = it.volumeButtonSwitchSong
                    }
            }
        }
    }

    private fun registerScreenLockReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    isNowInLockScreenPage = true
                } else if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    isNowInLockScreenPage = false
                }
                updateGestureButtons()
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, intentFilter)
    }

    private fun registerWallpaperChangedReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Events.post(WallpaperChangedEvent())
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_WALLPAPER_CHANGED)
        }
        registerReceiver(receiver, intentFilter)
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
            val buttonViews = buttonViews
            buttonViews?.forEach { view ->
                val button = view.tag as? GestureButton ?: return@forEach
                val lp = (view.layoutParams as WindowManager.LayoutParams).apply {
                    updateGestureButton(button)
                    if (button.position != Position.Bottom) {
                        val imePadding = imeInsetObserver.flow.value
                        y += -imePadding
                    }

                    val advancedSettings = DataStoreHolder.advancedSettings.data.first()
                    if (advancedSettings.hideTemporary) {
                        view.setOnClickListener { v ->
                            val lp = v.layoutParams as WindowManager.LayoutParams
                            lp.setFlags(false)
                            updateLayout(v, lp)
                            v.postDelayed(1000) {
                                val lp2 = v.layoutParams as WindowManager.LayoutParams
                                val enabled = (view.tag as? GestureButton)?.enabled ?: false
                                lp2.setFlags(enabled)
                                updateLayout(v, lp2)
                            }
                        }
                    } else {
                        view.setOnClickListener(null)
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

    private class ImeInsetObserver(val context: Context) {

        private val _flow = MutableStateFlow(0)
        val flow: StateFlow<Int> = _flow.asStateFlow()

        private var view: View? = null

        fun register() {
            unregister()
            this.view = View(context).apply {
                val localRect = Rect()
                val windowRect = Rect()
                viewTreeObserver.addOnGlobalLayoutListener {
                    getLocalVisibleRect(localRect)
                    getWindowVisibleDisplayFrame(windowRect)
                    val navBarHeight = ScreenUtils.getScreenHeight() - windowRect.bottom
                    val imePadding = windowRect.height() - localRect.height() + navBarHeight
                    if (localRect.height() == windowRect.height()) {
                        // ime invisible
                        _flow.value = 0
                    } else {
                        // ime visible
                        _flow.value = imePadding
                    }
                }
                val lp = WindowManager.LayoutParams().also { lp ->
                    lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                    lp.width = WindowManager.LayoutParams.MATCH_PARENT
                    lp.height = WindowManager.LayoutParams.MATCH_PARENT
                    lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                    lp.format = PixelFormat.RGBA_8888
                    lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }
                addView(this, lp)
            }
        }

        fun unregister() {
            val view = view
            if (view != null) {
                _flow.value = 0
                removeView(view)
                this.view = null
            }
        }

        private fun addView(view: View, lp: WindowManager.LayoutParams) {
            val wm = ContextCompat.getSystemService(context, WindowManager::class.java)!!
            wm.addView(view, lp)
        }

        private fun removeView(view: View) {
            val wm = ContextCompat.getSystemService(context, WindowManager::class.java)!!
            try {
                wm.removeViewImmediate(view)
            } catch (ignored: Exception) {
            }
        }
    }
}