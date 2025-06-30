package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GestureActionsDefaults.ActionNone
import com.aaron.sidegesture.constant.GestureActionsDefaults.ActionValue
import com.aaron.sidegesture.constant.GestureActionsDefaults.Center
import com.aaron.sidegesture.constant.GestureActionsDefaults.Down
import com.aaron.sidegesture.constant.GestureActionsDefaults.Up
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@Serializable
@Keep
data class GestureActions(
    val center: List<Action> = Center,
    val up: List<Action> = Up,
    val down: List<Action> = Down
)

@Serializable
@Keep
data class Action(
    val value: String = ActionValue,
    val data: String = "",
    @Transient
    val extra: Any? = null
) {
    companion object {
        val NONE: Action get() = ActionNone

        fun toList(vararg value: String): List<Action> {
            return value.map { Action(it) }
        }
    }
}