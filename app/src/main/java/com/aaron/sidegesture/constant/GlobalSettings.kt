package com.aaron.sidegesture.constant

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ktx.whenVibrationEffect
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
object GlobalSettings {

    val MinGestureButtonWidth = ConvertUtils.dp2px(8f)
    val MaxGestureButtonWidth = ConvertUtils.dp2px(60f)
    val MinTriggerDistance = ConvertUtils.dp2px(10f)
    val MaxTriggerDistance = ConvertUtils.dp2px(200f)
    const val MinGestureButtonLength = 0.1f
    const val MaxGestureButtonLength = 1f
    const val MinGestureButtonStart = 0f
    const val MaxGestureButtonStart = MaxGestureButtonLength - MinGestureButtonLength
    const val MinLongPressTriggerDelayMs = 100L
    const val MaxLongPressTriggerDelayMs = 2000L
    const val MinVibrationDurationMs = 5L
    const val MaxVibrationDurationMs = 1000L
    const val GestureButtonColorAlpha = 0.2f
    const val DisabledAlpha = 0.36f
    const val DimAlpha = 0.5f

    @Composable
    fun getPredefinedVibrationEffectText(effect: Int): String {
        return whenVibrationEffect(
            onNone = { stringResource(id = R.string.custom) },
            onTick = { stringResource(id = R.string.vibration_tick) },
            onClick = { stringResource(id = R.string.vibration_click) },
            onHeavyClick = { stringResource(id = R.string.vibration_heavy_click) },
            effect = effect
        )
    }
}