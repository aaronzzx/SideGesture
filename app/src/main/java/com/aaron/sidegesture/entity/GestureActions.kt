package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.config.Actions

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */
@Keep
sealed class GestureActions<T>(val up: T, val center: T, val down: T) {

    @Keep
    class Single(
        up: Int = Actions.NONE,
        center: Int = Actions.NONE,
        down: Int = Actions.NONE
    ) : GestureActions<Int>(up, center, down)

    @Keep
    class Multiple(
        up: List<Int> = emptyList(),
        center: List<Int> = emptyList(),
        down: List<Int> = emptyList()
    ) : GestureActions<List<Int>>(up, center, down)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GestureActions<*>

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