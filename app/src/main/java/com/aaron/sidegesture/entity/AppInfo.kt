package com.aaron.sidegesture.entity

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Any?,
    val isUserApp: Boolean
)