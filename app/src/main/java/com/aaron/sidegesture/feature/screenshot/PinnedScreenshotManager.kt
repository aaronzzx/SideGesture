package com.aaron.sidegesture.feature.screenshot

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.PathInterpolator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aaron.compose.ktx.clipToBackground
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.ktx.rootSize
import com.aaron.sidegesture.ui.theme.PinnedScreenshotDimensions
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.appColors
import com.aaron.sidegesture.ui.theme.componentShapes
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.motion
import com.blankj.utilcode.util.ConvertUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PinnedScreenshotManager(
    private val service: SideGestureService
) {

    private val windowManager = ContextCompat.getSystemService(service, WindowManager::class.java)!!
    private val windows = linkedMapOf<String, PinWindow>()
    private val deleteTargetState = PinDeleteTargetState()
    private var deleteTargetWindow: DeleteTargetWindow? = null
    private var pinnedDimensions = PinnedScreenshotDimensions()
    private var isScreenLocked = false
    private var safeInsets = PinSafeInsets.from(service, windowManager, emptyList())

    fun pin(
        bitmap: Bitmap,
        sourceRect: Rect? = null
    ) {
        ensureDeleteTargetWindow()
        val root = rootSize
        val minScale = minScale(bitmap)
        val chromeSize = pinChromeSizePx(pinnedDimensions)
        val targetScale = min(
            1f,
            min(
                (root.width * 0.45f - chromeSize * 2f) / bitmap.width,
                (root.height * 0.45f - chromeSize * 2f) / bitmap.height
            )
        ).coerceIn(minScale, maxScale(bitmap))
        val startScale = sourceRect?.let { rect ->
            min(rect.width / bitmap.width, rect.height / bitmap.height)
                .coerceAtLeast(0.01f)
        } ?: targetScale
        val startWindowWidth = bitmap.width * startScale + chromeSize * 2f
        val startWindowHeight = bitmap.height * startScale + chromeSize * 2f
        val state = PinWindowState(
            id = SystemClock.uptimeMillis().toString(),
            bitmap = bitmap,
            scale = startScale,
            x = sourceRect?.let { it.left - chromeSize }
                ?: ((root.width - startWindowWidth) / 2f).coerceAtLeast(0f),
            y = sourceRect?.let { it.top - chromeSize }
                ?: ((root.height - startWindowHeight) / 2f).coerceAtLeast(0f),
            safeInsets = safeInsets,
            dimensions = pinnedDimensions
        )
        if (sourceRect == null) {
            clampVisible(state)
        }
        val start = state.snapshot()
        state.scale = targetScale
        if (sourceRect != null) {
            state.x = sourceRect.center.x - state.displayWidth() / 2f
            state.y = sourceRect.center.y - state.displayHeight() / 2f
            clampInitialTargetVisible(state)
        } else {
            clampVisible(state)
        }
        state.collapsedEdge = null
        state.anchoredEdge = nearestEdge(state)
        state.normalScale = state.scale
        state.normalX = state.x
        state.normalY = state.y
        val target = state.snapshot()
        state.applySnapshot(start)
        val view = ComposeView(service).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeViewModelStoreOwner(service)
            setViewTreeSavedStateRegistryOwner(service)
            setContent {
                SideGestureTheme {
                    val dimensions = MaterialTheme.dimensions.pinnedScreenshot
                    SideEffect {
                        updatePinnedDimensions(dimensions)
                    }
                    PinnedScreenshotWindow(
                        state = state,
                        dimensions = dimensions,
                        onGestureStart = {
                            cancelLayoutAnimation(state)
                        },
                        onDrag = { pan, localPosition, time ->
                            handleDrag(state, pan, localPosition, time)
                        },
                        onResizeStart = { localPosition, handleSide ->
                            handleResizeStart(state, localPosition, handleSide)
                        },
                        onResize = { localPosition ->
                            handleResize(state, localPosition)
                        },
                        onGestureEnd = { mode, moved ->
                            handleGestureEnd(state, mode, moved)
                        },
                        onCollapsedTap = {
                            restore(state)
                        }
                    )
                }
            }
        }
        val layoutParams = createLayoutParams(state)
        if (isScreenLocked) {
            view.visibility = View.INVISIBLE
            layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        windowManager.addView(view, layoutParams)
        windows[state.id] = PinWindow(state, view, layoutParams)
        if (sourceRect != null && start != target) {
            animateTo(state, target)
        } else {
            state.applySnapshot(target)
            updateLayout(state.id)
        }
    }

    fun onEnvironmentChanged(buttons: List<GestureButton>) {
        safeInsets = PinSafeInsets.from(service, windowManager, buttons)
        updateDeleteTargetLayout()
        windows.values.forEach { window ->
            val state = window.state
            cancelLayoutAnimation(state)
            state.safeInsets = safeInsets
            val maxScale = maxScale(state.bitmap)
            val minScale = minScale(state.bitmap)
            if (state.scale !in minScale..maxScale) {
                state.scale = state.scale.coerceIn(minScale, maxScale)
            }
            if (state.normalScale !in minScale..maxScale) {
                state.normalScale = state.normalScale.coerceIn(minScale, maxScale)
            }
            if (state.collapsedEdge != null) {
                snapToEdge(state, collapsed = true)
            } else {
                clampVisible(state)
            }
            updateLayout(window)
        }
    }

    fun setScreenLocked(locked: Boolean) {
        if (isScreenLocked == locked) {
            return
        }
        isScreenLocked = locked
        if (locked) {
            hideDeleteTarget()
        }
        windows.values.forEach { window ->
            applyLockedState(window)
        }
    }

    fun release() {
        deleteTargetWindow?.let { window ->
            try {
                windowManager.removeViewImmediate(window.view)
            } catch (_: Exception) {
            }
        }
        deleteTargetWindow = null
        windows.values.toList().forEach { window ->
            cancelLayoutAnimation(window.state)
            try {
                windowManager.removeViewImmediate(window.view)
            } catch (_: Exception) {
            }
            window.state.bitmap.recycle()
        }
        windows.clear()
    }

    private fun remove(id: String) {
        val window = windows.remove(id) ?: return
        cancelLayoutAnimation(window.state)
        hideDeleteTarget(window.state)
        try {
            windowManager.removeViewImmediate(window.view)
        } catch (_: Exception) {
        }
        window.state.bitmap.recycle()
    }

    private fun ensureDeleteTargetWindow() {
        if (deleteTargetWindow != null) {
            return
        }
        val view = ComposeView(service).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeViewModelStoreOwner(service)
            setViewTreeSavedStateRegistryOwner(service)
            setContent {
                SideGestureTheme {
                    val dimensions = MaterialTheme.dimensions.pinnedScreenshot
                    SideEffect {
                        updatePinnedDimensions(dimensions)
                    }
                    PinDeleteTarget(
                        state = deleteTargetState,
                        dimensions = dimensions
                    )
                }
            }
        }
        val layoutParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = deleteTargetHeightPx(pinnedDimensions)
            x = 0
            y = rootSize.height - height
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = baseWindowFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            gravity = Gravity.LEFT or Gravity.TOP
        }
        try {
            windowManager.addView(view, layoutParams)
            deleteTargetWindow = DeleteTargetWindow(view, layoutParams)
        } catch (_: Exception) {
        }
    }

    private fun showDeleteTarget() {
        ensureDeleteTargetWindow()
        if (!isScreenLocked) {
            deleteTargetState.visible = true
        }
    }

    private fun hideDeleteTarget(state: PinWindowState? = null) {
        state?.overDeleteTarget = false
        deleteTargetState.active = false
        deleteTargetState.visible = false
    }

    private fun isOverDeleteTarget(pointer: Offset): Boolean {
        val root = rootSize
        val width = deleteTargetWidthPx(pinnedDimensions).toFloat()
        val height = deleteTargetCardHeightPx(pinnedDimensions).toFloat()
        val left = (root.width - width) / 2f
        val deleteTargetHeight = deleteTargetHeightPx(pinnedDimensions)
        val top = root.height - deleteTargetHeight + (deleteTargetHeight - height) / 2f
        return pointer.x in left..(left + width) &&
                pointer.y in top..(top + height)
    }

    private fun handleDrag(
        state: PinWindowState,
        pan: Offset,
        localPosition: Offset,
        uptimeMillis: Long
    ) {
        showDeleteTarget()
        state.x += pan.x
        state.y += pan.y
        clampVisible(state)
        val finger = Offset(state.x + localPosition.x, state.y + localPosition.y)
        val overDeleteTarget = isOverDeleteTarget(finger)
        state.overDeleteTarget = overDeleteTarget
        deleteTargetState.active = overDeleteTarget
        if (pan != Offset.Zero) {
            state.dragSamples += PinDragSample(uptimeMillis, state.center())
            trimSamples(state.dragSamples, uptimeMillis)
        }
        updateLayout(state.id)
    }

    private fun handleResizeStart(
        state: PinWindowState,
        localPosition: Offset,
        handleSide: PinResizeHandleSide
    ) {
        if (state.collapsedEdge != null) {
            return
        }
        val chromeSize = pinChromeSizePx(state.dimensions).toFloat()
        val width = state.contentWidth().toFloat()
        val anchor = when (handleSide) {
            PinResizeHandleSide.Left -> Offset(state.x + chromeSize + width, state.y + chromeSize)
            PinResizeHandleSide.Right -> Offset(state.x + chromeSize, state.y + chromeSize)
        }
        val pointer = Offset(state.x + localPosition.x, state.y + localPosition.y)
        val distance = pointer.distanceTo(anchor).coerceAtLeast(1f)
        state.resizeStart = PinResizeStart(
            handleSide = handleSide,
            anchor = anchor,
            scale = state.scale,
            x = state.x,
            y = state.y,
            distance = distance
        )
        state.normalScale = state.scale
        state.normalX = state.x
        state.normalY = state.y
    }

    private fun handleResize(
        state: PinWindowState,
        localPosition: Offset
    ) {
        val start = state.resizeStart ?: return
        val pointer = Offset(state.x + localPosition.x, state.y + localPosition.y)
        val scale = (start.scale * pointer.distanceTo(start.anchor) / start.distance)
            .coerceIn(minScale(state.bitmap), maxScale(state.bitmap))
        state.scale = scale
        state.y = start.y
        val chromeSize = pinChromeSizePx(state.dimensions)
        state.x = when (start.handleSide) {
            PinResizeHandleSide.Left -> start.anchor.x - chromeSize - state.contentWidth()
            PinResizeHandleSide.Right -> start.x
        }
        clampVisible(state)
        updateLayout(state.id)
    }

    private fun handleGestureEnd(
        state: PinWindowState,
        mode: PinGestureMode,
        moved: Boolean
    ) {
        val isTap = mode == PinGestureMode.Drag && !moved
        if (isTap && state.collapsedEdge != null) {
            hideDeleteTarget(state)
            restore(state)
            return
        }
        if (isTap) {
            hideDeleteTarget(state)
            state.normalScale = state.scale
            state.normalX = state.x
            state.normalY = state.y
            collapseToEdge(
                state = state,
                targetScale = max(minScale(state.bitmap), state.scale * COLLAPSE_SCALE_FACTOR),
                animate = true
            )
            state.dragSamples.clear()
            return
        }

        if (mode == PinGestureMode.Resize) {
            hideDeleteTarget(state)
            val resizedToMin = isAtMinScale(state)
            state.resizeStart = null
            if (!moved) {
                settleExpanded(state, animate = false)
            } else if (resizedToMin) {
                collapseToEdge(state, targetScale = minScale(state.bitmap), animate = true)
            } else {
                settleExpanded(state, animate = moved)
            }
        } else if (state.overDeleteTarget) {
            hideDeleteTarget(state)
            state.dragSamples.clear()
            remove(state.id)
            return
        } else if (state.collapsedEdge != null) {
            snapToEdge(state, collapsed = true, animate = moved)
            if (!moved) {
                updateLayout(state.id)
            }
        } else if (shouldCollapse(state)) {
            state.normalScale = state.scale
            state.normalX = state.x
            state.normalY = state.y
            collapseToEdge(
                state = state,
                targetScale = max(minScale(state.bitmap), state.scale * COLLAPSE_SCALE_FACTOR),
                animate = true
            )
        } else {
            settleExpanded(state, animate = moved)
        }
        hideDeleteTarget(state)
        state.dragSamples.clear()
    }

    private fun restore(state: PinWindowState) {
        if (state.collapsedEdge == null) {
            return
        }
        val start = state.snapshot()
        state.collapsedEdge = null
        state.scale = state.normalScale.coerceIn(minScale(state.bitmap), maxScale(state.bitmap))
        state.x = state.normalX
        state.y = state.normalY
        clampVisible(state)
        state.anchoredEdge = nearestEdge(state)
        state.normalScale = state.scale
        state.normalX = state.x
        state.normalY = state.y
        val target = state.snapshot()
        state.applySnapshot(start)
        animateTo(state, target)
    }

    private fun collapseToEdge(
        state: PinWindowState,
        targetScale: Float,
        animate: Boolean
    ) {
        val start = state.snapshot()
        val targetEdge = nearestEdge(state)
        state.scale = targetScale.coerceIn(minScale(state.bitmap), maxScale(state.bitmap))
        snapToEdge(state, collapsed = true, edge = targetEdge)
        val target = state.snapshot()
        state.applySnapshot(start)
        if (animate) {
            animateTo(state, target)
        } else {
            state.applySnapshot(target)
            updateLayout(state.id)
        }
    }

    private fun settleExpanded(
        state: PinWindowState,
        animate: Boolean
    ) {
        val start = state.snapshot()
        state.collapsedEdge = null
        state.anchoredEdge = nearestEdge(state)
        clampVisible(state)
        state.normalScale = state.scale
        state.normalX = state.x
        state.normalY = state.y
        val target = state.snapshot()
        if (animate && target != start) {
            state.applySnapshot(start)
            animateTo(state, target)
        } else {
            updateLayout(state.id)
        }
    }

    private fun shouldCollapse(state: PinWindowState): Boolean {
        if (state.dragSamples.size < 2) {
            return false
        }
        val end = state.dragSamples.last()
        val start = state.dragSamples.first()
        val dt = (end.time - start.time).coerceAtLeast(1L)
        val velocity = Offset(
            x = (end.center.x - start.center.x) / dt * 1000f,
            y = (end.center.y - start.center.y) / dt * 1000f
        )
        val speed = sqrt(velocity.x * velocity.x + velocity.y * velocity.y)
        if (speed < FLING_VELOCITY_THRESHOLD) {
            return false
        }
        val edgeDirection = when (nearestEdge(state)) {
            PinEdge.Left -> Offset(-1f, 0f)
            PinEdge.Right -> Offset(1f, 0f)
        }
        return velocity.x * edgeDirection.x + velocity.y * edgeDirection.y > 0f
    }

    private fun nearestEdge(state: PinWindowState): PinEdge {
        val center = state.center()
        val root = rootSize
        val leftDistance = center.x - state.safeInsets.left
        val rightDistance = root.width - state.safeInsets.right - center.x
        return if (leftDistance <= rightDistance) PinEdge.Left else PinEdge.Right
    }

    private fun snapToEdge(
        state: PinWindowState,
        collapsed: Boolean,
        animate: Boolean = false,
        edge: PinEdge? = null
    ) {
        val start = state.snapshot()
        val targetEdge = edge ?: nearestEdge(state)
        val xRange = allowedXRange(state)
        val yRange = allowedYRange(state)
        state.x = when (targetEdge) {
            PinEdge.Left -> xRange.start
            PinEdge.Right -> xRange.endInclusive
        }
        state.y = state.y.coerceIn(yRange.start, yRange.endInclusive)
        state.collapsedEdge = if (collapsed) targetEdge else null
        state.anchoredEdge = targetEdge
        clampVisible(state)
        if (animate) {
            val target = state.snapshot()
            state.applySnapshot(start)
            animateTo(state, target)
        }
    }

    private fun clampVisible(state: PinWindowState) {
        val xRange = allowedXRange(state)
        val yRange = allowedYRange(state)
        state.x = state.x.coerceIn(xRange.start, xRange.endInclusive)
        state.y = state.y.coerceIn(yRange.start, yRange.endInclusive)
    }

    private fun clampInitialTargetVisible(state: PinWindowState) {
        val xRange = twoHandleVisibleXRange(state)
        val yRange = twoHandleVisibleYRange(state)
        state.x = state.x.coerceIn(xRange.start, xRange.endInclusive)
        state.y = state.y.coerceIn(yRange.start, yRange.endInclusive)
    }

    private fun maxScale(bitmap: Bitmap): Float {
        val root = rootSize
        val chromeSize = pinChromeSizePx(pinnedDimensions) * 2f
        return min(
            MAX_SCALE,
            min(
                (root.width * 0.85f - chromeSize) / bitmap.width,
                (root.height * 0.85f - chromeSize) / bitmap.height
            )
        ).coerceAtLeast(minScale(bitmap))
    }

    private fun minScale(bitmap: Bitmap): Float {
        val shortEdge = min(bitmap.width, bitmap.height).coerceAtLeast(1)
        return max(
            MIN_SCALE,
            ConvertUtils.dp2px(MIN_VISIBLE_SHORT_EDGE_DP).toFloat() / shortEdge
        )
    }

    private fun trimSamples(samples: MutableList<PinDragSample>, now: Long) {
        while (samples.size > 1 && now - samples.first().time > FLING_WINDOW_MS) {
            samples.removeAt(0)
        }
    }

    private fun isAtMinScale(state: PinWindowState): Boolean {
        return state.scale <= minScale(state.bitmap) + MIN_SCALE_EPSILON
    }

    private fun cancelLayoutAnimation(state: PinWindowState) {
        val animator = state.layoutAnimator
        state.layoutAnimator = null
        animator?.cancel()
    }

    private fun animateTo(
        state: PinWindowState,
        target: PinLayoutSnapshot
    ) {
        cancelLayoutAnimation(state)
        val start = state.snapshot()
        state.collapsedEdge = target.collapsedEdge
        state.anchoredEdge = target.anchoredEdge
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = LAYOUT_ANIMATION_DURATION_MS.toLong()
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                state.scale = lerp(start.scale, target.scale, fraction)
                state.x = lerp(start.x, target.x, fraction)
                state.y = lerp(start.y, target.y, fraction)
                updateLayout(state.id)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (state.layoutAnimator == animation) {
                        state.applySnapshot(target)
                        updateLayout(state.id)
                        state.layoutAnimator = null
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (state.layoutAnimator == animation) {
                        state.layoutAnimator = null
                    }
                }
            })
        }
        state.layoutAnimator = animator
        animator.start()
    }

    private fun allowedXRange(state: PinWindowState): ClosedFloatingPointRange<Float> {
        return pinAllowedXRange(state)
    }

    private fun allowedYRange(state: PinWindowState): ClosedFloatingPointRange<Float> {
        val root = rootSize
        val handleOutset = pinHandleOutsetPx(state.dimensions).toFloat()
        val minY = state.safeInsets.top - handleOutset
        val maxY = root.height - state.safeInsets.bottom - state.displayHeight() + handleOutset
        return if (maxY >= minY) {
            minY..maxY
        } else {
            minY..minY
        }
    }

    private fun updateLayout(id: String) {
        val window = windows[id] ?: return
        updateLayout(window)
    }

    private fun updateLayout(window: PinWindow) {
        val state = window.state
        window.layoutParams.width = state.displayWidth()
        window.layoutParams.height = state.displayHeight()
        window.layoutParams.x = state.x.roundToInt()
        window.layoutParams.y = state.y.roundToInt()
        try {
            windowManager.updateViewLayout(window.view, window.layoutParams)
        } catch (_: Exception) {
        }
    }

    private fun applyLockedState(window: PinWindow) {
        window.view.visibility = if (isScreenLocked) View.INVISIBLE else View.VISIBLE
        window.layoutParams.flags = baseWindowFlags() or
                if (isScreenLocked) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0
        try {
            windowManager.updateViewLayout(window.view, window.layoutParams)
        } catch (_: Exception) {
        }
    }

    private fun updateDeleteTargetLayout() {
        val window = deleteTargetWindow ?: return
        window.layoutParams.height = deleteTargetHeightPx(pinnedDimensions)
        window.layoutParams.y = rootSize.height - window.layoutParams.height
        try {
            windowManager.updateViewLayout(window.view, window.layoutParams)
        } catch (_: Exception) {
        }
    }

    private fun updatePinnedDimensions(dimensions: PinnedScreenshotDimensions) {
        if (pinnedDimensions == dimensions) {
            return
        }
        pinnedDimensions = dimensions
        updateDeleteTargetLayout()
        windows.values.forEach { window ->
            window.state.dimensions = dimensions
            clampVisible(window.state)
            updateLayout(window)
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun createLayoutParams(state: PinWindowState): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            width = state.displayWidth()
            height = state.displayHeight()
            x = state.x.roundToInt()
            y = state.y.roundToInt()
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = baseWindowFlags()
            gravity = Gravity.LEFT or Gravity.TOP
        }
    }

    private fun baseWindowFlags(): Int {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
    }

    private data class PinWindow(
        val state: PinWindowState,
        val view: ComposeView,
        val layoutParams: WindowManager.LayoutParams
    )

    private companion object {
        const val MIN_SCALE = 0.35f
        const val MIN_VISIBLE_SHORT_EDGE_DP = 96f
        const val MAX_SCALE = 2.5f
        const val COLLAPSE_SCALE_FACTOR = 0.35f
        const val FLING_VELOCITY_THRESHOLD = 1800f
        const val FLING_WINDOW_MS = 120L
        const val LAYOUT_ANIMATION_DURATION_MS = 180
        const val MIN_SCALE_EPSILON = 0.005f
    }
}

