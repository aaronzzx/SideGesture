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

val GestureButton.fraction: Float get() = end - start

fun List<GestureButton>.find(rootSize: Size, offset: Offset): GestureButton? {
    return find { it.contains(rootSize, offset) }
}

fun GestureButton.contains(rootSize: Size, offset: Offset): Boolean {
    val bounds = bounds(rootSize)
    return bounds.contains(offset)
}

fun GestureButton.bounds(rootSize: Size): Rect {
    val y = rootSize.height * start
    val topLeft = if (position == LEFT) {
        Offset(0f, y)
    } else {
        Offset(rootSize.width - width, y)
    }
    val boundsSize = Size(width.toFloat(), rootSize.height * fraction)
    return Rect(topLeft, boundsSize)
}