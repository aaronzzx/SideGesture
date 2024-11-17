package com.aaron.sidegesture.utils

import android.Manifest.permission.VIBRATE
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import com.aaron.sidegesture.App

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
object VibratorUtils {

    @RequiresPermission(VIBRATE)
    fun vibrate() {
        val context = App.getContext()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}