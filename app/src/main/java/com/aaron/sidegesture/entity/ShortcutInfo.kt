package com.aaron.sidegesture.entity

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.Keep
import com.aaron.sidegesture.constant.ScaleableDefaults.DEFAULT_SCALE
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author DS-Z
 * @since 2025/6/26
 */
@Serializable
@Keep
data class LauncherInfo (
    val packageName: String,
    val className: String,
    val label: String,
    val shortcuts: List<ShortcutInfo> = emptyList(),
) {
    @Parcelize
    @Serializable
    @Keep
    data class ShortcutInfo(
        val packageName: String,
        val className: String,
        val intents: List<String>,
        val label: String,
        val iconRes: Int = 0,
        val iconPath: String? = null,
        val iconScale: Float = DEFAULT_SCALE,
        val miniWindow: Boolean = false,
        val iconBgColor: Int = 0,
        @IgnoredOnParcel
        @Transient
        val iconBitmap: Bitmap? = null
    ) : Parcelable
}
