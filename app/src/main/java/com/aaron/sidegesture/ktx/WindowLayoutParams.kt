package com.aaron.sidegesture.ktx

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.unit.IntSize
import com.aaron.sidegesture.constant.Position
import com.aaron.sidegesture.entity.GestureButton
import com.blankj.utilcode.util.ScreenUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

fun WindowManager.LayoutParams.updateMainView() {
    val rootSize = rootSize
    width = rootSize.width
    height = rootSize.height
}

fun WindowManager.LayoutParams.updateGestureButton(button: GestureButton) {
    val windowHeight = rootSize.height
    width = button.width
    height = (windowHeight * button.fraction).toInt()
    y = (windowHeight * button.start).toInt()
    @SuppressLint("RtlHardcoded")
    gravity = when (button.position) {
        Position.Left -> Gravity.LEFT or Gravity.TOP
        Position.Right -> Gravity.RIGHT or Gravity.TOP
    }
}

fun WindowManager.LayoutParams.setBasic(touchEnabled: Boolean) {
    type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    format = PixelFormat.RGBA_8888
    setFlags(touchEnabled)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    @SuppressLint("RtlHardcoded")
    gravity = Gravity.LEFT or Gravity.TOP
}

fun WindowManager.LayoutParams.setFlags(touchEnabled: Boolean) {
    flags = if (touchEnabled) {
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
    } else {
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    }
}

val rootSize: IntSize
    get() = IntSize(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())