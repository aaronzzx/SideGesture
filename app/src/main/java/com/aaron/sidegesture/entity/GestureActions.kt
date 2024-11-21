package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.Actions

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

sealed interface GestureActions<T> {

    val up: T
    val center: T
    val down: T
}

@Keep
data class PressActions(
    override val up: Int = Actions.NONE,
    override val center: Int = Actions.NONE,
    override val down: Int = Actions.NONE
) : GestureActions<Int>

@Keep
data class LongPressActions(
    override val up: List<Int> = emptyList(),
    override val center: List<Int> = emptyList(),
    override val down: List<Int> = emptyList()
) : GestureActions<List<Int>>