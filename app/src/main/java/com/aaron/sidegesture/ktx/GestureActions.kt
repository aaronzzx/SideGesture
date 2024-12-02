package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aaron.sidegesture.App
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.Actions
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
fun Actions.actionTextCompose(emptyIfNone: Boolean = false): String {
    if (!isLongActions) {
        return actionText(value, emptyIfNone)
    }
    return remember(values, emptyIfNone) {
        if (values.isEmpty()) {
            return@remember App.getContext().actionText(GlobalActions.NONE, emptyIfNone)
        }
        values
            .filter {
                it.isNotEmpty() && it != GlobalActions.NONE
            }
            .joinToString(separator = ",") {
                App.getContext().actionText(it, emptyIfNone)
            }
    }
}

fun GestureActions.actionsBy(direction: TriggerDirection): Actions {
    return when (direction) {
        TriggerDirection.Up -> up
        TriggerDirection.Center -> center
        TriggerDirection.Down -> down
    }
}

fun Actions.isEmpty(): Boolean {
    val value = value
    return value.isEmpty() || value == GlobalActions.NONE
}

fun Actions.isNotEmpty(): Boolean {
    return !isEmpty()
}