package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.ArcStyleItemSize
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.SectorStyleItemSize
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.Type
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

@Serializable
@Keep
data class ActionPanelStyles(
    val type: Int = Type,
    val json: String = ""
) {
    companion object {
        const val TYPE_ARC = ActionPanelStylesDefaults.TYPE_ARC
        const val TYPE_SECTOR = ActionPanelStylesDefaults.TYPE_SECTOR
    }

    @Transient
    val value: ActionPanelStyle = run {
        val json = json
        when (type) {
            TYPE_ARC -> if (json.isEmpty()) {
                ArcStyle()
            } else {
                JsonHelper.decodeFromString<ArcStyle>(json)
            }
            TYPE_SECTOR -> if (json.isEmpty()) {
                SectorStyle()
            } else {
                JsonHelper.decodeFromString<SectorStyle>(json)
            }
            else -> error("Unknown ActionPanelStyle type: $type")
        }
    }
}

sealed interface ActionPanelStyle

@Serializable
@Keep
data class ArcStyle(
    val itemSize: Int = ArcStyleItemSize
) : ActionPanelStyle

@Serializable
@Keep
data class SectorStyle(
    val itemSize: Int = SectorStyleItemSize
) : ActionPanelStyle
