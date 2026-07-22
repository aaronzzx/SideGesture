package com.aaron.sidegesture.ktx

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.WaveStyle

/**
 * @author DS-Z
 * @since 2025/11/5
 */

@Composable
fun WaveStyle.getIcon(): Painter {
    return getWaveStyleIcon(iconType)
}

@Composable
fun getWaveStyleIcon(iconType: Int): Painter {
    if (iconType == WaveStyle.ICON_TYPE_TRIANGLE) {
        return rememberVectorPainter(Icons.Default.PlayArrow)
    } else if (iconType == WaveStyle.ICON_TYPE_ANGLE) {
        return rememberVectorPainter(Icons.Default.ArrowForwardIos)
    } else if (iconType == WaveStyle.ICON_TYPE_ARROW_NEW) {
        return rememberVectorPainter(Icons.Default.Forward)
    }
    return rememberVectorPainter(Icons.Default.ArrowForward)
}

fun WaveStyle.getIconInitialRotation(position: Position): Float {
    return when (position) {
        Position.Left -> 0f
        Position.Right -> 180f
        Position.Bottom -> 270f
        Position.Top -> 90f
    }
}
