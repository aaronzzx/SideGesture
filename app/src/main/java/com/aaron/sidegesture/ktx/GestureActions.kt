package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.ui.TriggerDirection

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun <T> GestureActions<T>.actionBy(direction: TriggerDirection): T {
    return when (direction) {
        TriggerDirection.Up -> up
        TriggerDirection.Center -> center
        TriggerDirection.Down -> down
    }
}