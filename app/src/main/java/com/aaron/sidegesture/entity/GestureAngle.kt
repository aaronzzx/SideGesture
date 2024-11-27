package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/16
 */

@Serializable
@Keep
data class GestureAngles(
    val left: GestureAngle = GestureAngle(),
    val right: GestureAngle = GestureAngle()
)

@Serializable
@Keep
data class GestureAngle(
    val p1: Float = 0.12f,
    val p2: Float = 0.40f,
    val p3: Float = 0.70f,
    val p4: Float = 0.88f
) {

    val ps: List<Float> = listOf(p1, p2, p3, p4)

    init {
        require(p1 >= 0f && p1 <= p2 && p2 <= p3 && p3 <= p4 && p4 <= 1f) {
            "Illegal arguments: $p1, $p2, $p3, $p4, need 0<=p1<=p2<=p3<=p4<=1"
        }
    }
}