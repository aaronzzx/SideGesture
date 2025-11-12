package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.entity.global.ActionSettings
import kotlinx.serialization.Serializable

/**
 * @author DS-Z
 * @since 2025/11/12
 */
@Serializable
@Keep
data class MoveScreenData(
    val x: Int,
    val y: Int,
    val action: ActionSettings.MoveScreen.Action? = ActionSettings.MoveScreen.Action.Tap
)