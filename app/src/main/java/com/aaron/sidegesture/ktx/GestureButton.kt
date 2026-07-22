package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@Composable
fun GestureButton.buttonTextCompose(): String {
    return when (position) {
        Position.Left -> stringResource(id = R.string.left_gesture_button)
        Position.Right -> stringResource(id = R.string.right_gesture_button)
        Position.Bottom -> stringResource(id = R.string.bottom_gesture_button)
        Position.Top -> stringResource(id = R.string.top_gesture_button)
    }
}

@Composable
fun GestureButton.actionTextCompose(): String {
    var text = ""
    val slideActionText = slideActions.actionTextCompose()
    if (slideActionText.isNotEmpty()) {
        text += slideActionText
    }
    val longSlideActionText = longSlideActions.actionTextCompose()
    if (longSlideActionText.isNotEmpty()) {
        text += if (text.isEmpty()) {
            longSlideActionText
        } else {
            ",$longSlideActionText"
        }
    }
    return text
}

fun List<GestureButton>.find(offset: Offset, imePadding: Int = 0): GestureButton? {
    return find { it.contains(offset, imePadding) }
}

fun GestureButton.contains(offset: Offset, imePadding: Int = 0): Boolean {
    val bounds = bounds(imePadding)
    return bounds.contains(offset)
}

fun GestureButton.bounds(imePadding: Int = 0): Rect {
    return bounds(rootSize = rootSize, imePadding = imePadding)
}

fun GestureButton.bounds(rootSize: IntSize, imePadding: Int = 0): Rect {
    val y = rootSize.height * start
    val topLeft = when (position) {
        Position.Left -> Offset(0f, y - imePadding)
        Position.Right -> Offset((rootSize.width - width).toFloat(), y - imePadding)
        Position.Bottom -> Offset(rootSize.width * start, (rootSize.height - width).toFloat())
        Position.Top -> Offset(rootSize.width * start, 0f)
    }
    val boundsSize = when (position) {
        Position.Left, Position.Right -> Size(width.toFloat(), rootSize.height * fraction)
        Position.Bottom, Position.Top -> Size(rootSize.width * fraction, width.toFloat())
    }
    return Rect(topLeft, boundsSize)
}

val GestureButton.fraction: Float get() = end - start
