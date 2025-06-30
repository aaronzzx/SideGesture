package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * @author DS-Z
 * @since 2025/6/30
 */
@Serializable
@Keep
data class ActionSettings(
    val moveScreen: MoveScreen = MoveScreen()
) {
    @Serializable
    @Keep
    data class MoveScreen(val rate: Float = 2f)
}