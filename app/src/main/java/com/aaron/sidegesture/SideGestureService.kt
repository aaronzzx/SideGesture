package com.aaron.sidegesture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.View
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.event.WallpaperChangedEvent
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
import com.aaron.sidegesture.ui.widget.SideGestureContainer
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.Events
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
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

    private var mainView: View? = null
    private var buttonViews: List<View>? = null
    private var orientation = if (ScreenUtils.isLandscape()) 2 else 1

    private var isNowInLockScreenPage = false

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

    override fun onInterrupt() {
        coroutineScope.cancel()
    }

    override fun onSetOverlay() {
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
                        val buttons by DataStoreHolder
                            .gestureButtons
                            .data
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        val animationStyles by DataStoreHolder
                            .advancedSettings
                            .data
                            .map { it.animationStyles }
                            .collectAsStateWithLifecycle(initialValue = AnimationStyles())
                        SideGestureContainer(
                            modifier = Modifier.matchParentSize(),
                            buttons = buttons,
                            animationStyle = when (animationStyles.isAnimationEnabled) {
                                true -> animationStyles.value
                                else -> null
                            },
                            onAction = { action ->
                                proxy.onAction(action)
                            }
                        )
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main.immediate) {
            launch {
                DataStoreHolder.gestureButtons.data.collectLatest { buttons ->
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
        }

        registerScreenLockReceiver()
        registerWallpaperChangedReceiver()
    }

    private fun registerScreenLockReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_ON) {
                    isNowInLockScreenPage = true
                } else if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    isNowInLockScreenPage = false
                }
                updateGestureButtons()
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
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

                    val initialSettings = DataStoreHolder.initialSettings.data.first()
                    if (!initialSettings.gestureEnabled) {
                        setFlags(false)
                    } else {
                        val advancedSettings = DataStoreHolder.advancedSettings.data.first()
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

    private fun getCurrentPackageName(): String {
        return rootInActiveWindow?.packageName?.toString() ?: ""
    }

    private fun nowInLauncher(): Boolean {
        val pkgName = getCurrentPackageName()
        val launcherIntent = Intent().apply {
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolves = packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .filter {
                packageManager.getLaunchIntentForPackage(it.activityInfo.packageName ?: "") == null
            }
        return resolves.any { it.activityInfo?.packageName == pkgName }
    }
}