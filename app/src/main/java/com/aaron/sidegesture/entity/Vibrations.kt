package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
@Serializable
@Keep
data class Vibrations(
    val pressEnabled: Boolean = true,
    val longPressEnabled: Boolean = true,
    val actionPanelEnabled: Boolean = true
)