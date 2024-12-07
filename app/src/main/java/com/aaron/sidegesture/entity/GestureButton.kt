package com.aaron.sidegesture.entity

import android.os.SystemClock
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.GestureButtonColorAlpha
import com.blankj.utilcode.util.ColorUtils
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
    val position: Position,
    val enabled: Boolean = true,
    val start: Float = 0.0f,
    val end: Float = 0.1f,
    val width: Int = ConvertUtils.dp2px(16f),
    val angle: GestureAngle = GestureAngle(),
    val slideActions: GestureActions = GestureActions(),
    val longSlideActions: GestureActions = GestureActions(),
    val slideTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longSlideTriggerDistance: Int = ConvertUtils.dp2px(100f),
    val longSlideTriggerImmediately: Boolean = true,
    val longSlideTriggerDelayMs: Long = 0L,
    val vibrations: Vibrations = Vibrations(),
    val color: Int = 0
) : Comparable<GestureButton> {

    companion object {
        private const val ID_DEFAULT = "1"

        val Defaults: List<GestureButton> = run {
            listOf(
                GestureButton(
                    id = ID_DEFAULT,
                    position = Position.Left,
                    start = 0.0f,
                    end = 1.0f,
                    slideActions = GestureActions(center = Action.toList(GlobalActions.BACK))
                ),
                GestureButton(
                    id = ID_DEFAULT,
                    position = Position.Right,
                    start = 0.0f,
                    end = 1.0f,
                    slideActions = GestureActions(center = Action.toList(GlobalActions.BACK))
                )
            )
        }

        fun createPair(): List<GestureButton> {
            val id = SystemClock.uptimeMillis().toString()
            val colorInt = ColorUtils.getRandomColor(false)
            val color = Color(colorInt).copy(alpha = GestureButtonColorAlpha).toArgb()
            val b1 = GestureButton(
                id = id,
                position = Position.Left,
                color = color
            )
            val b2 = GestureButton(
                id = id,
                position = Position.Right,
                color = color
            )
            return listOf(b1, b2)
        }
    }

    val isDefault: Boolean = id == ID_DEFAULT

    override fun compareTo(other: GestureButton): Int {
        val idCompared = id.compareTo(other.id)
        if (idCompared == 0) {
            // id相同，意味着是一组的，比较position
            return position.compareTo(other.position)
        }
        return idCompared
    }
}