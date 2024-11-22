package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.Actions
import com.aaron.sidegesture.entity.GestureActions

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

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