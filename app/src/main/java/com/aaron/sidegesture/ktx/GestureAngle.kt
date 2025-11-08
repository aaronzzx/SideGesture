package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.TriggerDirection
import kotlin.reflect.KProperty0

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun GestureAngle.copyNew(
    fieldName: String,
    newP: Float,
    minGapP: Float
): GestureAngle {
    val notInEdgeMinGapP = minGapP * 2f
    return when (fieldName) {
        ::p1.name -> {
            val min = minGapP
            val max = p2 - notInEdgeMinGapP
            copy(p1 = newP.coerceIn(min, max))
        }
        ::p2.name -> {
            val min = p1 + notInEdgeMinGapP
            val max = (p3 - notInEdgeMinGapP).coerceAtMost(0.5f - minGapP)
            copy(p2 = newP.coerceIn(min, max))
        }
        ::p3.name -> {
            val min = (p2 + notInEdgeMinGapP).coerceAtLeast(0.5f + minGapP)
            val max = p4 - notInEdgeMinGapP
            copy(p3 = newP.coerceIn(min, max))
        }
        ::p4.name -> {
            val min = p3 + notInEdgeMinGapP
            val max = 1f - minGapP
            copy(p4 = newP.coerceIn(min, max))
        }
        else -> this
    }
}

fun GestureAngle.getKProperty(p: Float?): KProperty0<Float>? {
    return when (p) {
        p1 -> ::p1
        p2 -> ::p2
        p3 -> ::p3
        p4 -> ::p4
        else -> null
    }
}

fun GestureAngle.getDegree(index: Int): Float {
    return GESTURE_ANGLE_BASE * ps[index]
}

fun GestureAngle.getArcDegree(index: Int): Float {
    val base = GESTURE_ANGLE_BASE
    return when (index) {
        0 -> base * p1
        1 -> base * (p2 - p1)
        2 -> base * (p3 - p2)
        3 -> base * (p4 - p3)
        4 -> base * (1f - p4)
        else -> error("Unknown index: $index")
    }
}

fun GestureAngle.getDegrees(): List<Float> {
    return List(ps.size) { index ->
        getDegree(index)
    }
}

fun GestureAngle.getArcDegrees(): List<Float> {
    return List(ps.size + 1) { index ->
        getArcDegree(index)
    }
}

fun GestureAngle.getTriggerDirection(degree: Float): TriggerDirection? {
    return when {
        degree < getDegree(0) -> TriggerDirection.Up2
        degree in getDegree(0)..getDegree(1) -> TriggerDirection.Up
        degree in getDegree(1)..getDegree(2) -> TriggerDirection.Center
        degree in getDegree(2)..getDegree(3) -> TriggerDirection.Down
        degree > getDegree(3) -> TriggerDirection.Down2
        else -> TriggerDirection.Center2
    }
}

const val GESTURE_ANGLE_BASE = 180f