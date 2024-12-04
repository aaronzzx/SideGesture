package com.aaron.sidegesture.event

import com.aaron.sidegesture.entity.AppInfo

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */
data class IconResizeEvent(
    val appInfos: List<AppInfo>
)
