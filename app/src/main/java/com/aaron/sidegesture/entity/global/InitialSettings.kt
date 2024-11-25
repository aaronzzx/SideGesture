package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
@Serializable
@Keep
data class InitialSettings(
    val gestureEnabled: Boolean = false,
    val unlocked: Boolean = false
)
