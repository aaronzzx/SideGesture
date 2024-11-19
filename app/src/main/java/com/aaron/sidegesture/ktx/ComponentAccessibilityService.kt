package com.aaron.sidegesture.ktx

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.utils.MotionEventHelper
import com.blankj.utilcode.util.ScreenUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

private val screenWidth get() = ScreenUtils.getScreenWidth()
private val screenHeight get() = ScreenUtils.getScreenHeight()

fun ComponentAccessibilityService.setComposeOverlay(content: @Composable () -> Unit): ComposeView {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSPARENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        width = screenWidth
        height = screenHeight
        @SuppressLint("RtlHardcoded")
        gravity = Gravity.LEFT or Gravity.TOP
    }

    val composeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@setComposeOverlay)
        setViewTreeViewModelStoreOwner(this@setComposeOverlay)
        setViewTreeSavedStateRegistryOwner(this@setComposeOverlay)
        setContent {
            content()
        }
    }
    wm.addView(composeView, lp)

    return composeView
}

fun ComponentAccessibilityService.attachGestureButtons(buttons: Collection<GestureButton>): List<View> {
    return buttons.map { button ->
        attachGestureButton(button)
    }
}

fun ComponentAccessibilityService.attachGestureButton(button: GestureButton): View {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSPARENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val windowHeight = screenHeight
        width = button.width
        height = (windowHeight * button.fraction).toInt()
        y = (windowHeight * button.start).toInt()

        @SuppressLint("RtlHardcoded")
        gravity = if (button.position == GestureButton.LEFT) {
            Gravity.LEFT or Gravity.TOP
        } else {
            Gravity.RIGHT or Gravity.TOP
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    val view = View(this).apply {
        setOnTouchListener { _, event ->
            MotionEventHelper.dispatchMotionEvent(event)
            false
        }
    }
    wm.addView(view, lp)
    return view
}

fun ComponentAccessibilityService.removeWindows(views: Collection<View>) {
    views.forEach { view ->
        removeWindow(view)
    }
}

fun ComponentAccessibilityService.removeWindow(view: View) {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    try {
        wm.removeView(view)
    } catch (ignored: Exception) {
    }
}