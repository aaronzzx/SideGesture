package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GlobalSettings
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
@Serializable
@Keep
data class Vibrations(
    val slideEnabled: Boolean = true,
    val longSlideEnabled: Boolean = true,
    val actionPanelEnabled: Boolean = true,
    // 识别到手势立即振动
    val vibrateImmediately: Boolean = true,
    val predefinedEffect: Int = EFFECT_CLICK,
    val customVibrationMs: Long = 50L
) {
    companion object {
        const val EFFECT_NONE = 0
        const val EFFECT_TICK = 1
        const val EFFECT_CLICK = 2
        const val EFFECT_HEAVY_CLICK = 3
    }

    init {
        val min = GlobalSettings.MinVibrationDurationMs
        val max = GlobalSettings.MaxVibrationDurationMs
        require(customVibrationMs in min..max) {
            "Illegal customVibrationMs: $customVibrationMs, min: $min, max: $max"
        }
    }
}