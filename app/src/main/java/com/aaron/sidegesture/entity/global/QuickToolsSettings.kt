package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.ActionSettingsDefaults.QuickToolItems
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class QuickToolsSettings(
    val items: List<QuickToolItem> = QuickToolItems
)

@Serializable
@Keep
data class QuickToolItem(
    val type: QuickToolType,
    val enabled: Boolean = true
)

@Serializable
@Keep
enum class QuickToolType {
    MediaControl,
    Brightness,
    Volume,
    Flashlight,
    Mute,
    Wifi,
    Bluetooth,
    NotificationPanel,
    QuickSettingsPanel,
    LockScreen,
    Screenshot
}
