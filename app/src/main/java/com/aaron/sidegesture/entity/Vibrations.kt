package com.aaron.sidegesture.entity

import androidx.annotation.Keep

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/17
 */
@Keep
data class Vibrations(
    val forPress: Boolean = true,
    val forLongPress: Boolean = true,
    val forActionPanel: Boolean = true
)