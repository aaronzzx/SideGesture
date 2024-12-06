package com.aaron.sidegesture.constant

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aaron.sidegesture.R
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
object GlobalSettings {

    val MinGestureButtonWidth = ConvertUtils.dp2px(1f)
    val MaxGestureButtonWidth = ConvertUtils.dp2px(60f)
    val MinTriggerDistance = ConvertUtils.dp2px(10f)
    val MaxTriggerDistance = ConvertUtils.dp2px(200f)
    const val MinGestureButtonLength = 0.1f
    const val MaxGestureButtonLength = 1f
    const val MinGestureButtonStart = 0f
    const val MaxGestureButtonStart = MaxGestureButtonLength - MinGestureButtonLength
    const val MinLongPressTriggerDelayMs = 0L
    const val MaxLongPressTriggerDelayMs = 2000L
    const val MinVibrationDurationMs = 0L
    const val MaxVibrationDurationMs = 1000L
    const val GestureButtonColorAlpha = 0.2f
    const val DisabledAlpha = 0.36f
    const val DimAlpha = 0.5f

    @Composable
    fun getPredefinedVibrationEffectText(effect: VibrationEffects): String {
        return when (effect) {
            VibrationEffects.None -> stringResource(id = R.string.custom)
            VibrationEffects.Tick -> stringResource(id = R.string.vibration_tick)
            VibrationEffects.Click -> stringResource(id = R.string.vibration_click)
            VibrationEffects.HeavyClick -> stringResource(id = R.string.vibration_heavy_click)
        }
    }
}