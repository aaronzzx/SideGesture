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
    val actionPanelEnabled: Boolean = true,
    // 识别到手势立即振动
    val vibrateImmediately: Boolean = true,
    val predefinedVibrationLevel: Int = EFFECT_CLICK,
    val customVibrationMs: Long = DEFAULT_MS
) {
    companion object {
        const val EFFECT_NONE = 0
        const val EFFECT_TICK = 1
        const val EFFECT_CLICK = 2
        const val EFFECT_HEAVY_CLICK = 3

        const val MIN_MS = 5L
        const val MAX_MS = 500L
        const val DEFAULT_MS = 50L
    }

    init {
        require(customVibrationMs in MIN_MS..MAX_MS) {
            "Illegal customVibrationMs: $customVibrationMs, min: $MIN_MS, max: $MAX_MS"
        }
    }
}