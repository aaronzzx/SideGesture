package com.aaron.sidegesture.entity

import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */
sealed interface ActionPanelStyle {

    data class Arc(
        val itemSize: Int = ConvertUtils.dp2px(48f)
    ) : ActionPanelStyle
}