package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */
@Serializable
@Keep
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val iconScale: Float = DEFAULT_SCALE
) {
    companion object {
        const val DEFAULT_SCALE = 1f
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 1.5f
    }
}