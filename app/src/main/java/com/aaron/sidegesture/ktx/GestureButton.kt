package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@Composable
fun GestureButton.buttonTextCompose(): String {
    if (position == LEFT) {
        return stringResource(id = R.string.left_gesture_button)
    } else if (position == RIGHT) {
        return stringResource(id = R.string.right_gesture_button)
    }
    return ""
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