private enum class PinEdge {
    Left, Right
}

private enum class PinResizeHandleSide {
    Left, Right
}

private enum class PinGestureMode {
    Drag, Resize
}

private data class PinSafeInsets(
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
) {
    companion object {
        fun from(
            service: SideGestureService,
            windowManager: WindowManager,
            buttons: List<GestureButton>
        ): PinSafeInsets {
            val fallback = ConvertUtils.dp2px(8f)
            val systemInsets = readSystemInsets(service, windowManager)
            return PinSafeInsets(
                left = max(
                    systemInsets.left,
                    buttons.filter { it.enabled && it.position == Position.Left }.maxOfOrNull { it.width } ?: fallback
                ),
                right = max(
                    systemInsets.right,
                    buttons.filter { it.enabled && it.position == Position.Right }.maxOfOrNull { it.width } ?: fallback
                ),
                top = max(
                    systemInsets.top,
                    buttons.filter { it.enabled && it.position == Position.Top }.maxOfOrNull { it.width } ?: fallback
                ),
                bottom = max(
                    systemInsets.bottom,
                    buttons.filter { it.enabled && it.position == Position.Bottom }.maxOfOrNull { it.width } ?: fallback
                )
            )
        }

        private fun readSystemInsets(
            service: SideGestureService,
            windowManager: WindowManager
        ): PinSafeInsets {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insets = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                return PinSafeInsets(
                    left = insets.left,
                    right = insets.right,
                    top = insets.top,
                    bottom = insets.bottom
                )
            }
            return PinSafeInsets(
                left = 0,
                right = 0,
                top = systemBarSize(service, "status_bar_height"),
                bottom = systemBarSize(service, "navigation_bar_height")
            )
        }

        private fun systemBarSize(
            service: SideGestureService,
            resourceName: String
        ): Int {
            val resourceId = service.resources.getIdentifier(resourceName, "dimen", "android")
            return if (resourceId != 0) {
                service.resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }
    }
}

