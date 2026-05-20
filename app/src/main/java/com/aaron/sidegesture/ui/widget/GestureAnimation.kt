package com.aaron.sidegesture.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.entity.AnimationStyle
import com.aaron.sidegesture.entity.CapsuleStyle
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.TriggerDirection.Center
import com.aaron.sidegesture.entity.TriggerDirection.Center2
import com.aaron.sidegesture.entity.TriggerDirection.Down
import com.aaron.sidegesture.entity.TriggerDirection.Down2
import com.aaron.sidegesture.entity.TriggerDirection.Up
import com.aaron.sidegesture.entity.TriggerDirection.Up2
import com.aaron.sidegesture.entity.WaveStyle
import com.aaron.sidegesture.ktx.getCapsuleIcon
import com.aaron.sidegesture.ktx.getCapsuleIconInitialRotation
import com.aaron.sidegesture.ktx.getIcon
import com.aaron.sidegesture.ktx.getIconInitialRotation

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

@Composable
fun GestureAnimation(
    animationStyle: AnimationStyle,
    SideGestureState: SideGestureState,
    modifier: Modifier = Modifier
) {
    when (animationStyle) {
        is WaveStyle -> WaveGestureAnimation(
            modifier = modifier,
            animationStyle = animationStyle,
            sideGestureState = SideGestureState
        )
        is CapsuleStyle -> CapsuleGestureAnimation(
            modifier = modifier,
            animationStyle = animationStyle,
            sideGestureState = SideGestureState
        )
    }
}

@Composable
private fun CapsuleGestureAnimation(
    animationStyle: CapsuleStyle,
    sideGestureState: SideGestureState,
    modifier: Modifier = Modifier
) {
    val button = sideGestureState.button ?: return
    val icon = animationStyle.getCapsuleIcon()

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

        val progress = when (button.position) {
            Position.Left -> fingerXAnimVal
            Position.Right -> -fingerXAnimVal
            Position.Bottom -> -fingerYAnimVal
        }.coerceAtLeast(0f)
        if (progress <= 1f) {
            return@Canvas
        }

        val thickness = animationStyle.thickness.toFloat().coerceAtLeast(1f)
        val strokeWidth = animationStyle.strokeWidth.toFloat()
        val maxLength = animationStyle.maxLength.toFloat().coerceAtLeast(thickness)
        val length = progress.coerceAtMost(maxLength).coerceAtLeast(thickness)
        val entryDistance = (thickness + strokeWidth * 2f).coerceAtLeast(1f)
        val entryProgress = (progress / entryDistance).coerceIn(0f, 1f)
        val centerShiftRatio = (progress / maxLength).coerceIn(0f, 1f) * 0.2f
        val centerX = when (button.position) {
            Position.Left, Position.Right -> 0f
            Position.Bottom -> (originXAnimVal + (fingerXAnimVal - originXAnimVal) * centerShiftRatio)
                .coerceIn(thickness / 2f, size.width - thickness / 2f)
        }
        val centerY = when (button.position) {
            Position.Left, Position.Right -> (originYAnimVal + (fingerYAnimVal - originYAnimVal) * centerShiftRatio)
                .coerceIn(thickness / 2f, size.height - thickness / 2f)
            Position.Bottom -> 0f
        }
        val leftStart = lerpFloat(
            start = -length - strokeWidth,
            stop = 0f,
            fraction = entryProgress
        )
        val rightStart = lerpFloat(
            start = size.width + strokeWidth,
            stop = size.width - length,
            fraction = entryProgress
        )
        val bottomStart = lerpFloat(
            start = size.height + strokeWidth,
            stop = size.height - length,
            fraction = entryProgress
        )
        val topLeft = when (button.position) {
            Position.Left -> Offset(leftStart, centerY - thickness / 2f)
            Position.Right -> Offset(rightStart, centerY - thickness / 2f)
            Position.Bottom -> Offset(centerX - thickness / 2f, bottomStart)
        }
        val rectSize = when (button.position) {
            Position.Left, Position.Right -> Size(length, thickness)
            Position.Bottom -> Size(thickness, length)
        }
        val radiusCap = minOf(rectSize.width, rectSize.height) / 2f
        val cornerRadius = animationStyle.cornerRadius.toFloat().coerceIn(0f, radiusCap)
        val activeAlpha = if (sideGestureState.canDistanceTriggered(button, false)) 1f else 0.55f
        val backgroundColor = Color(animationStyle.backgroundColor).copy(
            alpha = Color(animationStyle.backgroundColor).alpha * activeAlpha
        )
        val strokeColor = Color(animationStyle.strokeColor).copy(
            alpha = Color(animationStyle.strokeColor).alpha * activeAlpha
        )

        drawRoundRect(
            color = backgroundColor,
            topLeft = topLeft,
            size = rectSize,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
        if (animationStyle.strokeWidth > 0) {
            drawRoundRect(
                color = strokeColor,
                topLeft = topLeft,
                size = rectSize,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(animationStyle.strokeWidth.toFloat())
            )
        }

        val degree = animationStyle.getCapsuleIconInitialRotation(button.position) + when (sideGestureState.triggerDirection) {
            Up -> when (button.position) {
                Position.Left -> -45f
                Position.Right -> 45f
                Position.Bottom -> -45f
            }
            Center, Center2 -> 0f
            Down -> when (button.position) {
                Position.Left -> 45f
                Position.Right -> -45f
                Position.Bottom -> 45f
            }
            Up2 -> when (button.position) {
                Position.Left -> -90f
                Position.Right -> 90f
                Position.Bottom -> -90f
            }
            Down2 -> when (button.position) {
                Position.Left -> 90f
                Position.Right -> -90f
                Position.Bottom -> 90f
            }
        }
        val iconSize = minOf(rectSize.width, rectSize.height) * animationStyle.iconScale
        val rectCenter = Offset(
            x = topLeft.x + rectSize.width / 2f,
            y = topLeft.y + rectSize.height / 2f
        )
        rotate(degree, pivot = rectCenter) {
            translate(left = rectCenter.x - iconSize / 2f, top = rectCenter.y - iconSize / 2f) {
                icon.run {
                    draw(
                        size = Size(iconSize, iconSize),
                        colorFilter = ColorFilter.tint(Color(animationStyle.iconColor)),
                        alpha = activeAlpha
                    )
                }
            }
        }
    }
}

