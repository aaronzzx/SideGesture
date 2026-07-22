package com.aaron.sidegesture.feature.gesture

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/11/15
 */
class GestureView @JvmOverloads constructor(
    context: Context,
    private val button: GestureButton? = null,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val exclusionRects: MutableList<Rect> = mutableListOf()

    init {
        updateExclusionRect()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            updateExclusionRect()
            updateSystemGestureExclusion()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateSystemGestureExclusion()
    }

    private fun updateExclusionRect() {
        if (!canExcludeSystemGesture()) {
            return
        }
        exclusionRects.clear()
        exclusionRects.add(Rect(0, 0, width, height))
    }

    @SuppressLint("NewApi")
    private fun updateSystemGestureExclusion() {
        if (!canExcludeSystemGesture()) {
            return
        }
        exclusionRects.firstOrNull()?.set(0, 0, width, height)
        systemGestureExclusionRects = exclusionRects
    }

    private fun canExcludeSystemGesture(): Boolean {
        val button = button ?: return false
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                button.position != Position.Top &&
                button.excludeSystemGestureRects
    }
}
