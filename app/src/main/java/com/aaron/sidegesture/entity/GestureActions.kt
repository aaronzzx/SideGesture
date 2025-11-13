package com.aaron.sidegesture.entity

import android.os.Parcelable
import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GestureActionsDefaults.ActionNone
import com.aaron.sidegesture.constant.GestureActionsDefaults.ActionValue
import com.aaron.sidegesture.constant.GestureActionsDefaults.Center
import com.aaron.sidegesture.constant.GestureActionsDefaults.Center2
import com.aaron.sidegesture.constant.GestureActionsDefaults.Down
import com.aaron.sidegesture.constant.GestureActionsDefaults.Down2
import com.aaron.sidegesture.constant.GestureActionsDefaults.Up
import com.aaron.sidegesture.constant.GestureActionsDefaults.Up2
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@Serializable
@Keep
data class GestureActions(
    // oho
    val center: List<Action> = Center,
    val up: List<Action> = Up,
    val down: List<Action> = Down,

    // 平行手势
    val center2: List<Action> = Center2,
    val up2: List<Action> = Up2,
    val down2: List<Action> = Down2
)

@Parcelize
@Serializable
@Keep
data class Action(
    val value: String = ActionValue,
    val data: String = "",
    @IgnoredOnParcel
    @Transient
    val extra: Any? = null
) : Parcelable {
    companion object {
        val NONE: Action get() = ActionNone

        fun toList(vararg value: String): List<Action> {
            return value.map { Action(it) }
        }
    }
}