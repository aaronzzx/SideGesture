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
    val MaxLongSlideTriggerDistance = ConvertUtils.dp2px(150f)
    const val MinBezierStrokeWidth = 0
    val MaxBezierStrokeWidth = ConvertUtils.dp2px(5f)
    val MinBezierWidth = ConvertUtils.dp2px(20f)
    val MaxBezierWidth = ConvertUtils.dp2px(80f)
    const val MinBezierLength = 1.8f
    const val MaxBezierLength = 4.0f
    const val MinIconScale = 0.0f
    const val MaxIconScale = 1.0f
    val MinCapsuleThickness = ConvertUtils.dp2px(20f)
    val MaxCapsuleThickness = ConvertUtils.dp2px(56f)
    val MinCapsuleLength = ConvertUtils.dp2px(40f)
    val MaxCapsuleLength = ConvertUtils.dp2px(120f)
    val MinCapsuleCornerRadius = ConvertUtils.dp2px(8f)
    val MaxCapsuleCornerRadius = ConvertUtils.dp2px(32f)
    val MinBubbleDiameter = ConvertUtils.dp2px(28f)
    val MaxBubbleDiameter = ConvertUtils.dp2px(72f)
    val MinBubbleOffset = ConvertUtils.dp2px(20f)
    val MaxBubbleOffset = ConvertUtils.dp2px(120f)
    val MinActionPanelItemSize = ConvertUtils.dp2px(32f)
    val MaxActionPanelItemSize = ConvertUtils.dp2px(64f)
    const val MinActionPanelInitialRadiusRatio = 1.1f
    const val MaxActionPanelInitialRadiusRatio = 3.0f
    const val MinActionPanelItemSpacingRatio = 1.1f
    const val MaxActionPanelItemSpacingRatio = 1.5f
    val MinActionPanelCornerRadius = ConvertUtils.dp2px(8f)
    val MaxActionPanelCornerRadius = ConvertUtils.dp2px(32f)
    const val MinActionPanelColumns = 1f
    const val MaxActionPanelColumns = 6f
    const val MinActionPanelRows = 1f
    const val MaxActionPanelRows = 10f
    const val MinActionPanelScrollSpeed = 12f
    const val MaxActionPanelScrollSpeed = 40f
    val MinActionPanelScrollHotZoneHeight = ConvertUtils.dp2px(16f).toFloat()
    val MaxActionPanelScrollHotZoneHeight = ConvertUtils.dp2px(56f).toFloat()
    const val MinMoveScreenRate = 1f
    const val MaxMoveScreenRate = 6f
    const val MinMoveScreenHover = 300f
    const val MaxMoveScreenHover = 1000f
    const val MinGotoBottomStrength = 1f
    const val MaxGotoBottomStrength = 20f
    // 小窗拖拽编辑器允许的最小窗口边长(dp)
    const val MinMiniWindowSizeDp = 100
    const val MinShellCommandTimeoutSec = 3f
    const val MaxShellCommandTimeoutSec = 30f
    const val MinGestureButtonPosition = 0f
    const val MaxGestureButtonPosition = 1f
    const val MinGestureButtonLength = 0.1f
    const val MinLongSlideTriggerDelayMs = 0L
    const val MaxLongSlideTriggerDelayMs = 1000L
    const val MinLongPressTriggerDelayMs = 100L
    const val MaxLongPressTriggerDelayMs = 1000L
    const val MinActionPanelAppSwitchWindowModeDelayMs = 500L
    const val MaxActionPanelAppSwitchWindowModeDelayMs = 2000L
    const val MinVibrationDurationMs = 0L
    const val MaxVibrationDurationMs = 100L
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
