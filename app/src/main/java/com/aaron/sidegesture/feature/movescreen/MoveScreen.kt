package com.aaron.sidegesture.feature.movescreen

import com.aaron.sidegesture.feature.gesture.LongSlideState
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aaron.compose.utils.SystemFontScaleHandler
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.DimAlpha
import com.aaron.sidegesture.constant.GlobalSettings.MaxMoveScreenRate
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.MoveScreenData
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.DoubleTap
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.LongPress
import com.aaron.sidegesture.entity.global.ActionSettings.MoveScreen.Action.Tap
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.ktx.tryVibrateForMoveScreen
import com.aaron.sidegesture.utils.JsonHelper
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/5/21
 */

private const val AccelerationStartScreenRatioPerSecond = 0.35f
private const val AccelerationMaxScreenRatioPerSecond = 1.25f
private const val AccelerationSpeedSmoothing = 0.5f
private const val NanosPerSecond = 1_000_000_000f

@Composable
fun MoveScreen(
    screenshot: Bitmap,
    state: MoveScreenState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color = backgroundColor)
                .drawBehind {
                    val left = if (state.showMoveScreenActionPopup) 0f else -state.offset.x
                    val top = if (state.showMoveScreenActionPopup) 0f else -state.offset.y
                    translate(
                        left = left,
                        top = top
                    ) {
                        drawImage(screenshot.asImageBitmap())
                    }

                    if (state.showMoveScreenActionPopup) {
                        drawRect(color = Color.Black.copy(alpha = DimAlpha))
                    }
                }
                .displayCutoutPadding()
                .drawBehind {
                    val offset = state.displayFingerOnScreen
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
                                x = offset.x.roundToInt() - magnifierSize.roundToPx() / 2,
                                y = offset.y.roundToInt() - magnifierSize.roundToPx() / 2
                            )
                            drawImage(
                                image = screenshot.asImageBitmap(),
                                srcOffset = srcOffset
                            )
                        }
                    }

                    //region 瞄准（描边光晕：先深色粗描边垫底，再纯白细芯，亮/暗/彩色背景都可见）
                    val magnifierCenter = Offset(
                        x = center.x,
                        y = magnifierSize.toPx() / 2f
                    )
                    val lineLength = 16.dp.toPx()
                    val ringRadius = magnifierSize.toPx() / 2f
                    val coreStroke = 2.dp.toPx()
                    val haloStroke = coreStroke + 3.5.dp.toPx()
                    val coreColor = Color.White
                    val haloColor = Color.Black.copy(alpha = 0.6f)

                    fun DrawScope.drawAim(color: Color, stroke: Float) {
                        drawLine(
                            color = color,
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
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
                            color = color,
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                            start = Offset(
                                x = magnifierCenter.x,
                                y = magnifierCenter.y - lineLength / 2
                            ),
                            end = Offset(
                                x = magnifierCenter.x,
                                y = magnifierCenter.y + lineLength / 2
                            )
                        )
                        drawCircle(
                            color = color,
                            radius = ringRadius,
                            center = magnifierCenter,
                            style = Stroke(stroke)
                        )
                    }
                    drawAim(haloColor, haloStroke)
                    drawAim(coreColor, coreStroke)
                    //endregion
                }
        )

        MoveScreenActionPopup(state)
    }
}

/**
 * 准星样式：不截屏，直接在目标落点绘制狙击镜准星。Android 7+ 可用，作为低版本兜底/轻量样式。
 * 复用 [MoveScreenState] 的手势、增益、悬停弹窗与点击分发，仅渲染层与放大镜不同。
 */
@Composable
fun CrosshairScreen(
    state: MoveScreenState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    if (state.showMoveScreenActionPopup) {
                        drawRect(color = Color.Black.copy(alpha = DimAlpha))
                    }
                    drawSniperReticle(state.displayFingerOnScreen)
                }
        )

        MoveScreenActionPopup(state)
    }
}

