package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.aaron.sidegesture.utils.JsonHelper
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

@Serializable
@Keep
data class ActionPanelStyles(
    val type: Int = TYPE_ARC,
    val json: String = ""
) {
    companion object {
        const val TYPE_ARC = 1
    }

    @Transient
    val value: ActionPanelStyle = run {
        val json = json
        if (json.isEmpty()) {
            return@run ArcStyle()
        }
        when (type) {
            TYPE_ARC -> JsonHelper.decodeFromString<ArcStyle>(json)
            else -> error("Unknown ActionPanelStyle type: $type")
        }
    }
}

sealed interface ActionPanelStyle

@Serializable
@Keep
data class ArcStyle(
    val itemSize: Int = ConvertUtils.dp2px(48f)
) : ActionPanelStyle