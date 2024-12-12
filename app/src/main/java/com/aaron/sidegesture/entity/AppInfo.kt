package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.AppInfoDefaults
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
    val iconScale: Float = AppInfoDefaults.DEFAULT_SCALE
) {
    companion object {
        const val DEFAULT_SCALE = AppInfoDefaults.DEFAULT_SCALE
        const val MIN_SCALE = AppInfoDefaults.MIN_SCALE
        const val MAX_SCALE = AppInfoDefaults.MAX_SCALE
    }
}