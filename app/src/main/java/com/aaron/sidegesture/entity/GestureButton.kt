package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aaron.sidegesture.constant.GlobalActions
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */
@Serializable
@Keep
data class GestureButton(
    val id: String,
    val position: Int,
    val enabled: Boolean = true,
    val start: Float = 0.0f,
    val end: Float = 0.25f,
    val width: Int = ConvertUtils.dp2px(16f),
    val angle: GestureAngle = GestureAngle(),
    val pressActions: GestureActions = GestureActions(),
    val longPressActions: GestureActions = GestureActions(),
    val pressTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longPressTriggerDistance: Int = ConvertUtils.dp2px(100f),
    val longPressTriggerImmediately: Boolean = true,
    val longPressTriggerDelayMs: Long = 100L,
    val vibrations: Vibrations = Vibrations(),
    val color: Int = Color(0x99CFB3FF).toArgb()
) : Comparable<GestureButton> {
    companion object {
        const val LEFT = 1
        const val RIGHT = 2

        const val MIN_STEP = 0.1f

        private const val ID = "1"

        val Defaults = listOf(
            GestureButton(
                id = ID,
                position = LEFT,
                start = 0.0f,
                end = 1.0f,
                pressActions = GestureActions(center = Actions.single(GlobalActions.BACK))
            ),
            GestureButton(
                id = ID,
                position = RIGHT,
                start = 0.0f,
                end = 1.0f,
                pressActions = GestureActions(center = Actions.single(GlobalActions.BACK))
            )
        )
    }

    init {
        require(end - start >= MIN_STEP) {
            "The min step must large than $MIN_STEP"
        }
    }

    override fun compareTo(other: GestureButton): Int {
        val idCompared = id.compareTo(other.id)
        if (idCompared == 0) {
            // id相同，意味着是一组的，比较position
            return position.compareTo(other.position)
        }
        return idCompared
    }
}