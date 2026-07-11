package com.aaron.sidegesture.action.handler

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.OverlayDismissAware
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.feature.screenshot.ScreenshotCropper
import com.aaron.sidegesture.feature.screenshot.ScreenshotStorage
import com.aaron.sidegesture.feature.screenshot.SmartScreenshotEditor
import com.aaron.sidegesture.feature.screenshot.SmartScreenshotState
import com.aaron.sidegesture.feature.screenshot.PinnedScreenshotManager
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.setFlags
import com.aaron.sidegesture.ktx.takeScreenshot
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.utils.showToast
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmartScreenshotActionHandler(
    private val service: SideGestureService,
    private val scope: CoroutineScope,
    private val pinnedScreenshotManager: PinnedScreenshotManager
) : ActionHandler, OverlayDismissAware {

    override val supportedActions = setOf(GlobalActions.SMART_SCREENSHOT)

    private val state = SmartScreenshotState()
    private var window: View? = null
    private var requestVersion = 0L

    override suspend fun handle(request: ActionRequest) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            showVersionTooLowToast(service, R.string.action_smart_screenshot)
            return
        }
        val version = ++requestVersion
        state.startCapture()
        delay(500)
        if (version != requestVersion) return
        val screenshot = service.takeScreenshot()
        if (version != requestVersion) {
            screenshot?.recycle()
            return
        }
        if (screenshot == null) {
            onDismiss()
            showToast(R.string.screenshot_capture_failed)
            return
        }
        ensureWindow()
        state.show(screenshot, ConvertUtils.dp2px(96f))
    }

    override fun onDismiss() {
        requestVersion++
        state.dismiss()
        state.cancelCapture()
        window?.let(service::removeWindow)
        window = null
    }

    private fun ensureWindow() {
        if (window != null) return
        window = service.attachComposeOverlay {
            SideGestureTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val screenshot = state.screenshot
                    if (state.visible && screenshot != null) {
                        SmartScreenshotEditor(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = screenshot,
                            state = state,
                            onCancel = ::onDismiss,
                            onSave = {
                                scope.launch {
                                    val output = crop(screenshot)
                                    val saved = withContext(Dispatchers.IO) {
                                        ScreenshotStorage.saveToGallery(service, output)
                                    }
                                    showToast(
                                        if (saved != null) R.string.screenshot_save_success
                                        else R.string.screenshot_save_failed
                                    )
                                    output.recycle()
                                }
                            },
                            onCopy = {
                                scope.launch {
                                    val output = crop(screenshot)
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
                                    output.recycle()
                                }
                            },
                            onShare = {
                                scope.launch {
                                    val output = crop(screenshot)
                                    onDismiss()
                                    val uri = withContext(Dispatchers.IO) {
                                        ScreenshotStorage.createShareUri(service, output)
                                    }
                                    if (uri == null || !ScreenshotStorage.share(service, uri)) {
                                        showToast(R.string.screenshot_share_failed)
                                    }
                                    output.recycle()
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
        setTouchEnabled(true)
    }

    private suspend fun crop(screenshot: android.graphics.Bitmap) = withContext(Dispatchers.Default) {
        ScreenshotCropper.crop(
            bitmap = screenshot,
            selectionRect = state.selectionRect,
            shape = state.shape
        )
    }

    private fun setTouchEnabled(enabled: Boolean) {
        val view = window ?: return
        val params = (view.layoutParams as WindowManager.LayoutParams).apply {
            setFlags(enabled)
        }
        service.updateLayout(view, params)
    }
}
