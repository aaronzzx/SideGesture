package com.aaron.sidegesture.constant

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.Vibrations
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
object GlobalSettings {

    val MinTriggerDistance = ConvertUtils.dp2px(10f)
    val MaxTriggerDistance = ConvertUtils.dp2px(200f)
    const val MinLongPressTriggerDelayMs = 100L
    const val MaxLongPressTriggerDelayMs = 2000L
    const val MinVibrationDurationMs = 5L
    const val MaxVibrationDurationMs = 1000L

    @Composable
    fun getPredefinedVibrationEffectText(effect: Int): String {
        return when (effect) {
            Vibrations.EFFECT_TICK -> stringResource(id = R.string.vibration_tick)
            Vibrations.EFFECT_CLICK -> stringResource(id = R.string.vibration_click)
            Vibrations.EFFECT_HEAVY_CLICK -> stringResource(id = R.string.vibration_heavy_click)
            Vibrations.EFFECT_NONE -> stringResource(id = R.string.custom)
            else -> error("Unknown vibration effect: $effect")
        }
    }
}