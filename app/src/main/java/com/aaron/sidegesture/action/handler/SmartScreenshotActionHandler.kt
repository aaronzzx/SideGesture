package com.aaron.sidegesture.action.handler

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ConfigurationAware
import com.aaron.sidegesture.action.OverlayActionHandler
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.feature.screenshot.CleanScreenshotCoordinator
import com.aaron.sidegesture.feature.screenshot.PinnedScreenshotManager
import com.aaron.sidegesture.feature.screenshot.ScreenshotCropper
import com.aaron.sidegesture.feature.screenshot.ScreenshotShape
import com.aaron.sidegesture.feature.screenshot.ScreenshotStorage
import com.aaron.sidegesture.feature.screenshot.SmartScreenshotEditor
import com.aaron.sidegesture.feature.screenshot.SmartScreenshotState
import com.aaron.sidegesture.ui.theme.WallpaperAwareSideGestureTheme
import com.aaron.sidegesture.utils.showToast
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmartScreenshotActionHandler(
    private val service: SideGestureService,
    private val scope: CoroutineScope,
    private val pinnedScreenshotManager: PinnedScreenshotManager,
    private val screenshotCoordinator: CleanScreenshotCoordinator
) : OverlayActionHandler, ConfigurationAware {

    override val supportedActions = setOf(GlobalActions.SMART_SCREENSHOT)

    private val state = SmartScreenshotState()
    override val touchEnabled: Flow<Boolean> = snapshotFlow { state.visible }
    private var requestVersion = 0L
    private val bitmapUseCounts = mutableMapOf<Bitmap, Int>()
    private val pendingRecycle = mutableSetOf<Bitmap>()

    override suspend fun handle(request: ActionRequest) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            showVersionTooLowToast(service, R.string.action_smart_screenshot)
            return
        }
        val version = ++requestVersion
        state.startCapture()
        val screenshot = screenshotCoordinator.capture()
        if (version != requestVersion) {
            screenshot?.recycle()
            return
        }
        if (screenshot == null) {
            onDismiss()
            showToast(R.string.screenshot_capture_failed)
            return
        }
        state.show(screenshot, ConvertUtils.dp2px(96f))
    }

    override fun onDismiss() {
        requestVersion++
        state.dismiss()
    }

    override fun onConfigurationChanged() {
        onDismiss()
    }

    @Composable
    override fun Content() {
        WallpaperAwareSideGestureTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                val screenshot = state.screenshot
                if (state.visible && screenshot != null) {
                    DisposableEffect(screenshot) {
                        onDispose { recycleWhenUnused(screenshot) }
                    }
                    SmartScreenshotEditor(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = screenshot,
                        state = state,
                        onCancel = ::onDismiss,
                        onSave = {
                            launchCrop(screenshot) { output ->
                                val saved = withContext(Dispatchers.IO) {
                                    ScreenshotStorage.saveToGallery(service, output)
                                }
                                showToast(
                                    if (saved != null) R.string.screenshot_save_success
                                    else R.string.screenshot_save_failed
                                )
                            }
                        },
                        onCopy = {
                            launchCrop(screenshot) { output ->
                                val uri = withContext(Dispatchers.IO) {
                                    ScreenshotStorage.createClipboardUri(service, output)
                                }
                                val copied = uri != null && withContext(Dispatchers.Main) {
                                    ScreenshotStorage.copyToClipboard(service, uri)
                                }
                                showToast(
                                    if (copied) R.string.screenshot_copy_success
                                    else R.string.screenshot_copy_failed
                                )
                            }
                        },
                        onShare = {
                            launchCrop(screenshot) { output ->
                                onDismiss()
                                val uri = withContext(Dispatchers.IO) {
                                    ScreenshotStorage.createShareUri(service, output)
                                }
                                if (uri == null || !ScreenshotStorage.share(service, uri)) {
                                    showToast(R.string.screenshot_share_failed)
                                }
                            }
                        },
                        onPin = {
                            val output = ScreenshotCropper.crop(
                                bitmap = screenshot,
                                selectionRect = state.selectionRect,
                                shape = state.shape
                            )
                            pinnedScreenshotManager.pin(
                                bitmap = output,
                                sourceRect = state.selectionRect
                            )
                            onDismiss()
                        }
                    )
                }
            }
        }
    }

    private suspend fun crop(
        screenshot: Bitmap,
        selectionRect: Rect,
        shape: ScreenshotShape
    ) = withContext(Dispatchers.Default) {
        ScreenshotCropper.crop(
            bitmap = screenshot,
            selectionRect = selectionRect,
            shape = shape
        )
    }

    private fun launchCrop(
        screenshot: Bitmap,
        block: suspend (Bitmap) -> Unit
    ) {
        val selectionRect = state.selectionRect
        val shape = state.shape
        bitmapUseCounts[screenshot] = (bitmapUseCounts[screenshot] ?: 0) + 1
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val output = crop(screenshot, selectionRect, shape)
                try {
                    block(output)
                } finally {
                    output.recycle()
                }
            } finally {
                releaseScreenshotUse(screenshot)
            }
        }
    }

    private fun recycleWhenUnused(screenshot: Bitmap) {
        if ((bitmapUseCounts[screenshot] ?: 0) == 0) {
            screenshot.recycle()
        } else {
            pendingRecycle += screenshot
        }
    }

    private fun releaseScreenshotUse(screenshot: Bitmap) {
        val remaining = (bitmapUseCounts[screenshot] ?: 0) - 1
        if (remaining > 0) {
            bitmapUseCounts[screenshot] = remaining
            return
        }
        bitmapUseCounts.remove(screenshot)
        if (pendingRecycle.remove(screenshot)) screenshot.recycle()
    }
}
