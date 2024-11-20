package com.aaron.sidegesture.ktx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */


fun Offset.toIntOffset() = IntOffset(x.toInt(), y.toInt())