/**
 * 狙击镜准星：外圈圆环 + 中心点 + 四向断口十字。准星不做边界限制，跟随目标自由移动
 * (越界时自然移出屏幕，实际点击的越界拦截由 MoveScreenActionHandler 校验)。
 */
private fun DrawScope.drawSniperReticle(center: Offset) {
    val radius = 22.dp.toPx()
    // 断口十字内端与中心点的间距，留足空当避免贴住中心点
    val gap = 11.dp.toPx()
    val coreStroke = 2.dp.toPx()
    val haloStroke = coreStroke + 3.5.dp.toPx()
    val coreDot = 2.5.dp.toPx()
    val haloDot = coreDot + 1.6.dp.toPx()
    val coreColor = Color.White
    val haloColor = Color.Black.copy(alpha = 0.6f)

    // 外圈圆环 + 四向断口十字，画两遍：先深色粗描边垫底，再纯白细芯叠上
    fun DrawScope.drawShape(color: Color, stroke: Float) {
        drawCircle(color = color, radius = radius, center = center, style = Stroke(stroke))
        drawLine(color, Offset(center.x - radius, center.y), Offset(center.x - gap, center.y), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(center.x + gap, center.y), Offset(center.x + radius, center.y), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y - gap), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(center.x, center.y + gap), Offset(center.x, center.y + radius), stroke, cap = StrokeCap.Round)
    }

    drawShape(haloColor, haloStroke)
    drawCircle(color = haloColor, radius = haloDot, center = center)
    drawShape(coreColor, coreStroke)
    drawCircle(color = coreColor, radius = coreDot, center = center)
}