private data class PinDragSample(
    val time: Long,
    val center: Offset
)

private data class PinResizeStart(
    val handleSide: PinResizeHandleSide,
    val anchor: Offset,
    val scale: Float,
    val x: Float,
    val y: Float,
    val distance: Float
)

private data class PinLayoutSnapshot(
    val scale: Float,
    val x: Float,
    val y: Float,
    val collapsedEdge: PinEdge?,
    val anchoredEdge: PinEdge
)

private data class DeleteTargetWindow(
    val view: ComposeView,
    val layoutParams: WindowManager.LayoutParams
)

private class PinDeleteTargetState {
    var visible by mutableStateOf(false)
    var active by mutableStateOf(false)
}

private class PinWindowState(
    val id: String,
    val bitmap: Bitmap,
    scale: Float,
    x: Float,
    y: Float,
    safeInsets: PinSafeInsets,
    dimensions: PinnedScreenshotDimensions
) {
    var scale by mutableStateOf(scale)
    var x by mutableStateOf(x)
    var y by mutableStateOf(y)
    var normalScale by mutableStateOf(scale)
    var normalX by mutableStateOf(x)
    var normalY by mutableStateOf(y)
    var collapsedEdge by mutableStateOf<PinEdge?>(null)
    var anchoredEdge by mutableStateOf(PinEdge.Right)
    var safeInsets by mutableStateOf(safeInsets)
    var dimensions by mutableStateOf(dimensions)
    var resizeStart: PinResizeStart? = null
    var layoutAnimator: ValueAnimator? = null
    var overDeleteTarget by mutableStateOf(false)
    val dragSamples: MutableList<PinDragSample> = mutableListOf()

    fun contentWidth(): Int = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    fun contentHeight(): Int = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    fun displayWidth(): Int = contentWidth() + pinChromeSizePx(dimensions) * 2
    fun displayHeight(): Int = contentHeight() + pinChromeSizePx(dimensions) * 2
    fun center(): Offset = Offset(x + displayWidth() / 2f, y + displayHeight() / 2f)
    fun snapshot(): PinLayoutSnapshot = PinLayoutSnapshot(
        scale = scale,
        x = x,
        y = y,
        collapsedEdge = collapsedEdge,
        anchoredEdge = anchoredEdge
    )

    fun applySnapshot(snapshot: PinLayoutSnapshot) {
        scale = snapshot.scale
        x = snapshot.x
        y = snapshot.y
        collapsedEdge = snapshot.collapsedEdge
        anchoredEdge = snapshot.anchoredEdge
    }
}

