package com.aaron.sidegesture.entity

import android.os.SystemClock
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.GestureButtonColorAlpha
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.serialization.Serializable
import kotlin.random.Random

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
    val end: Float = 0.1f,
    val width: Int = ConvertUtils.dp2px(16f),
    val angle: GestureAngle = GestureAngle(),
    val pressActions: GestureActions = GestureActions(),
    val longPressActions: GestureActions = GestureActions(),
    val pressTriggerDistance: Int = ConvertUtils.dp2px(30f),
    val longPressTriggerDistance: Int = ConvertUtils.dp2px(100f),
    val longPressTriggerImmediately: Boolean = true,
    val longPressTriggerDelayMs: Long = 100L,
    val vibrations: Vibrations = Vibrations(),
    val color: Int = 0
) : Comparable<GestureButton> {

    companion object {
        const val LEFT = 1
        const val RIGHT = 2

        private const val ID = "1"

        val Defaults = listOf(
            GestureButton(
                id = ID,
                position = LEFT,
                start = 0.0f,
                end = 1.0f,
                pressActions = GestureActions(
                    up = Actions.single(GlobalActions.LOCK_SCREEN),
                    center = Actions.single(GlobalActions.BACK)
                ),
                longPressActions = GestureActions(
                    up = Actions.multiple(
                        GlobalActions.WECHAT_SCAN,
                        GlobalActions.WECHAT_PAY,
                        GlobalActions.HOME,
                        GlobalActions.ALIPAY_SCAN,
                        GlobalActions.ALIPAY_PAY
                    ),
                    center = Actions.single(GlobalActions.PREVIOUS_APP)
                )
            ),
            GestureButton(
                id = ID,
                position = RIGHT,
                start = 0.0f,
                end = 1.0f,
                pressActions = GestureActions(
                    up = Actions.single(GlobalActions.LOCK_SCREEN),
                    center = Actions.single(GlobalActions.BACK)
                ),
                longPressActions = GestureActions(
                    up = Actions.multiple(
                        GlobalActions.WECHAT_SCAN,
                        GlobalActions.WECHAT_PAY,
                        GlobalActions.HOME,
                        GlobalActions.ALIPAY_SCAN,
                        GlobalActions.ALIPAY_PAY
                    ),
                    center = Actions.single(GlobalActions.PREVIOUS_APP)
                )
            )
        )

        fun createPair(): List<GestureButton> {
            val id = SystemClock.uptimeMillis().toString()
            val hue = Random(System.currentTimeMillis()).nextDouble(0.0, 360.0).toFloat()
            val color = Color.hsl(
                hue = hue,
                saturation = 0.5f,
                lightness = 0.5f,
                alpha = GestureButtonColorAlpha
            ).toArgb()
            val b1 = GestureButton(
                id = id,
                position = LEFT,
                color = color
            )
            val b2 = GestureButton(
                id = id,
                position = RIGHT,
                color = color
            )
            return listOf(b1, b2)
        }
    }

    val isDefault: Boolean = id == ID

    override fun compareTo(other: GestureButton): Int {
        val idCompared = id.compareTo(other.id)
        if (idCompared == 0) {
            // id相同，意味着是一组的，比较position
            return position.compareTo(other.position)
        }
        return idCompared
    }
}