package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

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
    @Serializable
    @Keep
    data class ShortcutInfo(
        val packageName: String,
        val className: String,
        val intents: List<String>,
        val label: String,
        val iconRes: Int = 0,
        val iconData: ByteArray? = null,
        val iconWidth: Int = 0,
        val iconHeight: Int = 0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ShortcutInfo

            if (iconRes != other.iconRes) return false
            if (iconWidth != other.iconWidth) return false
            if (iconHeight != other.iconHeight) return false
            if (packageName != other.packageName) return false
            if (className != other.className) return false
            if (intents != other.intents) return false
            if (label != other.label) return false
            if (iconData != null) {
                if (other.iconData == null) return false
                if (!iconData.contentEquals(other.iconData)) return false
            } else if (other.iconData != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = iconRes
            result = 31 * result + iconWidth
            result = 31 * result + iconHeight
            result = 31 * result + packageName.hashCode()
            result = 31 * result + className.hashCode()
            result = 31 * result + intents.hashCode()
            result = 31 * result + label.hashCode()
            result = 31 * result + (iconData?.contentHashCode() ?: 0)
            return result
        }
    }
}