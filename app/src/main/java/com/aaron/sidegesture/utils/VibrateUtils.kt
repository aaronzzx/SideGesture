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
    fun vibrate(context: Context, vibrations: Vibrations = Vibrations()) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                vibrations.predefinedEffect != VibrationEffects.None
            ) {
                val effect = when (vibrations.predefinedEffect) {
                    VibrationEffects.Tick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    VibrationEffects.Click -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    VibrationEffects.HeavyClick -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    VibrationEffects.None -> error("Stub!")
                }
                vibrator.vibrate(effect)
            } else if (vibrations.customVibrationMs > 0) {
                val effect = VibrationEffect.createOneShot(vibrations.customVibrationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            }
        } else if (vibrations.customVibrationMs > 0) {
            vibrator.vibrate(vibrations.customVibrationMs)
        }
    }
}