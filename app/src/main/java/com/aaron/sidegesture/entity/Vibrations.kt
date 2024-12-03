package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GlobalSettings
import com.aaron.sidegesture.constant.VibrationEffects
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
    val predefinedEffect: VibrationEffects = VibrationEffects.Click,
    val customVibrationMs: Long = 50L
) {
    init {
        val min = GlobalSettings.MinVibrationDurationMs
        val max = GlobalSettings.MaxVibrationDurationMs
        require(customVibrationMs in min..max) {
            "Illegal customVibrationMs: $customVibrationMs, min: $min, max: $max"
        }
    }
}