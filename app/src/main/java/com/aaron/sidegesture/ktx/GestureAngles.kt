package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.GestureAngles

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun GestureAngles.getTriggerDirection(degree: Float): TriggerDirection? {
    val base = BASE
    val angle1: Float = base * p1
    val angle2: Float = base * p2
    val angle3: Float = base * p3
    val angle4: Float = base * p4
    return when (degree) {
        in angle1..angle2 -> TriggerDirection.Up
        in angle2..angle3 -> TriggerDirection.Center
        in angle3..angle4 -> TriggerDirection.Down
        else -> null
    }
}

private const val BASE = 180f