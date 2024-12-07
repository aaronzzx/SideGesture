package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.aaron.sidegesture.ktx.serializableType
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

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
    val isLongSlide: Boolean
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
    val position: Position
)

@Keep
@Serializable
data object GestureSettings

@Keep
@Serializable
data object Home

@Keep
@Serializable
data class IconResize(
    val appInfos: List<AppInfo>
) {
    companion object {
        val typeMap = mapOf(typeOf<List<AppInfo>>() to serializableType<List<AppInfo>>())

        fun toRoute(savedStateHandle: SavedStateHandle): IconResize {
            return savedStateHandle.toRoute(typeMap)
        }
    }
}

@Keep
@Serializable
data object Unlock