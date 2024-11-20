package com.aaron.sidegesture.ktx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun List<GestureButton>.find(offset: Offset): GestureButton? {
    return find { it.contains(offset) }
}

fun GestureButton.contains(offset: Offset): Boolean {
    val bounds = bounds()
    return bounds.contains(offset)
}

fun GestureButton.bounds(): Rect {
    val y = rootSize.height * start
    val topLeft = if (position == LEFT) {
        Offset(0f, y)
    } else {
        Offset((rootSize.width - width).toFloat(), y)
    }
    val boundsSize = Size(width.toFloat(), rootSize.height * fraction)
    return Rect(topLeft, boundsSize)
}

val GestureButton.fraction: Float get() = end - start