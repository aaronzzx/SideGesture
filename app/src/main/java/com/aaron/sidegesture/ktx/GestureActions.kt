package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.GestureActions

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