package com.aaron.sidegesture.utils

import android.Manifest.permission.VIBRATE
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import com.aaron.sidegesture.entity.VibrationEffects
import com.aaron.sidegesture.entity.Vibrations

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
object VibrateUtils {

    @RequiresPermission(VIBRATE)
    fun vibrate(context: Context, vibrations: Vibrations) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val level = vibrations.predefinedEffect
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                when (level) {
                    VibrationEffects.None -> VibrationEffect.createOneShot(vibrations.customVibrationMs, 255)
                    VibrationEffects.Tick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    VibrationEffects.Click -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    VibrationEffects.HeavyClick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
            } else {
                VibrationEffect.createOneShot(vibrations.customVibrationMs, 255)
            }
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(vibrations.customVibrationMs)
        }
    }
}