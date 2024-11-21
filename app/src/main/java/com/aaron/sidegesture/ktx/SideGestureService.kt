package com.aaron.sidegesture.ktx

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.utils.MotionEventDispatcher
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@RequiresApi(Build.VERSION_CODES.R)
suspend fun SideGestureService.takeScreenshot(): Bitmap? = suspendCancellableCoroutine { cont ->
    takeScreenshot(0, applicationContext.mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
        override fun onSuccess(screenshotResult: AccessibilityService.ScreenshotResult) {
            val value = Bitmap.wrapHardwareBuffer(screenshotResult.hardwareBuffer, screenshotResult.colorSpace)
            var bmp: Bitmap? = null
            if (value != null) {
                bmp = Bitmap.createBitmap(value, 0, 0, ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
            }
            cont.resume(bmp)
        }

        override fun onFailure(errorCode: Int) {
            cont.resume(null)
        }
    })
}

fun SideGestureService.updateLayout(view: View, lp: WindowManager.LayoutParams) {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    wm.updateViewLayout(view, lp)
}

fun SideGestureService.attachComposeOverlay(content: @Composable () -> Unit): ComposeView {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        setBasic(false)
        updateMainView()
    }
    val composeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@attachComposeOverlay)
        setViewTreeViewModelStoreOwner(this@attachComposeOverlay)
        setViewTreeSavedStateRegistryOwner(this@attachComposeOverlay)
        setContent {
            content()
        }
    }
    wm.addView(composeView, lp)
    return composeView
}

fun SideGestureService.attachGestureButtons(buttons: Collection<GestureButton>): List<View> {
    return buttons.map { button ->
        attachGestureButton(button)
    }
}

fun SideGestureService.attachGestureButton(button: GestureButton): View {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        setBasic(true)
        updateGestureButton(button)
    }
    @SuppressLint("ClickableViewAccessibility")
    val view = View(this).apply {
        tag = button
        setOnTouchListener { _, event ->
            MotionEventDispatcher.dispatch(event)
            false
        }
    }
    wm.addView(view, lp)
    return view
}

fun SideGestureService.removeWindows(views: Collection<View>) {
    views.forEach { view ->
        removeWindow(view)
    }
}

fun SideGestureService.removeWindow(view: View) {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    try {
        wm.removeView(view)
    } catch (ignored: Exception) {
    }
}