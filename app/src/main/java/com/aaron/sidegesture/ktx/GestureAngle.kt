package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.GestureAngle

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

val GestureAngle.degree1: Float get() = BASE * p1
val GestureAngle.degree2: Float get() = BASE * p2
val GestureAngle.degree3: Float get() = BASE * p3
val GestureAngle.degree4: Float get() = BASE * p4

val GestureAngle.arcDegree1: Float get() = BASE * p1
val GestureAngle.arcDegree2: Float get() = BASE * (p2 - p1)
val GestureAngle.arcDegree3: Float get() = BASE * (p3 - p2)
val GestureAngle.arcDegree4: Float get() = BASE * (p4 - p3)
val GestureAngle.arcDegree5: Float get() = BASE * (1f - p4)

fun GestureAngle.getTriggerDirection(degree: Float): TriggerDirection? {
    return when (degree) {
        in degree1..degree2 -> TriggerDirection.Up
        in degree2..degree3 -> TriggerDirection.Center
        in degree3..degree4 -> TriggerDirection.Down
        else -> null
    }
}

private const val BASE = 180f