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

val GestureActions.actionText: String get() {
    var text = ""
    if (center.actionText.isNotEmpty()) {
        text += center.actionText
    }
    if (up.actionText.isNotEmpty()) {
        text += ",${up.actionText}"
    }
    if (down.actionText.isNotEmpty()) {
        text += ",${up.actionText}"
    }
    return text
}

val GestureActions.actionTextCompose: String @Composable get() {
    var text = ""
    if (center.actionTextCompose.isNotEmpty()) {
        text += center.actionTextCompose
    }
    if (up.actionTextCompose.isNotEmpty()) {
        text += ",${up.actionTextCompose}"
    }
    if (down.actionTextCompose.isNotEmpty()) {
        text += ",${up.actionTextCompose}"
    }
    return text
}

val Actions.actionText: String get() {
    if (values.isEmpty()) {
        return App.getContext().actionText(value)
    }
    return values.joinToString(separator = ",") {
        App.getContext().actionText(it)
    }
}

val Actions.actionTextCompose: String @Composable get() {
    if (values.isEmpty()) {
        return actionText(value)
    }
    return remember(values) {
        values.joinToString(separator = ",") {
            App.getContext().actionText(it)
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