package com.aaron.sidegesture.config

import androidx.annotation.Keep
import com.aaron.sidegesture.ui.TriggerDirection

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/16
 */
@Keep
data class GestureActions(
    val up: Int = NONE,
    val down: Int = NONE,
    val center: Int = NONE
) {
    companion object {
        const val NONE = 0
        const val BACK = 1
        const val HOME = 2
        const val RECENT = 3
        const val MENU = 4
        const val SEARCH_IN_APP = 5
        const val VOLUME_UP = 6
        const val VOLUME_DOWN = 7
        const val MUTE = 8
    }

    fun select(direction: TriggerDirection): Int {
        return when (direction) {
            TriggerDirection.Up -> up
            TriggerDirection.Center -> center
            TriggerDirection.Down -> down
        }
    }
}