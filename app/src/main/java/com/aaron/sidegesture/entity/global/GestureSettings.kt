package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.entity.GestureAngles
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
    val pressTriggerImmediately: Boolean = false,
    val pressTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longPressTriggerImmediately: Boolean = true,
    val longPressTriggerDistance: Int = ConvertUtils.dp2px(100f),
    val longPressTriggerDelayMs: Long = 100L,
    // 识别到手势立即振动
    val vibrateImmediately: Boolean = true,
    val predefinedVibrationLevel: Int = 0,
    val customVibrationLevel: Int = -1
)