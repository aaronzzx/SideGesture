package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.ArcStyleItemSize
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleColumns
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleCornerRadius
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleHorizontalPadding
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleItemSize
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleItemSpacing
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleRows
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleScrollHotZoneHeight
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleScrollSpeed
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.FolderStyleVerticalPadding
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.SectorStyleInitialRadiusRatio
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.SectorStyleItemSize
import com.aaron.sidegesture.constant.ActionPanelStylesDefaults.SectorStyleItemSpacingRatio
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
    val json: String = "",
    val jsonMap: Map<Int, String> = emptyMap()
) {
    companion object {
        const val TYPE_ARC = ActionPanelStylesDefaults.TYPE_ARC
        const val TYPE_SECTOR = ActionPanelStylesDefaults.TYPE_SECTOR
        const val TYPE_FOLDER = ActionPanelStylesDefaults.TYPE_FOLDER
    }

    fun payloadOf(targetType: Int): String {
        return jsonMap[targetType].orEmpty().ifEmpty {
            if (targetType == type) json else ""
        }
    }

    fun selectType(targetType: Int): ActionPanelStyles {
        val nextJsonMap = if (json.isNotEmpty() && jsonMap[type].isNullOrEmpty()) {
            jsonMap + (type to json)
        } else {
            jsonMap
        }
        return copy(
            type = targetType,
            json = nextJsonMap[targetType].orEmpty(),
            jsonMap = nextJsonMap
        )
    }

    fun updateStyle(targetType: Int, payload: String): ActionPanelStyles {
        return copy(
            type = targetType,
            json = payload,
            jsonMap = jsonMap + (targetType to payload)
        )
    }

    @Transient
    val value: ActionPanelStyle = run {
        val json = payloadOf(type)
        when (type) {
            TYPE_ARC -> runCatching {
                if (json.isEmpty()) ArcStyle() else JsonHelper.decodeFromString<ArcStyle>(json)
            }.getOrDefault(ArcStyle())
            TYPE_SECTOR -> runCatching {
                if (json.isEmpty()) SectorStyle() else JsonHelper.decodeFromString<SectorStyle>(json)
            }.getOrDefault(SectorStyle())
            TYPE_FOLDER -> runCatching {
                if (json.isEmpty()) FolderStyle() else decodeFolderStyle(json)
            }.getOrDefault(FolderStyle())
            else -> FolderStyle()
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
    val itemSize: Int = SectorStyleItemSize,
    val initialRadiusRatio: Float = SectorStyleInitialRadiusRatio,
    val itemSpacingRatio: Float = SectorStyleItemSpacingRatio
) : ActionPanelStyle

@Serializable
@Keep
data class FolderStyle(
    val itemSize: Int = FolderStyleItemSize,
    val columns: Int = FolderStyleColumns,
    val rows: Int = FolderStyleRows,
    val itemSpacing: Int = FolderStyleItemSpacing,
    val horizontalPadding: Int = FolderStyleHorizontalPadding,
    val verticalPadding: Int = FolderStyleVerticalPadding,
    val cornerRadius: Int = FolderStyleCornerRadius,
    val scrollSpeed: Int = FolderStyleScrollSpeed,
    val scrollHotZoneHeight: Int = FolderStyleScrollHotZoneHeight
) : ActionPanelStyle

@Serializable
private data class LegacyFolderStyle(
    val itemSize: Int = FolderStyleItemSize,
    val columns: Int = FolderStyleColumns,
    val maxRows: Int = FolderStyleRows,
    val itemSpacing: Int = FolderStyleItemSpacing,
    val horizontalPadding: Int = FolderStyleHorizontalPadding,
    val verticalPadding: Int = FolderStyleVerticalPadding,
    val cornerRadius: Int = FolderStyleCornerRadius,
    val scrollSpeed: Int = FolderStyleScrollSpeed,
    val scrollHotZoneHeight: Int = FolderStyleScrollHotZoneHeight
)

private fun decodeFolderStyle(json: String): FolderStyle {
    return runCatching {
        JsonHelper.decodeFromString<FolderStyle>(json)
    }.getOrElse {
        val legacy = JsonHelper.decodeFromString<LegacyFolderStyle>(json)
        FolderStyle(
            itemSize = legacy.itemSize,
            columns = legacy.columns,
            rows = legacy.maxRows,
            itemSpacing = legacy.itemSpacing,
            horizontalPadding = legacy.horizontalPadding,
            verticalPadding = legacy.verticalPadding,
            cornerRadius = legacy.cornerRadius,
            scrollSpeed = legacy.scrollSpeed,
            scrollHotZoneHeight = legacy.scrollHotZoneHeight
        )
    }
}

fun normalizeActionPanelStyleType(type: Int): Int {
    return when (type) {
        ActionPanelStyles.TYPE_ARC,
        ActionPanelStyles.TYPE_SECTOR -> ActionPanelStyles.TYPE_SECTOR
        else -> ActionPanelStyles.TYPE_FOLDER
    }
}
