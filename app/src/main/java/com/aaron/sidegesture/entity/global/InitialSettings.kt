package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.InitialSettingsDefaults.GestureEnabled
import com.aaron.sidegesture.constant.InitialSettingsDefaults.Unlocked
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
@Serializable
@Keep
data class InitialSettings(
    val gestureEnabled: Boolean = GestureEnabled,
    val unlocked: Boolean = Unlocked
)
