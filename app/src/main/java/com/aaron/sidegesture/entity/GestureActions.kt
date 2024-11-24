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

        fun single(action: String): Actions {
            return Actions(action)
        }

        fun multiple(vararg actions: String): Actions {
            return Actions(actions.joinToString(","))
        }
    }

    @Transient
    val values: List<String> = run {
        val actionValue = actionValue
        if (actionValue.contains(",")) {
            return@run actionValue.split(",")
        }
        emptyList()
    }

    @Transient
    val value: String = run {
        if (values.isNotEmpty()) {
            return@run values[0]
        }
        actionValue
    }
}