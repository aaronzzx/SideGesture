package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GlobalSettings
import com.aaron.sidegesture.constant.VibrationDefaults.ActionPanelEnabled
import com.aaron.sidegesture.constant.VibrationDefaults.CustomVibrationMs
import com.aaron.sidegesture.constant.VibrationDefaults.LongSlideEnabled
import com.aaron.sidegesture.constant.VibrationDefaults.PredefinedEffect
import com.aaron.sidegesture.constant.VibrationDefaults.SlideEnabled
import com.aaron.sidegesture.constant.VibrationDefaults.VibrateImmediately
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
@Serializable
@Keep
data class Vibrations(
    val slideEnabled: Boolean = SlideEnabled,
    val longSlideEnabled: Boolean = LongSlideEnabled,
    val actionPanelEnabled: Boolean = ActionPanelEnabled,
    val vibrateImmediately: Boolean = VibrateImmediately,
    val predefinedEffect: VibrationEffects = PredefinedEffect,
    val customVibrationMs: Long = CustomVibrationMs
) {
    init {
        val min = GlobalSettings.MinVibrationDurationMs
        val max = GlobalSettings.MaxVibrationDurationMs
        require(customVibrationMs in min..max) {
            "Illegal customVibrationMs: $customVibrationMs, min: $min, max: $max"
        }
    }
}