@Composable
private fun MoveScreenActionPopup(state: MoveScreenState) {
    val colorScheme = MaterialTheme.colorScheme
    val showLocation = state.popupAnchor
    val animationSpec = spring<Float>(stiffness = Spring.StiffnessHigh)
    val parentWidth = 70.dp
    val parentHeight = 150.dp
    AnimatedVisibility(
        modifier = Modifier
            .graphicsLayer {
                val offsetX = parentWidth.toPx() / 2f
                val offsetY = parentHeight.toPx() / 2f
                translationX = showLocation.x - offsetX
                translationY = showLocation.y - offsetY
            },
        visible = state.showMoveScreenActionPopup,
        enter = fadeIn(animationSpec) + scaleIn(animationSpec, 0.9f),
        exit = fadeOut(animationSpec) + scaleOut(animationSpec, 0.9f)
    ) {
        Column(
            modifier = Modifier
                .width(parentWidth)
                .height(parentHeight)
                .onGloballyPositioned {
                    state.updateActionPopupBounds(it.boundsInRoot())
                }
                .shadow(
                    elevation = 4.dp,
                    shape = MaterialTheme.shapes.small
                )
                .background(
                    color = colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActionSettings.MoveScreen.Action.entries.forEach { action ->
                key(action) {
                    SystemFontScaleHandler(false) {
                        Text(
                            modifier = Modifier
                                .onGloballyPositioned {
                                    state.updateActionBounds(action, it.boundsInRoot())
                                }
                                .fillMaxWidth()
                                .weight(1f)
                                .wrapContentSize(),
                            text = when (action) {
                                Tap -> stringResource(R.string.tap)
                                DoubleTap -> stringResource(R.string.double_tap)
                                LongPress -> stringResource(R.string.long_press)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberMoveScreenState(
    gestureSettings: GestureSettings,
    actionSettings: ActionSettings.MoveScreen
): MoveScreenState {
    val coroutineScope = rememberCoroutineScope()
    return remember(gestureSettings, actionSettings, coroutineScope) {
        MoveScreenState(
            actionSettings = actionSettings,
            coroutineScope = coroutineScope,
            onActionSelected = {
                gestureSettings.vibrations.tryVibrateForMoveScreen()
            }
        )
    }
}

enum class MoveScreenPhase {
    Following, HoverPending, Selecting
}

@Stable
class MoveScreenState(
    private val actionSettings: ActionSettings.MoveScreen,
    private val coroutineScope: CoroutineScope,
    private val screenSizeProvider: () -> IntSize = {
        IntSize(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
    },
    private val hoverDelay: suspend (Long) -> Unit = { delay(it) },
    private val nanoTimeProvider: () -> Long = System::nanoTime,
    private val onActionSelected: () -> Unit = {}
) : LongSlideState() {

    var visible: Boolean by mutableStateOf(false)
        private set
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set
    val fingerOnScreen: Offset by derivedStateOf {
        origin + srcOffset * 2f + (offset - srcOffset)
    }
    val displayFingerOnScreen: Offset
        get() = frozenTarget ?: fingerOnScreen
    var popupAnchor: Offset by mutableStateOf(Offset.Unspecified)
        private set
    var phase: MoveScreenPhase by mutableStateOf(MoveScreenPhase.Following)
        private set
    val showMoveScreenActionPopup: Boolean
        get() = phase == MoveScreenPhase.Selecting
    var pendingAction: ActionSettings.MoveScreen.Action? by mutableStateOf(null)
        private set

    private var srcOffset: Offset by mutableStateOf(Offset.Zero)
    private var longPressJob: Job? = null
    private var hoverBounds: Rect? = null
    private var popupBounds: Rect? = null
    private val actionBounds = mutableMapOf<ActionSettings.MoveScreen.Action, Rect>()
    private var frozenTarget: Offset? by mutableStateOf(null)
    private var hoverGeneration = 0L
    private var popupPointerMoved = false
    private var lastDragTimeNanos: Long? = null
    private var smoothedSpeed = 0f
    private var previousDragAmount = Offset.Zero

    override fun onDragStart(offset: Offset) {
        super.onDragStart(offset)
        visible = true
        restartAccelerationTracking()
    }

    override fun onDrag(dragAmount: Offset) {
        super.onDrag(dragAmount)

        if (phase == MoveScreenPhase.Selecting) {
            updatePopupSelection()
            return
        }

        offset += dragAmount * resolveMoveRate(dragAmount)
        srcOffset += dragAmount
        updateHoverState()
    }

    fun updateActionPopupBounds(bounds: Rect) {
        if (phase != MoveScreenPhase.Selecting) return
        popupBounds = bounds
        if (!bounds.contains(finger)) {
            resumeFollowing()
        } else if (popupPointerMoved) {
            updatePopupSelection()
        }
    }

    fun updateActionBounds(action: ActionSettings.MoveScreen.Action, bounds: Rect) {
        if (phase != MoveScreenPhase.Selecting) return
        actionBounds[action] = bounds
        if (popupPointerMoved) {
            updatePopupSelection()
        }
    }

    fun done(): Action {
        val target = if (phase == MoveScreenPhase.Selecting) {
            frozenTarget ?: fingerOnScreen
        } else {
            fingerOnScreen
        }
        val moveScreenData = MoveScreenData(
            x = target.x.toInt(),
            y = target.y.toInt(),
            action = if (phase == MoveScreenPhase.Selecting) pendingAction ?: Tap else Tap
        )
        val data = JsonHelper.encodeToString(moveScreenData)
        return Action(GlobalActions.MOVE_SCREEN, data)
    }

    override fun reset() {
        super.reset()
        visible = false
        offset = Offset.Zero
        srcOffset = Offset.Zero
        invalidateHover()
        popupBounds = null
        actionBounds.clear()
        frozenTarget = null
        popupAnchor = Offset.Unspecified
        pendingAction = null
        popupPointerMoved = false
        phase = MoveScreenPhase.Following
        clearAccelerationTracking()
    }

    private fun updatePopupSelection() {
        popupPointerMoved = true
        val popupBounds = popupBounds ?: return
        if (!popupBounds.contains(finger)) {
            resumeFollowing()
            return
        }

        val action = ActionSettings.MoveScreen.Action.entries.firstOrNull {
            actionBounds[it]?.contains(finger) == true
        }
        if (pendingAction == action) return
        pendingAction = action
        if (action != null) {
            onActionSelected()
        }
    }

    private fun resumeFollowing() {
        invalidateHover()
        popupBounds = null
        actionBounds.clear()
        frozenTarget = null
        pendingAction = null
        popupPointerMoved = false
        phase = MoveScreenPhase.Following
        restartAccelerationTracking()
        updateHoverState()
    }

    private fun resolveMoveRate(dragAmount: Offset): Float {
        val baseRate = actionSettings.rate
        if (!actionSettings.fastMoveAccelerationEnabled) return baseRate

        val now = nanoTimeProvider()
        val previousTime = lastDragTimeNanos
        lastDragTimeNanos = now
        if (dragAmount == Offset.Zero || previousTime == null || now <= previousTime) {
            smoothedSpeed = 0f
            return baseRate
        }

        val reversed = previousDragAmount != Offset.Zero &&
            previousDragAmount.x * dragAmount.x + previousDragAmount.y * dragAmount.y <= 0f
        previousDragAmount = dragAmount
        if (reversed) {
            smoothedSpeed = 0f
            return baseRate
        }

        val elapsedSeconds = (now - previousTime) / NanosPerSecond
        val distance = sqrt(
            dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y
        )
        val speed = distance / elapsedSeconds
        smoothedSpeed = if (speed <= smoothedSpeed) {
            speed
        } else {
            smoothedSpeed + (speed - smoothedSpeed) * AccelerationSpeedSmoothing
        }

        val screenSize = screenSizeProvider()
        val shortEdge = minOf(screenSize.width, screenSize.height).toFloat()
        if (shortEdge <= 0f) return baseRate

        val accelerationStart = shortEdge * AccelerationStartScreenRatioPerSecond
        val accelerationMax = shortEdge * AccelerationMaxScreenRatioPerSecond
        val linearProgress = (
            (smoothedSpeed - accelerationStart) / (accelerationMax - accelerationStart)
        ).coerceIn(0f, 1f)
        val easedProgress = linearProgress * linearProgress * (3f - 2f * linearProgress)
        val maxRate = maxOf(baseRate, MaxMoveScreenRate)
        return baseRate + (maxRate - baseRate) * easedProgress
    }

    private fun restartAccelerationTracking() {
        clearAccelerationTracking()
        lastDragTimeNanos = nanoTimeProvider()
    }

    private fun clearAccelerationTracking() {
        lastDragTimeNanos = null
        smoothedSpeed = 0f
        previousDragAmount = Offset.Zero
    }

    private fun updateHoverState() {
        if (!actionSettings.popupEnabled) {
            cancelPendingHover()
            return
        }

        val target = fingerOnScreen
        val screenSize = screenSizeProvider()
        if (target.x.toInt() !in 0..screenSize.width ||
            target.y.toInt() !in 0..screenSize.height
        ) {
            cancelPendingHover()
            return
        }

        val currentHoverBounds = hoverBounds
        if (currentHoverBounds != null && currentHoverBounds.contains(finger)) return

        this.hoverBounds = Rect(center = finger, radius = actionSettings.radius.toFloat())
        longPressJob?.cancel()
        hoverGeneration++
        val generation = hoverGeneration
        phase = MoveScreenPhase.HoverPending
        longPressJob = coroutineScope.launch {
            hoverDelay(actionSettings.hoverDelayMs)
            if (generation != hoverGeneration || phase != MoveScreenPhase.HoverPending) {
                return@launch
            }
            frozenTarget = fingerOnScreen
            popupAnchor = finger
            pendingAction = null
            popupBounds = null
            actionBounds.clear()
            popupPointerMoved = false
            hoverBounds = null
            phase = MoveScreenPhase.Selecting
            longPressJob = null
        }
    }

    private fun cancelPendingHover() {
        if (phase != MoveScreenPhase.HoverPending && longPressJob == null) return
        invalidateHover()
        phase = MoveScreenPhase.Following
    }

    private fun invalidateHover() {
        hoverGeneration++
        longPressJob?.cancel()
        longPressJob = null
        hoverBounds = null
    }
}
