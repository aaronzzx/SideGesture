package com.aaron.sidegesture.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
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
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.TriggerDirection.Center
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Up
import com.aaron.sidegesture.entity.WaveStyle

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
        is WaveStyle -> WaveGestureAnimation(
            modifier = modifier,
            animationStyle = animationStyle,
            sideGestureState = sideGestureState
        )
    }
}

@Composable
private fun WaveGestureAnimation(
    animationStyle: WaveStyle,
    sideGestureState: SideGestureState,
    modifier: Modifier = Modifier
) {
    val button = sideGestureState.button ?: return
    val arrowBack = rememberVectorPainter(Icons.Default.ArrowBack)
    val arrowForward = rememberVectorPainter(Icons.Default.ArrowForward)
    val arrowUpward = rememberVectorPainter(Icons.Default.ArrowUpward)
    val bezierPath = remember { Path() }
    // 贝塞尔偏移值
    val bezierOffset = when (button.position) {
        // 使贝塞尔显示在手指落点上方
        Position.Left, Position.Right -> 70.dp.toPx()
        Position.Bottom -> 0f
    }
    // 贝塞尔与边界间距
    val bezierSpacing = 40.dp.toPx()
    // 贝塞尔的最大宽度
    val bezierMaxWidth = 40.dp.toPx()
    // 贝塞尔长度的一半
    val bezierLengthHalf = 100.dp.toPx()
    // 贝塞尔变形约束
    val bezierTransformOffsetCoerce = 55.dp.toPx()

    Canvas(modifier = modifier) {
        val originXAnimVal = sideGestureState.originXAnimVal
        val originYAnimVal = sideGestureState.originYAnimVal
        val fingerXAnimVal = sideGestureState.fingerXAnimVal
        val fingerYAnimVal = sideGestureState.fingerYAnimVal
        if (originXAnimVal.isNaN() ||
            originYAnimVal.isNaN() ||
            fingerXAnimVal.isNaN() ||
            fingerYAnimVal.isNaN()
        ) {
            return@Canvas
        }
        when (button.position) {
            Position.Left -> if (fingerXAnimVal < 0f) return@Canvas
            Position.Right -> if (fingerXAnimVal > 0f) return@Canvas
            Position.Bottom -> if (fingerYAnimVal > 0f) return@Canvas
        }

        // 贝塞尔形变偏移值
        val transformOffset = when (button.position) {
            Position.Left, Position.Right -> originYAnimVal - fingerYAnimVal
            Position.Bottom -> originXAnimVal - fingerXAnimVal
        }.coerceIn(-bezierTransformOffsetCoerce, bezierTransformOffsetCoerce)
        // 能完整显示整个贝塞尔并且留有间距
        val safeOrigin = when (button.position) {
            Position.Left, Position.Right -> originYAnimVal - bezierOffset
            Position.Bottom -> originXAnimVal - bezierOffset
        }.coerceIn(
            minimumValue = bezierLengthHalf + bezierSpacing,
            maximumValue = when (button.position) {
                Position.Left, Position.Right -> size.height - bezierLengthHalf - bezierSpacing
                Position.Bottom -> size.width - bezierLengthHalf - bezierSpacing
            }
        )
        bezierPath.also { path ->
            path.reset()

            val moveToX = when (button.position) {
                Position.Left -> 0f
                Position.Right -> size.width
                Position.Bottom -> safeOrigin - bezierLengthHalf
            }
            val moveToY = when (button.position) {
                Position.Left, Position.Right -> safeOrigin - bezierLengthHalf
                Position.Bottom -> size.height
            }
            path.moveTo(moveToX, moveToY)

            var safeFingerX: Float
            var safeFingerY: Float
            when (button.position) {
                Position.Left, Position.Right -> {
                    safeFingerX = when (button.position) {
                        Position.Left -> fingerXAnimVal.coerceAtMost(bezierMaxWidth)
                        else -> (size.width + fingerXAnimVal).coerceAtLeast(size.width - bezierMaxWidth)
                    }
                    safeFingerY = safeOrigin - bezierLengthHalf / 2.5f - transformOffset
                    path.cubicTo(
                        x1 = when (button.position) {
                            Position.Left -> -1f
                            else -> size.width + 1f
                        },
                        y1 = safeFingerY,
                        x2 = safeFingerX,
                        y2 = safeFingerY,
                        x3 = safeFingerX,
                        y3 = safeOrigin - transformOffset
                    )

                    safeFingerY = safeOrigin + bezierLengthHalf / 2.5f - transformOffset
                    path.cubicTo(
                        x1 = safeFingerX,
                        y1 = safeFingerY,
                        x2 = when (button.position) {
                            Position.Left -> 0f
                            else -> size.width
                        },
                        y2 = safeFingerY,
                        x3 = when (button.position) {
                            Position.Left -> -1f
                            else -> size.width + 1f
                        },
                        y3 = safeOrigin + bezierLengthHalf
                    )
                }
                Position.Bottom -> {
                    safeFingerX = safeOrigin - bezierLengthHalf / 2.5f - transformOffset
                    safeFingerY = (size.height + fingerYAnimVal).coerceAtLeast(size.height - bezierMaxWidth)
                    path.cubicTo(
                        x1 = safeFingerX,
                        y1 = size.height + 1f,
                        x2 = safeFingerX,
                        y2 = safeFingerY,
                        x3 = safeOrigin - transformOffset,
                        y3 = safeFingerY
                    )

                    safeFingerX = safeOrigin + bezierLengthHalf / 2.5f - transformOffset
                    path.cubicTo(
                        x1 = safeFingerX,
                        y1 = safeFingerY,
                        x2 = safeFingerX,
                        y2 = size.height + 1f,
                        x3 = safeOrigin + bezierLengthHalf,
                        y3 = size.height
                    )
                }
            }

            if (animationStyle.strokeWidth > 0) {
                val offset2 = when (button.position) {
                    Position.Left -> Offset(-animationStyle.strokeWidth.toFloat(), 0f)
                    Position.Right -> Offset(animationStyle.strokeWidth.toFloat(), 0f)
                    Position.Bottom -> Offset(0f, animationStyle.strokeWidth.toFloat())
                }
                path.translate(offset2)
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

        val bezierBounds = when (button.position) {
            Position.Left, Position.Right -> bezierPath.getBounds().translate(Offset(0f, -transformOffset))
            Position.Bottom -> bezierPath.getBounds().translate(Offset(-transformOffset, 0f))
        }
        // 默认图标
        val defaultIcon = when (button.position) {
            Position.Left -> arrowForward
            Position.Right -> arrowBack
            Position.Bottom -> arrowUpward
        }
        defaultIcon.run {
            val degree = when (sideGestureState.triggerDirection) {
                Up -> when (button.position) {
                    Position.Left -> -45f
                    Position.Right -> 45f
                    Position.Bottom -> -45f
                }
                Center -> 0f
                Down -> when (button.position) {
                    Position.Left -> 45f
                    Position.Right -> -45f
                    Position.Bottom -> 45f
                }
            }
            rotate(degree, pivot = bezierBounds.center) {
                val radius = when (button.position) {
                    Position.Left, Position.Right -> bezierBounds.width * 0.6f
                    Position.Bottom -> bezierBounds.height * 0.6f
                }
                val left = when (button.position) {
                    Position.Left -> bezierBounds.width * 0.2f - animationStyle.strokeWidth
                    Position.Right -> size.width - bezierBounds.width * 0.8f + animationStyle.strokeWidth
                    Position.Bottom -> bezierBounds.left + bezierBounds.width / 2f - radius / 2f
                }
                val top = when (button.position) {
                    Position.Left, Position.Right -> bezierBounds.top + bezierBounds.height / 2f - radius / 2f
                    Position.Bottom -> size.height - bezierBounds.height * 0.8f + animationStyle.strokeWidth
                }
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