@Composable
private fun PinnedScreenshotWindow(
    state: PinWindowState,
    dimensions: PinnedScreenshotDimensions,
    onGestureStart: () -> Unit,
    onDrag: (Offset, Offset, Long) -> Unit,
    onResizeStart: (Offset, PinResizeHandleSide) -> Unit,
    onResize: (Offset) -> Unit,
    onGestureEnd: (PinGestureMode, Boolean) -> Unit,
    onCollapsedTap: () -> Unit
) {
    val bitmap = remember(state.bitmap) { state.bitmap.asImageBitmap() }
    val pinScale by animateFloatAsState(
        targetValue = if (state.overDeleteTarget) {
            MaterialTheme.motion.pinnedScreenshotScale
        } else {
            1f
        },
        animationSpec = tween(MaterialTheme.motion.pinnedScreenshotScaleDurationMillis),
        label = "pinDeleteScale"
    )
    val pinAlpha by animateFloatAsState(
        targetValue = if (state.overDeleteTarget) {
            MaterialTheme.motion.pinnedScreenshotAlpha
        } else {
            1f
        },
        animationSpec = tween(MaterialTheme.motion.pinnedScreenshotScaleDurationMillis),
        label = "pinDeleteAlpha"
    )
    val latestOnGestureStart = rememberUpdatedState(onGestureStart)
    val latestOnDrag = rememberUpdatedState(onDrag)
    val latestOnResizeStart = rememberUpdatedState(onResizeStart)
    val latestOnResize = rememberUpdatedState(onResize)
    val latestOnGestureEnd = rememberUpdatedState(onGestureEnd)
    val latestOnCollapsedTap = rememberUpdatedState(onCollapsedTap)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    latestOnGestureStart.value()
                    val touchTarget = dimensions.resizeTouchTarget.toPx()
                    val resizeHandleSide = pinResizeHandleAt(
                        position = down.position,
                        state = state,
                        width = size.width,
                        height = size.height,
                        touchTarget = touchTarget
                    )
                    val mode = if (
                        state.collapsedEdge == null &&
                        resizeHandleSide != null
                    ) {
                        latestOnResizeStart.value(down.position, resizeHandleSide)
                        PinGestureMode.Resize
                    } else {
                        PinGestureMode.Drag
                    }
                    var moved = false
                    var event = awaitPointerEvent(pass = PointerEventPass.Main)
                    while (event.changes.any { it.pressed && !it.changedToUpIgnoreConsumed() }) {
                        val pressedChanges = event.changes.filter { it.pressed }
                        if (pressedChanges.size == 1) {
                            val change = pressedChanges.first()
                            val pan = change.position - change.previousPosition
                            if (mode == PinGestureMode.Resize) {
                                if (pan != Offset.Zero) {
                                    moved = true
                                    latestOnResize.value(change.position)
                                    change.consume()
                                }
                            } else if (pan != Offset.Zero) {
                                moved = true
                                latestOnDrag.value(pan, change.position, SystemClock.uptimeMillis())
                                change.consume()
                            }
                        }
                        event = awaitPointerEvent(pass = PointerEventPass.Main)
                    }
                    if (!moved && state.collapsedEdge != null) {
                        latestOnCollapsedTap.value()
                    } else {
                        latestOnGestureEnd.value(mode, moved)
                    }
                }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pinScale
                    scaleY = pinScale
                    alpha = pinAlpha
                }
        ) {
            val shape = MaterialTheme.componentShapes.pinnedScreenshotPanel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensions.handleOutset)
                    .clipToBackground(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = MaterialTheme.alpha.pinnedSurface
                        ),
                        shape = shape
                    )
            ) {
                Image(
                    modifier = Modifier
                        .padding(dimensions.imageInset)
                        .fillMaxSize()
                        .clip(shape),
                    bitmap = bitmap,
                    contentDescription = null
                )
            }
            PinResizeHandleSide.entries.forEach { handleSide ->
                AnimatedVisibility(
                    visible = handleSide in visibleResizeHandleSides(state),
                    modifier = Modifier.align(pinResizeHandleAlignment(handleSide)),
                    enter = fadeIn(
                        animationSpec = tween(MaterialTheme.motion.pinnedScreenshotFadeDurationMillis)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(MaterialTheme.motion.pinnedScreenshotFadeDurationMillis)
                    )
                ) {
                    PinResizeHandle(
                        handleSide = handleSide,
                        dimensions = dimensions
                    )
                }
            }
        }
    }
}

