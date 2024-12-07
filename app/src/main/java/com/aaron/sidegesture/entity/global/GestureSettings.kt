package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.entity.GestureAngles
import com.aaron.sidegesture.entity.Vibrations
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
@Serializable
@Keep
data class GestureSettings(
    val angles: GestureAngles = GestureAngles(),
    val slideTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longSlideTriggerImmediately: Boolean = true,
    val longSlideTriggerDistance: Int = ConvertUtils.dp2px(100f),
    val longSlideTriggerDelayMs: Long = 0L,
    val isCustomVibration: Boolean = false,
    val vibrations: Vibrations = Vibrations()
)