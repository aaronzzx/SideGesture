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
fun Vibrations.vibrate() {
    VibrateUtils.vibrate()
}