@Composable
private fun PinResizeHandle(
    handleSide: PinResizeHandleSide,
    dimensions: PinnedScreenshotDimensions
) {
    val color = MaterialTheme.appColors.resizeHandle
    Canvas(
        modifier = Modifier
            .padding(dimensions.handlePadding)
            .size(dimensions.resizeHandleSize)
    ) {
        val stroke = Stroke(
            width = dimensions.handleStrokeWidth.toPx(),
            cap = StrokeCap.Round
        )
        val width = size.width
        val height = size.height
        val path = Path()
        if (handleSide == PinResizeHandleSide.Right) {
            path.moveTo(width, height * 0.18f)
            path.cubicTo(
                width,
                height * 0.72f,
                width * 0.72f,
                height,
                width * 0.18f,
                height
            )
            drawPath(
                color = color,
                path = path,
                style = stroke
            )
        } else {
            path.moveTo(0f, height * 0.18f)
            path.cubicTo(
                0f,
                height * 0.72f,
                width * 0.28f,
                height,
                width * 0.82f,
                height
            )
            drawPath(
                color = color,
                path = path,
                style = stroke
            )
        }
    }
}

private fun pinResizeHandleAlignment(handleSide: PinResizeHandleSide): Alignment {
    return when (handleSide) {
        PinResizeHandleSide.Left -> Alignment.BottomStart
        PinResizeHandleSide.Right -> Alignment.BottomEnd
    }
}

