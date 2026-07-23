package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GestureSettingsDefaults.Angles
import com.aaron.sidegesture.constant.GestureSettingsDefaults.IsCustomVibration
import com.aaron.sidegesture.constant.GestureSettingsDefaults.IsPreciseSlideType
import com.aaron.sidegesture.constant.GestureSettingsDefaults.LongPressTriggerDelayMs
import com.aaron.sidegesture.constant.GestureSettingsDefaults.LongSlideTriggerDelayMs
import com.aaron.sidegesture.constant.GestureSettingsDefaults.LongSlideTriggerDistance
import com.aaron.sidegesture.constant.GestureSettingsDefaults.LongSlideTriggerImmediately
import com.aaron.sidegesture.constant.GestureSettingsDefaults.SlideTriggerDistance
import com.aaron.sidegesture.constant.GestureSettingsDefaults.Vibrations
import com.aaron.sidegesture.entity.GestureAngles
import com.aaron.sidegesture.entity.Vibrations
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
@Serializable
@Keep
data class GestureSettings(
    val angles: GestureAngles = Angles,
    val slideTriggerDistance: Int = SlideTriggerDistance,
    val longPressTriggerDelayMs: Long = LongPressTriggerDelayMs,
    val longSlideTriggerDistance: Int = LongSlideTriggerDistance,
    val longSlideTriggerImmediately: Boolean = LongSlideTriggerImmediately,
    val longSlideTriggerDelayMs: Long = LongSlideTriggerDelayMs,
    val isCustomVibration: Boolean = IsCustomVibration,
    val vibrations: Vibrations = Vibrations,
    val isPreciseSlideType: Boolean = IsPreciseSlideType
)
