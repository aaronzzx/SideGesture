package com.aaron.sidegesture.entity

import android.os.SystemClock
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aaron.sidegesture.constant.GestureButtonDefaults
import com.aaron.sidegesture.constant.GestureButtonDefaults.AlignRegion
import com.aaron.sidegesture.constant.GestureButtonDefaults.Angle
import com.aaron.sidegesture.constant.GestureButtonDefaults.Color
import com.aaron.sidegesture.constant.GestureButtonDefaults.Enabled
import com.aaron.sidegesture.constant.GestureButtonDefaults.End
import com.aaron.sidegesture.constant.GestureButtonDefaults.LongSlideActions
import com.aaron.sidegesture.constant.GestureButtonDefaults.LongSlideTriggerDelayMs
import com.aaron.sidegesture.constant.GestureButtonDefaults.LongSlideTriggerDistance
import com.aaron.sidegesture.constant.GestureButtonDefaults.LongSlideTriggerImmediately
import com.aaron.sidegesture.constant.GestureButtonDefaults.SlideActions
import com.aaron.sidegesture.constant.GestureButtonDefaults.SlideTriggerDistance
import com.aaron.sidegesture.constant.GestureButtonDefaults.Start
import com.aaron.sidegesture.constant.GestureButtonDefaults.Vibrations
import com.aaron.sidegesture.constant.GestureButtonDefaults.Width
import com.aaron.sidegesture.constant.GestureSettingsDefaults.LongPressTriggerDelayMs
import com.blankj.utilcode.util.ColorUtils
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
    val enabled: Boolean = Enabled,
    val start: Float = Start,
    val end: Float = End,
    val width: Int = Width,
    val angle: GestureAngle = Angle,
    val slideActions: GestureActions = SlideActions,
    val longSlideActions: GestureActions = LongSlideActions,
    val slideTriggerDistance: Int = SlideTriggerDistance,
    val longPressTriggerDelayMs: Long = LongPressTriggerDelayMs,
    val longSlideTriggerDistance: Int = LongSlideTriggerDistance,
    val longSlideTriggerImmediately: Boolean = LongSlideTriggerImmediately,
    val longSlideTriggerDelayMs: Long = LongSlideTriggerDelayMs,
    val vibrations: Vibrations = Vibrations,
    val color: Int = Color,
    val alignRegion: Boolean = AlignRegion
) : Comparable<GestureButton> {

    companion object {
        private const val ID_DEFAULT = GestureButtonDefaults.ID_DEFAULT

        val SideDefaults: List<GestureButton> get() = GestureButtonDefaults.SideDefaults
        val BottomDefaults: List<GestureButton> get() = GestureButtonDefaults.BottomDefaults

        fun createSidePair(): List<GestureButton> {
            val id = SystemClock.uptimeMillis().toString()
            val colorInt = ColorUtils.getRandomColor(false)
            val color = Color(colorInt).toArgb()
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

        fun createBottom(): GestureButton {
            val id = SystemClock.uptimeMillis().toString()
            val colorInt = ColorUtils.getRandomColor(false)
            val color = Color(colorInt).toArgb()
            return GestureButton(
                id = id,
                position = Position.Bottom,
                color = color
            )
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