@Composable
private fun PinDeleteTarget(
    state: PinDeleteTargetState,
    dimensions: PinnedScreenshotDimensions
) {
    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn(
            animationSpec = tween(MaterialTheme.motion.pinnedScreenshotFadeDurationMillis)
        ),
        exit = fadeOut(
            animationSpec = tween(MaterialTheme.motion.pinnedScreenshotFadeDurationMillis)
        )
    ) {
        val containerColor = if (state.active) {
            MaterialTheme.colorScheme.error.copy(alpha = MaterialTheme.alpha.pinnedDeleteActive)
        } else {
            MaterialTheme.appColors.fixedBlack.copy(
                alpha = MaterialTheme.alpha.pinnedDeleteInactive
            )
        }
        val contentColor = MaterialTheme.appColors.fixedWhite.copy(
            alpha = if (state.active) 1f else MaterialTheme.alpha.pinnedContentInactive
        )
        val iconScale by animateFloatAsState(
            targetValue = if (state.active) {
                MaterialTheme.motion.pinnedDeleteActiveScale
            } else {
                1f
            },
            animationSpec = tween(MaterialTheme.motion.pinnedScreenshotScaleDurationMillis),
            label = "deleteTargetScale"
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(dimensions.deleteTargetWidth)
                    .height(dimensions.deleteTargetCardHeight)
                    .clipToBackground(
                        color = containerColor,
                        shape = RoundedCornerShape(dimensions.deleteTargetCornerRadius)
                    )
                    .padding(vertical = dimensions.deleteTargetVerticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(dimensions.deleteTargetIconSize)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    imageVector = Icons.Default.Delete,
                    tint = contentColor,
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.pinned_screenshot_delete_target),
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun pinResizeHandleAt(
    position: Offset,
    state: PinWindowState,
    width: Int,
    height: Int,
    touchTarget: Float
): PinResizeHandleSide? {
    if (position.y < height - touchTarget) {
        return null
    }
    val visibleSides = visibleResizeHandleSides(state)
    return when {
        PinResizeHandleSide.Left in visibleSides && position.x <= touchTarget -> {
            PinResizeHandleSide.Left
        }
        PinResizeHandleSide.Right in visibleSides && position.x >= width - touchTarget -> {
            PinResizeHandleSide.Right
        }
        else -> null
    }
}

private fun visibleResizeHandleSides(state: PinWindowState): List<PinResizeHandleSide> {
    if (state.collapsedEdge != null) {
        return emptyList()
    }
    return buildList {
        if (!isPinnedToLeftEdge(state)) {
            add(PinResizeHandleSide.Left)
        }
        if (!isPinnedToRightEdge(state)) {
            add(PinResizeHandleSide.Right)
        }
    }
}

private fun isPinnedToLeftEdge(state: PinWindowState): Boolean {
    return abs(state.x - pinAllowedXRange(state).start) <= EDGE_SNAP_EPSILON_PX
}

private fun isPinnedToRightEdge(state: PinWindowState): Boolean {
    return abs(state.x - pinAllowedXRange(state).endInclusive) <= EDGE_SNAP_EPSILON_PX
}

private fun pinAllowedXRange(state: PinWindowState): ClosedFloatingPointRange<Float> {
    val root = rootSize
    val handleOutset = pinHandleOutsetPx(state.dimensions).toFloat()
    val minX = state.safeInsets.left - handleOutset
    val maxX = root.width - state.safeInsets.right - state.displayWidth() + handleOutset
    return if (maxX >= minX) {
        minX..maxX
    } else {
        minX..minX
    }
}

private fun twoHandleVisibleXRange(state: PinWindowState): ClosedFloatingPointRange<Float> {
    val root = rootSize
    val minX = state.safeInsets.left.toFloat()
    val maxX = root.width - state.safeInsets.right - state.displayWidth().toFloat()
    return if (maxX >= minX) {
        minX..maxX
    } else {
        pinAllowedXRange(state)
    }
}

private fun twoHandleVisibleYRange(state: PinWindowState): ClosedFloatingPointRange<Float> {
    val root = rootSize
    val minY = state.safeInsets.top.toFloat()
    val maxY = root.height - state.safeInsets.bottom - state.displayHeight().toFloat()
    return if (maxY >= minY) {
        minY..maxY
    } else {
        val handleOutset = pinHandleOutsetPx(state.dimensions).toFloat()
        val fallbackMinY = state.safeInsets.top - handleOutset
        val fallbackMaxY = root.height - state.safeInsets.bottom - state.displayHeight() + handleOutset
        if (fallbackMaxY >= fallbackMinY) {
            fallbackMinY..fallbackMaxY
        } else {
            fallbackMinY..fallbackMinY
        }
    }
}

private fun Offset.distanceTo(other: Offset): Float {
    val x = x - other.x
    val y = y - other.y
    return sqrt(x * x + y * y)
}

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float
): Float {
    return start + (stop - start) * fraction
}

private fun pinChromeSizePx(dimensions: PinnedScreenshotDimensions): Int {
    return ConvertUtils.dp2px(
        dimensions.handleOutset.value + dimensions.imageInset.value
    )
}

private fun pinHandleOutsetPx(dimensions: PinnedScreenshotDimensions): Int {
    return ConvertUtils.dp2px(dimensions.handleOutset.value)
}

private fun deleteTargetHeightPx(dimensions: PinnedScreenshotDimensions): Int {
    return ConvertUtils.dp2px(dimensions.deleteTargetWindowHeight.value)
}

private fun deleteTargetWidthPx(dimensions: PinnedScreenshotDimensions): Int {
    return ConvertUtils.dp2px(dimensions.deleteTargetWidth.value)
}

private fun deleteTargetCardHeightPx(dimensions: PinnedScreenshotDimensions): Int {
    return ConvertUtils.dp2px(dimensions.deleteTargetCardHeight.value)
}

private const val EDGE_SNAP_EPSILON_PX = 1f
