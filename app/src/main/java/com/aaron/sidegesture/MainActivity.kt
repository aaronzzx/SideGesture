package com.aaron.sidegesture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import kotlin.math.asin
import kotlin.math.hypot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideGestureTheme {
                SideSlideContainer()
            }
        }
    }
}

@Composable
private fun SideSlideContainer() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFF7F7F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = AbsoluteAlignment.Left
        ) {
            val density = LocalDensity.current
            val maxWidth = 40.dp
            var firstPoint: Offset by remember { mutableStateOf(Offset.Zero) }
            var secondPoint: Offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(600.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                firstPoint = offset.copy(x = 0f)
                                secondPoint = offset.copy(x = 0f)
                            },
                            onDrag = { change, dragAmount ->
                                density.run {
                                    val temp = secondPoint + dragAmount
                                    if (temp.x >= maxWidth.toPx() / 2f) {
                                        secondPoint += dragAmount / 3f
                                    } else {
                                        secondPoint = temp
                                    }
                                }
                            },
                            onDragEnd = {
                                firstPoint = Offset.Zero
                                secondPoint = Offset.Zero
                            },
                            onDragCancel = {
                                firstPoint = Offset.Zero
                                secondPoint = Offset.Zero
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .drawBehind {
                            if (firstPoint != Offset.Zero && secondPoint != Offset.Zero) {
                                val maxWidth = maxWidth.toPx()
                                val bezierRadius = 80.dp.toPx()
                                val firstPoint = firstPoint.copy(
                                    y = firstPoint.y - 48.dp.toPx()
                                )
                                val secondPoint = secondPoint.copy(
                                    x = secondPoint.x.coerceAtMost(maxWidth),
                                    y = secondPoint.y - 48.dp.toPx()
                                )
                                val path = Path().also {
                                    it.moveTo(0f, firstPoint.y - bezierRadius)
                                    it.cubicTo(
                                        x1 = 4f,
                                        y1 = firstPoint.y - bezierRadius / 2f,
                                        x2 = secondPoint.x,
                                        y2 = firstPoint.y - bezierRadius / 2f,
                                        x3 = secondPoint.x,
                                        y3 = firstPoint.y
                                    )
                                    it.cubicTo(
                                        x1 = secondPoint.x,
                                        y1 = firstPoint.y + bezierRadius / 2f,
                                        x2 = 4f,
                                        y2 = firstPoint.y + bezierRadius / 2f,
                                        x3 = 0f,
                                        y3 = firstPoint.y + bezierRadius
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Black
                                )
                                drawPath(
                                    path = path,
                                    color = Color(0xFFBDBDBD),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                val firstY = firstPoint.y
                                val secondX = secondPoint.x
                                val secondY = secondPoint.y
                                val offsetY = secondY - firstY
                                val hypotenuse = hypot(offsetY, secondX)
                                val radians = asin(secondX / hypotenuse)
                                val degrees = Math.toDegrees(radians.toDouble())
                                if (degrees <= 60f) {
                                    if (offsetY < 0) {
                                        // up
                                        rotationZ = -45f
                                    } else {
                                        // down
                                        rotationZ = 45f
                                    }
                                } else {
                                    rotationZ = 0f
                                }

                                translationX = 6.dp.toPx()
                                translationY = (-48).dp.toPx() + firstPoint.y - 12.dp.toPx()

                                scaleX = secondPoint.x.coerceAtMost(maxWidth.toPx()) / maxWidth.toPx()
                                scaleY = scaleX
                            },
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "arrow",
                        colorFilter = ColorFilter.tint(color = Color.White.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}