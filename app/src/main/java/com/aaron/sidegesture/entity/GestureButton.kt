package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */
@Keep
data class GestureButton(
    val position: Int,
    val start: Float = 0f,
    val end: Float = 1f,
    val width: Int = ConvertUtils.dp2px(16f),
    val angles: GestureAngles = GestureAngles(),
    val pressAction: GestureActions<Int> = GestureActions.Single(),
    val longPressAction: GestureActions<List<Int>> = GestureActions.Multiple(),
    val pressTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longPressTriggerDistance: Int = ConvertUtils.dp2px(100f),
    val longPressTriggerDelayMs: Long = 100L,
    val longPressNeedFingerUp: Boolean = false,
    val vibrations: Vibrations = Vibrations(),
    val color: Int = android.graphics.Color.argb(255, 0, 0, 255)
) {
    companion object {
        const val LEFT = 1
        const val RIGHT = 2
    }
}