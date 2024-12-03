package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aaron.sidegesture.App
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.GestureActions

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@Composable
fun GestureActions.actionTextCompose(): String {
    var text = ""
    val centerText = center.actionTextCompose(true)
    if (centerText.isNotEmpty()) {
        text += centerText
    }
    val upText = up.actionTextCompose(true)
    if (upText.isNotEmpty()) {
        text += if (text.isEmpty()) {
            upText
        } else {
            ",$upText"
        }
    }
    val downText = down.actionTextCompose(true)
    if (downText.isNotEmpty()) {
        text += if (text.isEmpty()) {
            downText
        } else {
            ",$downText"
        }
    }
    return text
}

@Composable
fun List<Action>.actionTextCompose(emptyIfNone: Boolean = false): String {
    if (size <= 1) {
        val value = firstOrNull() ?: Action.NONE
        return actionText(value, emptyIfNone)
    }
    return remember(this, emptyIfNone) {
        this
            .filter {
                it.value.isNotEmpty() && it.value != GlobalActions.NONE
            }
            .joinToString(separator = ",") {
                App.getContext().actionText(it, emptyIfNone)
            }
    }
}

fun GestureActions.actionsBy(direction: TriggerDirection): List<Action> {
    return when (direction) {
        TriggerDirection.Up -> up
        TriggerDirection.Center -> center
        TriggerDirection.Down -> down
    }
}