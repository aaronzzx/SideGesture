package com.aaron.sidegesture.feature.screenshot

import android.graphics.Bitmap
import android.os.Build
import android.view.Choreographer
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.ktx.takeScreenshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

interface WindowVisibilityController {

    fun hideWindowsForScreenshot()

    fun restoreWindowsAfterScreenshot()
}

class CleanScreenshotCoordinator(
    private val windowVisibilityController: WindowVisibilityController,
    private val captureScreenshot: suspend () -> Bitmap?,
    private val hiddenFrameWaiter: suspend () -> Unit,
    private val isScreenshotSupported: () -> Boolean,
    private val mainDispatcher: CoroutineDispatcher
) {

    private val captureMutex = Mutex()

    constructor(
        service: SideGestureService,
        windowVisibilityController: WindowVisibilityController
    ) : this(
        windowVisibilityController = windowVisibilityController,
        captureScreenshot = { service.takeScreenshot() },
        hiddenFrameWaiter = ::awaitHiddenWindowsDrawn,
        isScreenshotSupported = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.R },
        mainDispatcher = Dispatchers.Main.immediate
    )

    suspend fun capture(): Bitmap? {
        if (!isScreenshotSupported()) return null
        return captureMutex.withLock {
            var restoreRequired = false
            try {
                withContext(mainDispatcher) {
                    restoreRequired = true
                    windowVisibilityController.hideWindowsForScreenshot()
                    hiddenFrameWaiter()
                }
                captureScreenshot()
            } finally {
                if (restoreRequired) {
                    withContext(NonCancellable + mainDispatcher) {
                        windowVisibilityController.restoreWindowsAfterScreenshot()
                    }
                }
            }
        }
    }
}

private suspend fun awaitHiddenWindowsDrawn() {
    awaitFrame()
    awaitFrame()
}

private suspend fun awaitFrame() = suspendCancellableCoroutine { continuation ->
    val choreographer = Choreographer.getInstance()
    val callback = Choreographer.FrameCallback {
        if (continuation.isActive) continuation.resume(Unit)
    }
    continuation.invokeOnCancellation {
        choreographer.removeFrameCallback(callback)
    }
    choreographer.postFrameCallback(callback)
}
