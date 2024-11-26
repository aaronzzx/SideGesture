package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.GestureAngle

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

val GestureAngle.angle1: Float get() = BASE * p1
val GestureAngle.angle2: Float get() = BASE * p2
val GestureAngle.angle3: Float get() = BASE * p3
val GestureAngle.angle4: Float get() = BASE * p4

fun GestureAngle.getTriggerDirection(degree: Float): TriggerDirection? {
    return when (degree) {
        in angle1..angle2 -> TriggerDirection.Up
        in angle2..angle3 -> TriggerDirection.Center
        in angle3..angle4 -> TriggerDirection.Down
        else -> null
    }
}

private const val BASE = 180f