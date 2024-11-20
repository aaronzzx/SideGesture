package com.aaron.sidegesture.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.entity.AnimationStyle
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.entity.TriggerDirection.Center
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Up

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

@Composable
fun GestureAnimation(
    animationStyle: AnimationStyle,
    sideGestureState: SideGestureState,
    modifier: Modifier = Modifier
) {
    when (animationStyle) {
        is AnimationStyle.Wave -> WaveGestureAnimation(
            modifier = modifier,
            animationStyle = animationStyle,
            sideGestureState = sideGestureState
        )
    }
}

@Composable
private fun WaveGestureAnimation(
    animationStyle: AnimationStyle.Wave,
    sideGestureState: SideGestureState,
    modifier: Modifier = Modifier
) {
    val arrowBack = rememberVectorPainter(Icons.Default.ArrowBack)
    val arrowForward = rememberVectorPainter(Icons.Default.ArrowForward)
    val bezierPath = remember { Path() }
    // 贝塞尔偏移值，使贝塞尔显示在手指落点上方
    val bezierOffset = 70.dp.toPx()
    // 贝塞尔与边界间距
    val bezierSpacing = 40.dp.toPx()
    // 贝塞尔的最大宽度
    val bezierMaxWidth = 40.dp.toPx()
    // 贝塞尔长度的一半
    val halfBezierLength = 100.dp.toPx()
    // 贝塞尔变形限制
    val offsetYCoerce = 55.dp.toPx()

    Canvas(modifier = modifier) {
        val originY = sideGestureState.originY
        val fingerX = sideGestureState.fingerX
        val fingerY = sideGestureState.fingerY
        if (originY.isNaN() || fingerX.isNaN() || fingerY.isNaN()) {
            return@Canvas
        }
        val button = sideGestureState.button ?: return@Canvas
        if (button.position == LEFT && fingerX < 0f) {
            return@Canvas
        } else if (button.position == RIGHT && fingerX > 0f) {
            return@Canvas
        }

        // 动画y轴偏移值
        val offsetY = (originY - fingerY).coerceIn(-offsetYCoerce, offsetYCoerce)
        // 能完整显示整个贝塞尔并且留有间距
        val safeOriginY = (originY - bezierOffset).coerceIn(
            minimumValue = halfBezierLength + bezierSpacing,
            maximumValue = size.height - halfBezierLength - bezierSpacing
        )
        bezierPath.also {
            it.reset()
            val moveToX = when (button.position == LEFT) {
                true -> 0f
                else -> size.width
            }
            val safeFingerX = when (button.position == LEFT) {
                true -> fingerX.coerceAtMost(bezierMaxWidth)
                else -> (size.width + fingerX).coerceAtLeast(size.width - bezierMaxWidth)
            }
            it.moveTo(moveToX, safeOriginY - halfBezierLength)
            it.cubicTo(
                x1 = when (button.position == LEFT) {
                    true -> -1f
                    else -> size.width + 1f
                },
                y1 = safeOriginY - halfBezierLength / 2.5f - offsetY,
                x2 = safeFingerX,
                y2 = safeOriginY - halfBezierLength / 2.5f - offsetY,
                x3 = safeFingerX,
                y3 = safeOriginY - offsetY
            )
            it.cubicTo(
                x1 = safeFingerX,
                y1 = safeOriginY + halfBezierLength / 2.5f - offsetY,
                x2 = when (button.position == LEFT) {
                    true -> 0f
                    else -> size.width
                },
                y2 = safeOriginY + halfBezierLength / 2.5f - offsetY,
                x3 = when (button.position == LEFT) {
                    true -> -1f
                    else -> size.width + 1f
                },
                y3 = safeOriginY + halfBezierLength
            )

            if (animationStyle.strokeWidth > 0) {
                val offset = when (button.position == LEFT) {
                    true -> Offset(-animationStyle.strokeWidth.toFloat(), 0f)
                    else -> Offset(animationStyle.strokeWidth.toFloat(), 0f)
                }
                it.translate(offset)
            }
        }
        // 绘制背景
        drawPath(path = bezierPath, color = Color(animationStyle.backgroundColor))
        if (animationStyle.strokeWidth > 0) {
            // 绘制轮廓
            drawPath(
                path = bezierPath,
                color = Color(animationStyle.strokeColor),
                style = Stroke(animationStyle.strokeWidth.toFloat())
            )
        }

        val bezierBounds = bezierPath.getBounds().translate(Offset(0f, -offsetY))
        // 默认图标
        val defaultIcon = when (button.position) {
            LEFT -> arrowForward
            else -> arrowBack
        }
        defaultIcon.run {
            val degree = when (sideGestureState.triggerDirection) {
                Up -> if (button.position == LEFT) -45f else 45f
                Center -> 0f
                Down -> if (button.position == LEFT) 45f else -45f
            }
            rotate(degree, pivot = bezierBounds.center) {
                val radius = bezierBounds.width * 0.6f
                val left = when (button.position) {
                    LEFT -> bezierBounds.width * 0.2f - animationStyle.strokeWidth
                    else -> size.width - bezierBounds.width * 0.8f + animationStyle.strokeWidth
                }
                val top = bezierBounds.top + bezierBounds.height / 2f - radius / 2f
                translate(left = left, top = top) {
                    draw(
                        size = Size(radius, radius),
                        colorFilter = ColorFilter.tint(Color(animationStyle.iconColor))
                    )
                }
            }
        }
    }
}