package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

@Serializable
@Keep
data class AnimationStyles(
    val type: Int = TYPE_WAVE,
    val json: String = ""
) {
    companion object {
        const val TYPE_WAVE = 1
    }

    @Transient
    val value: AnimationStyle = run {
        val json = json
        if (json.isEmpty()) {
            return@run WaveStyle()
        }
        when (type) {
            TYPE_WAVE -> JsonHelper.decodeFromString<WaveStyle>(json)
            else -> error("Unknown AnimationStyle type: $type")
        }
    }
}

sealed interface AnimationStyle

@Serializable
@Keep
data class WaveStyle(
    val backgroundColor: Int = android.graphics.Color.BLACK,
    val strokeColor: Int = android.graphics.Color.TRANSPARENT,
    val strokeWidth: Int = 0,
    val iconColor: Int = android.graphics.Color.argb(200, 255, 255, 255)
) : AnimationStyle