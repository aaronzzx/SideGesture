package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

sealed interface ActionPanelStyle

@Keep
data class ArcStyle(
    val itemSize: Int = ConvertUtils.dp2px(48f)
) : ActionPanelStyle