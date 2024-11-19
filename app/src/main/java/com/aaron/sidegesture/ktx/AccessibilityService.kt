package com.aaron.sidegesture.ktx

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.utils.MotionEventHelper

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun AccessibilityService.attachGestureButtons(
    size: IntSize,
    buttons: Collection<GestureButton>
): List<View> {
    return buttons.map { button ->
        attachGestureButton(size, button)
    }
}

fun AccessibilityService.attachGestureButton(
    size: IntSize,
    button: GestureButton
): View {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSPARENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val windowHeight = size.height
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
        setOnTouchListener { view, event ->
            MotionEventHelper.dispatchMotionEvent(event)
            false
        }
    }
    wm.addView(view, lp)
    return view
}

fun AccessibilityService.removeWindows(views: Collection<View>) {
    views.forEach { view ->
        removeWindow(view)
    }
}

fun AccessibilityService.removeWindow(view: View) {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    try {
        wm.removeView(view)
    } catch (ignored: Exception) {
    }
}