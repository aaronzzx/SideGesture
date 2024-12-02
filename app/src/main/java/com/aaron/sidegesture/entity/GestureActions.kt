package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GlobalActions
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@Serializable
@Keep
data class GestureActions(
    val up: Actions = Actions(),
    val center: Actions = Actions(),
    val down: Actions = Actions()
)

@Serializable
@Keep
data class Actions(private val actionValue: String = GlobalActions.NONE) {

    companion object {
        val NONE = Actions()

        fun create(vararg action: String): Actions {
            if (action.size > 1) {
                return Actions(action.joinToString(","))
            }
            val act = action.firstOrNull() ?: GlobalActions.NONE
            return Actions(act)
        }
    }

    val isLongActions: Boolean = actionValue.contains(",")

    @Transient
    val values: List<String> = run {
        val actionValue = actionValue
        if (actionValue.contains(",")) {
            return@run actionValue.split(",")
        }
        if (actionValue.isEmpty() || actionValue == GlobalActions.NONE) {
            return@run emptyList()
        }
        listOf(actionValue)
    }

    @Transient
    val value: String = run {
        if (values.isNotEmpty()) {
            return@run values[0]
        }
        actionValue
    }
}