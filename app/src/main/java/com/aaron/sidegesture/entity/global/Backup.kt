package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.entity.GestureButton
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/10
 */
@Serializable
@Keep
data class Backup(
    val initialSettings: InitialSettings? = null,
    val advancedSettings: AdvancedSettings? = null,
    val gestureSettings: GestureSettings? = null,
    val gestureButtons: List<GestureButton>? = null,
    val bottomGestureButtons: List<GestureButton>? = null,
    val timestamp: Long? = null,
    val version: String? = null
)
