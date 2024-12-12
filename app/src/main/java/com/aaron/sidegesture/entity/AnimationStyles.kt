package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.AnimationStylesDefaults
import com.aaron.sidegesture.constant.AnimationStylesDefaults.IsAnimationEnabled
import com.aaron.sidegesture.constant.AnimationStylesDefaults.Type
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleBackgroundColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleIconColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleStrokeColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleStrokeWidth
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
    val type: Int = Type,
    val json: String = "",
    val isAnimationEnabled: Boolean = IsAnimationEnabled
) {
    companion object {
        const val TYPE_WAVE = AnimationStylesDefaults.TYPE_WAVE
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
    val backgroundColor: Int = WaveStyleBackgroundColor,
    val strokeColor: Int = WaveStyleStrokeColor,
    val strokeWidth: Int = WaveStyleStrokeWidth,
    val iconColor: Int = WaveStyleIconColor
) : AnimationStyle