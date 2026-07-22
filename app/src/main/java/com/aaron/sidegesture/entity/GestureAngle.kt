package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.GestureAnglesDefaults.Bottom
import com.aaron.sidegesture.constant.GestureAnglesDefaults.Left
import com.aaron.sidegesture.constant.GestureAnglesDefaults.P1
import com.aaron.sidegesture.constant.GestureAnglesDefaults.P2
import com.aaron.sidegesture.constant.GestureAnglesDefaults.P3
import com.aaron.sidegesture.constant.GestureAnglesDefaults.P4
import com.aaron.sidegesture.constant.GestureAnglesDefaults.Right
import com.aaron.sidegesture.constant.GestureAnglesDefaults.Top
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/16
 */

@Serializable
@Keep
data class GestureAngles(
    val left: GestureAngle = Left,
    val right: GestureAngle = Right,
    val bottom: GestureAngle = Bottom,
    val top: GestureAngle = Top
)

@Serializable
@Keep
data class GestureAngle(
    val p1: Float = P1,
    val p2: Float = P2,
    val p3: Float = P3,
    val p4: Float = P4
) {

    val ps: List<Float> = listOf(p1, p2, p3, p4)

    init {
        require(p1 >= 0f && p1 <= p2 && p2 <= p3 && p3 <= p4 && p4 <= 1f) {
            "Illegal arguments: $p1, $p2, $p3, $p4, need 0<=p1<=p2<=p3<=p4<=1"
        }
    }
}