private fun lerpFloat(
    start: Float,
    stop: Float,
    fraction: Float
): Float {
    return start + (stop - start) * fraction
}

@Composable
private fun WaveGestureAnimation(
    animationStyle: WaveStyle,
    sideGestureState: SideGestureState,
    modifier: Modifier = Modifier
) {
    val button = sideGestureState.button ?: return
    val icon = animationStyle.getIcon()
    val bezierPath = remember { Path() }
    // 贝塞尔偏移值
    val bezierOffset = when (button.position) {
        // 使贝塞尔显示在手指落点上方
        Position.Left, Position.Right -> if (animationStyle.safeBounds) 70.dp.toPx() else 0f
        Position.Bottom -> 0f
    }
    // 贝塞尔与边界间距
    val bezierSpacing = if (animationStyle.safeBounds) 40.dp.toPx() else 0f
    // 贝塞尔的最大宽度
    val bezierMaxWidth = animationStyle.width.toFloat()
    // 贝塞尔长度的一半
    val bezierLengthHalf = bezierMaxWidth * animationStyle.bezierLengthHalfRatio
    // 贝塞尔沿边缘滑动变形约束
    val bezierTransformOffsetCoerce = if (animationStyle.transformEnabled) bezierLengthHalf / 2f else 0f

    Canvas(modifier = modifier) {
        val triggerDirection = sideGestureState.triggerDirection
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
            minimumValue = when (animationStyle.safeBounds) {
                true -> bezierLengthHalf + bezierSpacing
                else -> 0f
            },
            maximumValue = when (button.position) {
                Position.Left, Position.Right -> when (animationStyle.safeBounds) {
                    true -> size.height - bezierLengthHalf - bezierSpacing
                    else -> size.height
                }
                Position.Bottom -> when (animationStyle.safeBounds) {
                    true -> size.width - bezierLengthHalf - bezierSpacing
                    else -> size.width
                }
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

            // 避免边缘出现没覆盖全的白边
            val factor = 1.dp.toPx()
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
                            Position.Left -> -factor
                            else -> size.width + factor
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
                            Position.Left -> -factor
                            else -> size.width + factor
                        },
                        y3 = safeOrigin + bezierLengthHalf
                    )
                }
                Position.Bottom -> {
                    safeFingerX = safeOrigin - bezierLengthHalf / 2.5f - transformOffset
                    safeFingerY = (size.height + fingerYAnimVal).coerceAtLeast(size.height - bezierMaxWidth)
                    path.cubicTo(
                        x1 = safeFingerX,
                        y1 = size.height + factor,
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
                        y2 = size.height,
                        x3 = safeOrigin + bezierLengthHalf,
                        y3 = size.height + factor
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
        icon.run {
            val initialDegree = animationStyle.getIconInitialRotation(button.position)
            val degree = initialDegree + when (triggerDirection) {
                Up -> when (button.position) {
                    Position.Left -> -45f
                    Position.Right -> 45f
                    Position.Bottom -> -45f
                }
                Center, Center2 -> 0f
                Down -> when (button.position) {
                    Position.Left -> 45f
                    Position.Right -> -45f
                    Position.Bottom -> 45f
                }
                Up2 -> when (button.position) {
                    Position.Left -> -90f
                    Position.Right -> 90f
                    Position.Bottom -> -90f
                }
                Down2 -> when (button.position) {
                    Position.Left -> 90f
                    Position.Right -> -90f
                    Position.Bottom -> 90f
                }
            }
            rotate(degree, pivot = bezierBounds.center) {
                val radius = when (button.position) {
                    Position.Left, Position.Right -> bezierBounds.width * animationStyle.iconScale
                    Position.Bottom -> bezierBounds.height * animationStyle.iconScale
                }
                val paddingHori = (bezierBounds.width - radius) / 2f
                val paddingVert = (bezierBounds.height - radius) / 2f
                val left = when (button.position) {
                    Position.Left -> paddingHori - animationStyle.strokeWidth
                    Position.Right -> size.width - bezierBounds.width + paddingHori + animationStyle.strokeWidth
                    Position.Bottom -> bezierBounds.left + bezierBounds.width / 2f - radius / 2f
                }
                val top = when (button.position) {
                    Position.Left, Position.Right -> bezierBounds.top + bezierBounds.height / 2f - radius / 2f
                    Position.Bottom -> size.height - bezierBounds.height + paddingVert + animationStyle.strokeWidth
                }
                translate(left = left, top = top) {
                    val canTriggered = sideGestureState.canDistanceTriggered(button, false)
                    draw(
                        size = Size(radius, radius),
                        colorFilter = ColorFilter.tint(Color(animationStyle.iconColor)),
                        alpha = if (canTriggered) 1f else 0.25f
                    )
                }
            }
        }
    }
}
