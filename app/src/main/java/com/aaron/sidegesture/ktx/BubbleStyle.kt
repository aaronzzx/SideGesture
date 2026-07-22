package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.aaron.sidegesture.entity.BubbleStyle
import com.aaron.sidegesture.entity.Position

/**
 * @author OpenAI
 * @since 2026/5/21
 */

@Composable
fun BubbleStyle.getBubbleIcon(): Painter {
    return getWaveStyleIcon(iconType)
}

fun BubbleStyle.getBubbleIconInitialRotation(position: Position): Float {
    return when (position) {
        Position.Left -> 0f
        Position.Right -> 180f
        Position.Bottom -> 270f
        Position.Top -> 90f
    }
}
