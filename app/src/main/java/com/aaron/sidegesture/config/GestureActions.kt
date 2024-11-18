package com.aaron.sidegesture.config

import androidx.annotation.Keep
import com.aaron.sidegesture.ui.TriggerDirection

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/16
 */

object GestureActions {

    const val NONE = 0
    const val BACK = 1
    const val HOME = 2
    const val RECENT = 3
    const val MENU = 4
    const val SEARCH_IN_APP = 5
    const val VOLUME_UP = 6
    const val VOLUME_DOWN = 7
    const val MUTE = 8
    const val LOCK_SCREEN = 9
    const val PREVIOUS_APP = 10
}

@Keep
sealed class GestureAction<T>(val up: T, val center: T, val down: T) {

    @Keep
    class Single(
        up: Int = GestureActions.NONE,
        center: Int = GestureActions.NONE,
        down: Int = GestureActions.NONE
    ) : GestureAction<Int>(up, center, down)

    @Keep
    class Multiple(
        up: List<Int> = emptyList(),
        center: List<Int> = emptyList(),
        down: List<Int> = emptyList()
    ) : GestureAction<List<Int>>(up, center, down)

    fun select(direction: TriggerDirection): T {
        return when (direction) {
            TriggerDirection.Up -> up
            TriggerDirection.Center -> center
            TriggerDirection.Down -> down
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GestureAction<*>

        if (up != other.up) return false
        if (down != other.down) return false
        if (center != other.center) return false

        return true
    }

    override fun hashCode(): Int {
        var result = up?.hashCode() ?: 0
        result = 31 * result + (down?.hashCode() ?: 0)
        result = 31 * result + (center?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "GestureAction(up=$up, center=$center, down=$down)"
    }
}