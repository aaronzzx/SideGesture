package com.aaron.sidegesture.entity

import androidx.annotation.Keep

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

sealed interface AnimationStyle

@Keep
data class WaveStyle(
    val backgroundColor: Int = android.graphics.Color.BLACK,
    val strokeColor: Int = android.graphics.Color.TRANSPARENT,
    val strokeWidth: Int = 0,
    val iconColor: Int = android.graphics.Color.argb(200, 255, 255, 255)
) : AnimationStyle