package com.aaron.sidegesture.entity

import androidx.annotation.Keep

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/16
 */
@Keep
data class GestureAngles(
    val p1: Float = 0.12f,
    val p2: Float = 0.40f,
    val p3: Float = 0.70f,
    val p4: Float = 0.88f
)