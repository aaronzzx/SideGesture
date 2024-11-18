package com.aaron.sidegesture

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.Box
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
import com.aaron.composeaccessibility.setComposeOverlay
import com.aaron.sidegesture.config.Actions
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ui.SideGesturePad
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
        setComposeOverlay2(this, this, this) {
            val coroutineScope = rememberCoroutineScope()
            var rootSize by remember { mutableStateOf(IntSize.Zero) }

            DisposableEffect(key1 = coroutineScope) {
                val buttons = listOf(
                    GestureButton(LEFT, 0.00f, 0.30f),
                    GestureButton(LEFT, 0.35f, 0.65f),
                    GestureButton(LEFT, 0.70f, 1.00f),
                    GestureButton(RIGHT, 0.00f, 0.30f),
                    GestureButton(RIGHT, 0.35f, 0.65f),
                    GestureButton(RIGHT, 0.70f, 1.00f),
                )
                var windows: List<View>? = null
                coroutineScope.launch {
                    snapshotFlow { rootSize }
                        .filter { it.width > 0 && it.height > 0 }
                        .collect { size ->
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

            Box(
                modifier = Modifier
                    .onGloballyPositioned {
                        rootSize = it.size
                    }
                    .fillMaxSize()
            )
        }
    }

    private fun test() {
        setComposeOverlay(this, this, this) {
            SideGesturePad(
                modifier = Modifier.fillMaxSize(),
                onAction = { action ->
//                    when (action) {
//                        GestureActions.BACK -> {
//                            performGlobalAction(GLOBAL_ACTION_BACK)
//                        }
//                        GestureActions.LOCK_SCREEN -> {
//                            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
//                        }
//                        GestureActions.PREVIOUS_APP -> {
//                            previousApp()
//                        }
//                    }
                },
                buttons = listOf(
                    GestureButton(
                        position = LEFT,
                        pressAction = GestureActions.Single(
                            up = Actions.LOCK_SCREEN,
                            center = Actions.BACK
                        ),
                        longPressAction = GestureActions.Multiple(
                            center = listOf(
                                Actions.PREVIOUS_APP,
                                Actions.PREVIOUS_APP,
                                Actions.PREVIOUS_APP,
                                Actions.PREVIOUS_APP,
                                Actions.PREVIOUS_APP
                            )
                        )
                    ),
//                    GestureButton(
//                        position = GestureButton.RIGHT,
//                        pressAction = GestureAction.Single(
//                            up = GestureActions.LOCK_SCREEN,
//                            center = GestureActions.BACK
//                        ),
//                        longPressAction = GestureAction.Multiple(
//                            center = listOf(GestureActions.PREVIOUS_APP)
//                        )
//                    )
                )
            )
        }
    }

    private fun previousApp() {
        val prevPkgName = prevPkgName ?: return
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

        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        @SuppressLint("RtlHardcoded")
        gravity = Gravity.LEFT or Gravity.TOP
    }

    val composeView = ComposeView(this).apply {
        setContent {
            content()
        }
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
    }
    wm.addView(composeView, lp)

    return composeView
}