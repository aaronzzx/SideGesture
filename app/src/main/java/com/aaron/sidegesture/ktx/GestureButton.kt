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

val GestureButton.buttonTextCompose: String @Composable get() {
    if (position == LEFT) {
        return stringResource(id = R.string.left_gesture_button)
    } else if (position == RIGHT) {
        return stringResource(id = R.string.right_gesture_button)
    }
    return ""
}

val GestureButton.actionText: String get() {
    var text = ""
    val slideActionText = slideActions.actionText
    if (slideActionText.isNotEmpty()) {
        text += slideActionText
    }
    val longSlideActionText = longSlideActions.actionText
    if (longSlideActionText.isNotEmpty()) {
        text += ",$longSlideActionText"
    }
    return text
}

val GestureButton.actionTextCompose: String @Composable get() {
    var text = ""
    val slideActionText = slideActions.actionTextCompose
    if (slideActionText.isNotEmpty()) {
        text += slideActionText
    }
    val longSlideActionText = longSlideActions.actionTextCompose
    if (longSlideActionText.isNotEmpty()) {
        text += ",$longSlideActionText"
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