package com.aaron.sidegesture

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.config.GestureAction
import com.aaron.sidegesture.config.GestureActions
import com.aaron.sidegesture.ui.GestureButton
import com.aaron.sidegesture.ui.SideGesturePad

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Blue.copy(alpha = 0.1f))
                    .pointerInput(Unit) {
                        var origin = Offset.Zero
                        var finger = Offset.Zero
                        detectDragGestures(
                            onDragStart = { offset ->
                                origin = offset
                                finger = offset
                            },
                            onDrag = { _, dragAmount ->
                                finger += dragAmount
                                Log.d("zzx", "$finger")
                            }
                        )
                    }
            )
        }
    }

    private fun test() {
        setComposeOverlay2(this, this, this) {
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
                        position = GestureButton.LEFT,
                        pressAction = GestureAction.Single(
                            up = GestureActions.LOCK_SCREEN,
                            center = GestureActions.BACK
                        ),
                        longPressAction = GestureAction.Multiple(
                            center = listOf(
                                GestureActions.PREVIOUS_APP,
                                GestureActions.PREVIOUS_APP,
                                GestureActions.PREVIOUS_APP,
                                GestureActions.PREVIOUS_APP,
                                GestureActions.PREVIOUS_APP
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

        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        width = 100
        height = 400
        @SuppressLint("RtlHardcoded")
        gravity = Gravity.CENTER
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