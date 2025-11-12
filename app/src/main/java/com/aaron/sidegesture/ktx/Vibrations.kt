package com.aaron.sidegesture.ktx

import android.Manifest.permission.VIBRATE
import androidx.annotation.RequiresPermission
import com.aaron.sidegesture.App
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.utils.VibrateUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

@RequiresPermission(VIBRATE)
fun Vibrations.tryVibrateForSlide() {
    if (slideEnabled) {
        VibrateUtils.vibrate(App.getContext(), this)
    }
}

@RequiresPermission(VIBRATE)
fun Vibrations.tryVibrateForLongSlide() {
    if (longSlideEnabled) {
        VibrateUtils.vibrate(App.getContext(), this)
    }
}

@RequiresPermission(VIBRATE)
fun Vibrations.tryVibrateForActionPanel() {
    if (actionPanelEnabled) {
        VibrateUtils.vibrate(App.getContext(), this)
    }
}

@RequiresPermission(VIBRATE)
fun Vibrations.tryVibrateForMoveScreen() {
    if (moveScreenEnabled) {
        VibrateUtils.vibrate(App.getContext(), this)
    }
}