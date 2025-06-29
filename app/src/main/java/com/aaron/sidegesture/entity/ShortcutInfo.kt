package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.ScaleableDefaults.DEFAULT_SCALE
import kotlinx.serialization.Serializable

/**
 * @author DS-Z
 * @since 2025/6/26
 */
@Serializable
@Keep
data class LauncherInfo (
    val packageName: String,
    val className: String,
    val label: String,
    val shortcuts: List<ShortcutInfo> = emptyList(),
) {
    @Serializable
    @Keep
    data class ShortcutInfo(
        val packageName: String,
        val className: String,
        val intents: List<String>,
        val label: String,
        val iconRes: Int = 0,
        val iconPath: String? = null,
        val iconScale: Float = DEFAULT_SCALE,
        val iconBgColor: Int = 0
    )
}