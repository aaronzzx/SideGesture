package com.aaron.sidegesture

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.config.Actions
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ui.SideGesturePad
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private var prevPkgName: String? = null
    private var curPkgName: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when(event?.eventType){
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkgName = event.packageName?.toString() ?: ""
                val intent = packageManager.getLaunchIntentForPackage(pkgName)
                if (intent != null && curPkgName != pkgName) {
                    prevPkgName = curPkgName
                    curPkgName = pkgName
                }
            }
            else -> Unit
        }
    }

    override fun onInterrupt() {
    }

    override fun onSetOverlay() {
        val buttons = listOf(
            GestureButton(
                position = LEFT,
                start = 0.3f,
                end = 1.0f,
                pressAction = GestureActions.Single(
                    up = Actions.LOCK_SCREEN,
                    center = Actions.BACK
                ),
                longPressAction = GestureActions.Multiple(
                    up = listOf(
                        Actions.HOME,
                        Actions.HOME,
                        Actions.HOME,
                        Actions.HOME,
                        Actions.HOME
                    ),
                    center = listOf(Actions.PREVIOUS_APP)
                )
            ),
            GestureButton(
                position = RIGHT,
                start = 0.3f,
                end = 1.0f,
                pressAction = GestureActions.Single(
                    up = Actions.LOCK_SCREEN,
                    center = Actions.BACK
                ),
                longPressAction = GestureActions.Multiple(
                    center = listOf(Actions.PREVIOUS_APP)
                )
            )
        )
        setComposeOverlay2(this, this, this) {
            SideGestureTheme {
                val coroutineScope = rememberCoroutineScope()
                var rootSize by remember { mutableStateOf(IntSize.Zero) }

                DisposableEffect(coroutineScope) {
                    var windows: List<View>? = null
                    coroutineScope.launch {
                        snapshotFlow { rootSize }
                            .filter { it.width > 0 && it.height > 0 }
                            .collect { size ->
                                Log.d("zzx", "$size")
                                val windowsVal = windows
                                if (!windowsVal.isNullOrEmpty()) {
                                    removeWindows(windowsVal)
                                }
                                windows = attachGestureButtons(size, buttons)
                            }
                    }
                    onDispose {
                        val windowsVal = windows
                        if (!windowsVal.isNullOrEmpty()) {
                            removeWindows(windowsVal)
                        }
                    }
                }

                SideGesturePad(
                    modifier = Modifier
                        .onGloballyPositioned {
                            rootSize = it.size
                        }
                        .fillMaxSize(),
                    onAction = { action ->
                        when (action) {
                            Actions.BACK -> {
                                performGlobalAction(GLOBAL_ACTION_BACK)
                            }
                            Actions.HOME -> {
                                performGlobalAction(GLOBAL_ACTION_HOME)
                            }
                            Actions.LOCK_SCREEN -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                                }
                            }
                            Actions.PREVIOUS_APP -> {
                                previousApp()
                            }
                        }
                    },
                    buttons = buttons,
                )
            }
        }
    }

    private fun previousApp() {
        val prevPkgName = prevPkgName
        if (prevPkgName.isNullOrEmpty()) {
            ToastUtils.showShort("没有上一个应用")
            return
        }
        val curPkgName = curPkgName
        if (prevPkgName == curPkgName) return
        val intent = packageManager.getLaunchIntentForPackage(prevPkgName) ?: return
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            this.prevPkgName = curPkgName
            this.curPkgName = prevPkgName
        } catch (ignored: Exception) {
        }
    }
}

fun AccessibilityService.setComposeOverlay2(
    lifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    content: @Composable () -> Unit,
): ComposeView {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSPARENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        @SuppressLint("RtlHardcoded")
        gravity = Gravity.LEFT or Gravity.TOP
    }

    val composeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
        setContent {
            content()
        }
    }
    wm.addView(composeView, lp)

    return composeView
}