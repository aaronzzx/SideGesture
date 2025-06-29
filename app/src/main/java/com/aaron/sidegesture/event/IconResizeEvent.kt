package com.aaron.sidegesture.event

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */
data class IconResizeEvent(
    val scaleFactors: Map<String, Float>,
    val bgColors: Map<String, Int>
)
