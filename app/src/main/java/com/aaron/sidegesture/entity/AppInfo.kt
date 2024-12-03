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
    val label: String
)