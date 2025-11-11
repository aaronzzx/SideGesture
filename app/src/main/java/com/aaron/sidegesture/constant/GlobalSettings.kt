package com.aaron.sidegesture.constant

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.DayNightMode
import com.aaron.sidegesture.entity.VibrationEffects
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
object GlobalSettings {

    val MinGestureButtonWidth = ConvertUtils.dp2px(1f)
    val MaxGestureButtonWidth = ConvertUtils.dp2px(60f)
    val MinSlideTriggerDistance = ConvertUtils.dp2px(24f)
    val MaxSlideTriggerDistance = ConvertUtils.dp2px(40f)
    val MinLongSlideTriggerDistance = ConvertUtils.dp2px(80f)
    val MaxLongSlideTriggerDistance = ConvertUtils.dp2px(100f)
    const val MinBezierStrokeWidth = 0
    val MaxBezierStrokeWidth = ConvertUtils.dp2px(5f)
    val MinBezierWidth = ConvertUtils.dp2px(20f)
    val MaxBezierWidth = ConvertUtils.dp2px(80f)
    const val MinBezierLength = 1.8f
    const val MaxBezierLength = 4.0f
    const val MinIconScale = 0.0f
    const val MaxIconScale = 1.0f
    const val MinMoveScreenRate = 1f
    const val MaxMoveScreenRate = 3f
    const val MinGestureButtonPosition = 0f
    const val MaxGestureButtonPosition = 1f
    const val MinGestureButtonLength = 0.1f
    const val MinLongSlideTriggerDelayMs = 0L
    const val MaxLongSlideTriggerDelayMs = 250L
    const val MinLongPressTriggerDelayMs = 100L
    const val MaxLongPressTriggerDelayMs = 1000L
    const val MinVibrationDurationMs = 0L
    const val MaxVibrationDurationMs = 100L
    const val GestureButtonColorAlpha = 0.36f
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

    @Composable
    fun getDayNightModeText(dayNightMode: DayNightMode): String {
        return when (dayNightMode) {
            DayNightMode.Auto -> stringResource(id = R.string.day_night_mode_auto)
            DayNightMode.Day -> stringResource(id = R.string.day_night_mode_day)
            DayNightMode.Night -> stringResource(id = R.string.day_night_mode_night)
        }
    }
}