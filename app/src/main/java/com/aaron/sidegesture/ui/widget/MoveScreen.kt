package com.aaron.sidegesture.ui.widget

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import kotlin.math.roundToInt

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/5/21
 */

@Composable
fun MoveScreen(
    screenshot: Bitmap,
    state: MoveScreenState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black
) {
    Box(
        modifier = modifier
            .background(color = backgroundColor)
            .drawBehind {
                translate(
                    left = -state.offset.x,
                    top = -state.offset.y
                ) {
                    drawImage(screenshot.asImageBitmap())
                }
            }
            .displayCutoutPadding()
            .drawBehind {
                val magnifierSize = 80.dp
                val path = Path().also {
                    it.addOval(
                        Rect(
                            offset = Offset.Zero,
                            size = Size(magnifierSize.toPx(), magnifierSize.toPx())
                        )
                    )
                }
                translate(
                    left = size.width / 2f - magnifierSize.toPx() / 2f
                ) {
                    clipPath(path) {
                        val srcOffset = IntOffset(
                            x = state.fingerOnScreen.x.roundToInt() - magnifierSize.roundToPx() / 2,
                            y = state.fingerOnScreen.y.roundToInt() - magnifierSize.roundToPx() / 2
                        )
                        drawImage(
                            image = screenshot.asImageBitmap(),
                            srcOffset = srcOffset
                        )
                    }
                }

                //region 瞄准
                val magnifierCenter = Offset(
                    x = center.x,
                    y = magnifierSize.toPx() / 2f
                )
                val lineLength = 16.dp.toPx()
                val lineColor = Color.LightGray
                val strokeWidth = 2.dp.toPx()
                drawLine(
                    color = lineColor,
                    strokeWidth = strokeWidth,
                    start = Offset(
                        x = magnifierCenter.x - lineLength / 2,
                        y = magnifierCenter.y
                    ),
                    end = Offset(
                        x = magnifierCenter.x + lineLength / 2,
                        y = magnifierCenter.y
                    )
                )
                drawLine(
                    color = lineColor,
                    strokeWidth = strokeWidth,
                    start = Offset(
                        x = magnifierCenter.x,
                        y = magnifierCenter.y - lineLength / 2
                    ),
                    end = Offset(
                        x = magnifierCenter.x,
                        y = magnifierCenter.y + lineLength / 2
                    )
                )
                //endregion
            }
    )
}

@Composable
fun rememberMoveScreenState(rate: Float = 1f): MoveScreenState {
    return remember(rate) {
        MoveScreenState(rate)
    }
}

@Stable
class MoveScreenState(private val rate: Float = 1f) : LongSlideState() {

    var visible: Boolean by mutableStateOf(false)
        private set
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set
    // 等待模拟点击的坐标
    val fingerOnScreen: Offset by derivedStateOf {
        origin + srcOffset * 2f + (offset - srcOffset)
    }
    private var srcOffset: Offset by mutableStateOf(Offset.Zero)

    override fun onDragStart(offset: Offset) {
        super.onDragStart(offset)
        visible = true
    }

    override fun onDrag(dragAmount: Offset) {
        super.onDrag(dragAmount)
        offset += dragAmount * rate
        srcOffset += dragAmount
    }

    fun done(): Action {
        val finger = fingerOnScreen
        val data = "${finger.x.toInt()},${finger.y.toInt()}"
        return Action(GlobalActions.MOVE_SCREEN, data)
    }

    override fun reset() {
        super.reset()
        visible = false
        offset = Offset.Zero
        srcOffset = Offset.Zero
    }
}