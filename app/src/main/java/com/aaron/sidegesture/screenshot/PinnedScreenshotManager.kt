package com.aaron.sidegesture.screenshot

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.ktx.rootSize
import com.blankj.utilcode.util.ConvertUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PinnedScreenshotManager(
    private val service: SideGestureService
) {

    private val windowManager = ContextCompat.getSystemService(service, WindowManager::class.java)!!
    private val windows = linkedMapOf<String, PinWindow>()
    private var isScreenLocked = false

    fun pin(bitmap: Bitmap, buttons: List<GestureButton>) {
        val safeInsets = PinSafeInsets.from(service, windowManager, buttons)
        val root = rootSize
        val initialScale = min(
            1f,
            min(
                root.width * 0.45f / bitmap.width,
                root.height * 0.45f / bitmap.height
            )
        ).coerceIn(MIN_SCALE, maxScale(bitmap))
        val state = PinWindowState(
            id = SystemClock.uptimeMillis().toString(),
            bitmap = bitmap,
            scale = initialScale,
            x = ((root.width - bitmap.width * initialScale) / 2f).coerceAtLeast(0f),
            y = ((root.height - bitmap.height * initialScale) / 2f).coerceAtLeast(0f),
            safeInsets = safeInsets
        )
        state.anchoredEdge = nearestEdge(state)
        val view = ComposeView(service).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeViewModelStoreOwner(service)
            setViewTreeSavedStateRegistryOwner(service)
            setContent {
                MaterialTheme {
                    PinnedScreenshotWindow(
                        state = state,
                        onClose = { remove(state.id) },
                        onTransform = { pan, zoom, pointerCount, time ->
                            handleTransform(state, pan, zoom, pointerCount, time)
                        },
                        onGestureEnd = { pointerCount, moved ->
                            handleGestureEnd(state, pointerCount, moved)
                        },
                        onCollapsedTap = {
                            restore(state)
                        }
                    )
                }
            }
        }
        val layoutParams = createLayoutParams(state)
        windowManager.addView(view, layoutParams)
        view.alpha = if (isScreenLocked) 0f else 1f
        windows[state.id] = PinWindow(state, view, layoutParams)
    }

    fun onEnvironmentChanged(buttons: List<GestureButton>) {
        val safeInsets = PinSafeInsets.from(service, windowManager, buttons)
        windows.values.forEach { window ->
            val state = window.state
            state.safeInsets = safeInsets
            val maxScale = maxScale(state.bitmap)
            if (state.scale > maxScale) {
                state.scale = maxScale
            }
            if (state.normalScale > maxScale) {
                state.normalScale = maxScale
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
        val alpha = if (locked) 0f else 1f
        windows.values.forEach { window ->
            window.view.alpha = alpha
        }
    }

    fun release() {
        windows.values.toList().forEach { window ->
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
        try {
            windowManager.removeViewImmediate(window.view)
        } catch (_: Exception) {
        }
        window.state.bitmap.recycle()
    }

    private fun handleTransform(
        state: PinWindowState,
        pan: Offset,
        zoom: Float,
        pointerCount: Int,
        uptimeMillis: Long
    ) {
        state.scale = (state.scale * zoom).coerceIn(MIN_SCALE, maxScale(state.bitmap))
        state.x += pan.x
        state.y += pan.y
        clampVisible(state)
        if (pointerCount == 1 && pan != Offset.Zero) {
            state.dragSamples += PinDragSample(uptimeMillis, state.center())
            trimSamples(state.dragSamples, uptimeMillis)
        }
        updateLayout(state.id)
    }

    private fun handleGestureEnd(
        state: PinWindowState,
        pointerCount: Int,
        moved: Boolean
    ) {
        val isTap = pointerCount == 1 && !moved
        if (isTap && state.collapsedEdge != null) {
            restore(state)
            return
        }

        if (state.collapsedEdge != null) {
            snapToEdge(state, collapsed = true)
        } else if (shouldCollapse(state)) {
            state.normalScale = state.scale
            state.normalX = state.x
            state.normalY = state.y
            state.scale = max(MIN_SCALE, state.scale * 0.35f)
            snapToEdge(state, collapsed = true)
        } else {
            snapToEdge(state, collapsed = false)
        }
        updateLayout(state.id)
        state.dragSamples.clear()
    }

    private fun restore(state: PinWindowState) {
        val edge = state.collapsedEdge ?: return
        val targetY = state.y
        state.collapsedEdge = null
        state.scale = state.normalScale.coerceIn(MIN_SCALE, maxScale(state.bitmap))
        val xRange = allowedXRange(state)
        val yRange = allowedYRange(state)
        state.x = when (edge) {
            PinEdge.Left -> xRange.start
            PinEdge.Right -> xRange.endInclusive
        }
        state.y = targetY.coerceIn(yRange.start, yRange.endInclusive)
        clampVisible(state)
        state.normalX = state.x
        state.normalY = state.y
        updateLayout(state.id)
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
        collapsed: Boolean
    ) {
        val edge = nearestEdge(state)
        val xRange = allowedXRange(state)
        val yRange = allowedYRange(state)
        state.x = when (edge) {
            PinEdge.Left -> xRange.start
            PinEdge.Right -> xRange.endInclusive
        }
        state.y = state.y.coerceIn(yRange.start, yRange.endInclusive)
        state.collapsedEdge = if (collapsed) edge else null
        state.anchoredEdge = edge
        clampVisible(state)
    }

    private fun clampVisible(state: PinWindowState) {
        val xRange = allowedXRange(state)
        val yRange = allowedYRange(state)
        state.x = state.x.coerceIn(xRange.start, xRange.endInclusive)
        state.y = state.y.coerceIn(yRange.start, yRange.endInclusive)
    }

    private fun maxScale(bitmap: Bitmap): Float {
        val root = rootSize
        return min(
            MAX_SCALE,
            min(
                root.width * 0.85f / bitmap.width,
                root.height * 0.85f / bitmap.height
            )
        )
    }

    private fun trimSamples(samples: MutableList<PinDragSample>, now: Long) {
        while (samples.size > 1 && now - samples.first().time > FLING_WINDOW_MS) {
            samples.removeAt(0)
        }
    }

    private fun allowedXRange(state: PinWindowState): ClosedFloatingPointRange<Float> {
        val root = rootSize
        val minX = state.safeInsets.left.toFloat()
        val maxX = (root.width - state.safeInsets.right - state.displayWidth()).toFloat()
        return if (maxX >= minX) {
            minX..maxX
        } else {
            minX..minX
        }
    }

    private fun allowedYRange(state: PinWindowState): ClosedFloatingPointRange<Float> {
        val root = rootSize
        val minY = state.safeInsets.top.toFloat()
        val maxY = (root.height - state.safeInsets.bottom - state.displayHeight()).toFloat()
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

    @SuppressLint("RtlHardcoded")
    private fun createLayoutParams(state: PinWindowState): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            width = state.displayWidth()
            height = state.displayHeight()
            x = state.x.roundToInt()
            y = state.y.roundToInt()
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            gravity = Gravity.LEFT or Gravity.TOP
        }
    }

    private data class PinWindow(
        val state: PinWindowState,
        val view: ComposeView,
        val layoutParams: WindowManager.LayoutParams
    )

    private companion object {
        const val MIN_SCALE = 0.35f
        const val MAX_SCALE = 2.5f
        const val FLING_VELOCITY_THRESHOLD = 1800f
        const val FLING_WINDOW_MS = 120L
    }
}

private enum class PinEdge {
    Left, Right
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
                top = max(systemInsets.top, fallback),
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

private class PinWindowState(
    val id: String,
    val bitmap: Bitmap,
    scale: Float,
    x: Float,
    y: Float,
    safeInsets: PinSafeInsets
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
    val dragSamples: MutableList<PinDragSample> = mutableListOf()

    fun displayWidth(): Int = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    fun displayHeight(): Int = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    fun center(): Offset = Offset(x + displayWidth() / 2f, y + displayHeight() / 2f)
}

@Composable
private fun PinnedScreenshotWindow(
    state: PinWindowState,
    onClose: () -> Unit,
    onTransform: (Offset, Float, Int, Long) -> Unit,
    onGestureEnd: (Int, Boolean) -> Unit,
    onCollapsedTap: () -> Unit
) {
    val bitmap = remember(state.bitmap) { state.bitmap.asImageBitmap() }
    val latestOnTransform = rememberUpdatedState(onTransform)
    val latestOnGestureEnd = rememberUpdatedState(onGestureEnd)
    val latestOnCollapsedTap = rememberUpdatedState(onCollapsedTap)
    val imageShape = remember { androidx.compose.foundation.shape.RoundedCornerShape(18.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    var pointerCountMax = 1
                    var moved = false
                    var event = awaitPointerEvent(pass = PointerEventPass.Main)
                    while (event.changes.any { it.pressed && !it.changedToUpIgnoreConsumed() }) {
                        pointerCountMax = max(pointerCountMax, event.changes.size)
                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()
                        if (pan != Offset.Zero || zoom != 1f) {
                            moved = true
                            latestOnTransform.value(pan, zoom, event.changes.size, SystemClock.uptimeMillis())
                            event.changes.forEach { if (it.pressed) it.consume() }
                        }
                        event = awaitPointerEvent(pass = PointerEventPass.Main)
                    }
                    if (!moved && state.collapsedEdge != null) {
                        latestOnCollapsedTap.value()
                    } else {
                        latestOnGestureEnd.value(pointerCountMax, moved)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 18.dp, shape = imageShape, clip = false)
                .clip(imageShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = bitmap,
                contentDescription = null
            )
        }
        FilledIconButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(closeButtonPadding(state.anchoredEdge))
                .size(28.dp),
            onClick = onClose,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.72f),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null
            )
        }
    }
}

private fun closeButtonPadding(edge: PinEdge): androidx.compose.foundation.layout.PaddingValues {
    val endPadding = if (edge == PinEdge.Right) 2.dp else 4.dp
    return androidx.compose.foundation.layout.PaddingValues(
        top = 2.dp,
        end = endPadding
    )
}
