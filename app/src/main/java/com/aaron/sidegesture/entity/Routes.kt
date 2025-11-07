package com.aaron.sidegesture.entity

import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/7
 */

@Keep
@Serializable
data object About

@Keep
@Serializable
data class ActionSelect(
    val gestureButtonId: String,
    val position: Position,
    val direction: TriggerDirection,
    val isLongSlide: Boolean,
    val isSideButton: Boolean,
    val isOHOGesture: Boolean
)

@Keep
@Serializable
data object AdvancedSettings

@Keep
@Serializable
data object AppBlacklist

@Keep
@Serializable
data object AdjustGestureAngles

@Serializable
@Keep
data class GestureButtonSettings(
    val buttonId: String,
    val position: Position,
    val isSideButton: Boolean
)

@Keep
@Serializable
data object GestureSettings

@Keep
@Serializable
data object Home

@Keep
@Serializable
data class IconResize(val ids: List<String>) {

    companion object {
        val iconCache = mutableMapOf<String, Drawable>()
        val iconBgColorCache = mutableMapOf<String, Int>()
    }
}

@Keep
@Serializable
data object Unlock

@Keep
@Serializable
data object BugCollecting

@Keep
@Serializable
data object WaveAnimationStyle