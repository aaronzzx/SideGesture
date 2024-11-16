package com.aaron.sidegesture.config

import androidx.annotation.Keep
import com.aaron.sidegesture.ui.TriggerDirection

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/16
 */
@Keep
data class GestureAngles(
    val p1: Float = 0.12f,
    val p2: Float = 0.4f,
    val p3: Float = 0.75f,
    val p4: Float = 0.88f
) {
    companion object {
        private const val BASE = 180f
    }

    private val angle1: Float = BASE * p1
    private val angle2: Float = BASE * p2
    private val angle3: Float = BASE * p3
    private val angle4: Float = BASE * p4

    fun getTriggerDirection(degree: Float): TriggerDirection {
        if (degree in angle1..angle2) {
            return TriggerDirection.Up
        } else if (degree in angle3..angle4) {
            return TriggerDirection.Down
        }
        return TriggerDirection.Center
    }
}