package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.aaron.sidegesture.entity.CapsuleStyle
import com.aaron.sidegesture.entity.Position

/**
 * @author OpenAI
 * @since 2026/5/20
 */

@Composable
fun CapsuleStyle.getCapsuleIcon(): Painter {
    return getWaveStyleIcon(iconType)
}

fun CapsuleStyle.getCapsuleIconInitialRotation(position: Position): Float {
    return when (position) {
        Position.Left -> 0f
        Position.Right -> 180f
        Position.Bottom -> 270f
        Position.Top -> 90f
    }
}
