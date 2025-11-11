package com.aaron.sidegesture.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import androidx.annotation.RequiresApi
import com.aaron.sidegesture.SideGestureService
import com.blankj.utilcode.util.ScreenUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/27
 */
object AccessibilityUtils {

    /**
     * 实现对（x，y）坐标进行点击操作。
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun click(service: AccessibilityService?, x: Int, y: Int): Boolean {
        if (service == null) {
            return false
        }
        val point = Point(x, y)
        val builder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(point.x.toFloat(), point.y.toFloat())
        builder.addStroke(StrokeDescription(path, 0L, 100L))
        val gesture = builder.build()

        return service.dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
            }
        }, null)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun fastVerticalScroll(service: SideGestureService, toTop: Boolean): Boolean {
        val screenWidth = ScreenUtils.getScreenWidth()
        val screenHeight = ScreenUtils.getScreenHeight()
        val point = Point(screenWidth / 2, screenHeight / 2)
        val builder = GestureDescription.Builder()
        if (toTop) {
            val path = Path()
            path.moveTo(point.x.toFloat(), point.y.toFloat())
            path.lineTo(point.x.toFloat(), point.y.toFloat() + Int.MAX_VALUE)
            builder.addStroke(StrokeDescription(path, 0L, 100L))
        } else {
            repeat(service.gotoBottomStrength.coerceIn(1, GestureDescription.getMaxStrokeCount())) { index ->
                val delay = index * 100L
                val path = Path()
                path.moveTo(point.x.toFloat(), point.y.toFloat())
                path.lineTo(point.x.toFloat(), 0f)
                builder.addStroke(StrokeDescription(path, delay, 10L))
            }
        }
        return service.dispatchGesture(builder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
            }
        }, null)
    }
}
