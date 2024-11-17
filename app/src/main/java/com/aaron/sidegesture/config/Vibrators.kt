package com.aaron.sidegesture.config

import android.Manifest.permission.VIBRATE
import androidx.annotation.RequiresPermission
import com.aaron.sidegesture.utils.VibratorUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
data class Vibrators(
    val forPress: Boolean = true,
    val forLongPress: Boolean = true,
    val forActionPanel: Boolean = true
) {
    @RequiresPermission(VIBRATE)
    fun vibrate() {
        VibratorUtils.vibrate()
    }
}
