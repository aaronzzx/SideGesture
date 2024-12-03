package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GlobalActions
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@Serializable
@Keep
data class GestureActions(
    val center: List<Action> = emptyList(),
    val up: List<Action> = emptyList(),
    val down: List<Action> = emptyList()
)

@Serializable
@Keep
data class Action(
    val value: String = GlobalActions.NONE,
    val data: String = ""
) {
    companion object {
        val NONE = Action(value = GlobalActions.NONE, data = "")

        fun toList(vararg value: String): List<Action> {
            return value.map { Action(it) }
        }
    }
}