package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.AnimationStylesDefaults
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleBackgroundColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleCornerRadius
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleIconColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleIconScale
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleIconType
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleMaxLength
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleStrokeColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleStrokeWidth
import com.aaron.sidegesture.constant.AnimationStylesDefaults.CapsuleStyleThickness
import com.aaron.sidegesture.constant.AnimationStylesDefaults.IsAnimationEnabled
import com.aaron.sidegesture.constant.AnimationStylesDefaults.Type
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleBackgroundColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleBezierLengthHalfRatio
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleIconColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleIconScale
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleIconType
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleSafeBounds
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleStrokeColor
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleStrokeWidth
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleTransformEnabled
import com.aaron.sidegesture.constant.AnimationStylesDefaults.WaveStyleWidth
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
    val jsonMap: Map<Int, String> = emptyMap(),
    val isAnimationEnabled: Boolean = IsAnimationEnabled
) {
    companion object {
        const val TYPE_WAVE = AnimationStylesDefaults.TYPE_WAVE
        const val TYPE_CAPSULE = AnimationStylesDefaults.TYPE_CAPSULE
    }

    fun payloadOf(targetType: Int): String {
        return jsonMap[targetType].orEmpty().ifEmpty {
            if (targetType == type) json else ""
        }
    }

    fun selectType(targetType: Int): AnimationStyles {
        val nextJsonMap = if (json.isNotEmpty() && jsonMap[type].isNullOrEmpty()) {
            jsonMap + (type to json)
        } else {
            jsonMap
        }
        return copy(
            type = targetType,
            json = nextJsonMap[targetType].orEmpty(),
            jsonMap = nextJsonMap
        )
    }

    fun updateStyle(targetType: Int, payload: String): AnimationStyles {
        return copy(
            type = targetType,
            json = payload,
            jsonMap = jsonMap + (targetType to payload)
        )
    }

    @Transient
    val value: AnimationStyle = run {
        val json = payloadOf(type)
        when (type) {
            TYPE_WAVE -> runCatching {
                if (json.isEmpty()) WaveStyle() else JsonHelper.decodeFromString<WaveStyle>(json)
            }.getOrDefault(WaveStyle())
            TYPE_CAPSULE -> runCatching {
                if (json.isEmpty()) CapsuleStyle() else JsonHelper.decodeFromString<CapsuleStyle>(json)
            }.getOrDefault(CapsuleStyle())
            else -> WaveStyle()
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
    val width: Int = WaveStyleWidth,
    val bezierLengthHalfRatio: Float = WaveStyleBezierLengthHalfRatio,
    val safeBounds: Boolean = WaveStyleSafeBounds,
    val transformEnabled: Boolean = WaveStyleTransformEnabled,
    val iconColor: Int = WaveStyleIconColor,
    val iconScale: Float = WaveStyleIconScale,
    val iconType: Int = WaveStyleIconType,
    val stickySlideEnabled: Boolean = false
) : AnimationStyle {

    companion object {
        const val ICON_TYPE_ARROW = 1
        const val ICON_TYPE_TRIANGLE = 2
        const val ICON_TYPE_ANGLE = 3
        const val ICON_TYPE_ARROW_NEW = 4
    }
}

@Serializable
@Keep
data class CapsuleStyle(
    val backgroundColor: Int = CapsuleStyleBackgroundColor,
    val strokeColor: Int = CapsuleStyleStrokeColor,
    val strokeWidth: Int = CapsuleStyleStrokeWidth,
    val thickness: Int = CapsuleStyleThickness,
    val maxLength: Int = CapsuleStyleMaxLength,
    val cornerRadius: Int = CapsuleStyleCornerRadius,
    val iconColor: Int = CapsuleStyleIconColor,
    val iconScale: Float = CapsuleStyleIconScale,
    val iconType: Int = CapsuleStyleIconType
) : AnimationStyle
