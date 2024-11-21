package com.aaron.sidegesture.ktx

import android.Manifest.permission.VIBRATE
import androidx.annotation.RequiresPermission
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.utils.VibrateUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@RequiresPermission(VIBRATE)
fun Vibrations.tryVibrateForPress() {
    if (pressEnabled) {
        VibrateUtils.vibrate()
    }
}

@RequiresPermission(VIBRATE)
fun Vibrations.tryVibrateForLongPress() {
    if (longPressEnabled) {
        VibrateUtils.vibrate()
    }
}

@RequiresPermission(VIBRATE)
fun Vibrations.tryVibrateForActionPanel() {
    if (actionPanelEnabled) {
        VibrateUtils.vibrate()
    }
}