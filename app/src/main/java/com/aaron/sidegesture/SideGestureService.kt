package com.aaron.sidegesture

import android.content.res.Configuration
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionManager
import com.aaron.sidegesture.action.ForegroundAppAware
import com.aaron.sidegesture.action.OverlayActionHandler
import com.aaron.sidegesture.action.handler.AppActionHandler
import com.aaron.sidegesture.action.handler.DeviceActionHandler
import com.aaron.sidegesture.action.handler.HideGestureButtonActionHandler
import com.aaron.sidegesture.action.handler.MediaActionHandler
import com.aaron.sidegesture.action.handler.MoveScreenActionHandler
import com.aaron.sidegesture.action.handler.PaymentActionHandler
import com.aaron.sidegesture.action.handler.QuickLauncherActionHandler
import com.aaron.sidegesture.action.handler.QuickToolsActionHandler
import com.aaron.sidegesture.action.handler.ScrollActionHandler
import com.aaron.sidegesture.action.handler.ShellActionHandler
import com.aaron.sidegesture.action.handler.SmartScreenshotActionHandler
import com.aaron.sidegesture.action.handler.SystemActionHandler
import com.aaron.sidegesture.action.handler.TaskSwitcherActionHandler
import com.aaron.sidegesture.feature.actionoverlay.ActionOverlayHost
import com.aaron.sidegesture.feature.environment.ServiceEnvironmentMonitor
import com.aaron.sidegesture.feature.gesture.GestureWindowManager
import com.aaron.sidegesture.feature.screenshot.CleanScreenshotCoordinator
import com.aaron.sidegesture.feature.screenshot.PinnedScreenshotManager
import com.aaron.sidegesture.feature.screenshot.WindowVisibilityController
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.feature.update.UpdateCheckScheduler
import com.aaron.sidegesture.feature.volumebutton.VolumeButtonController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private val serviceScope = MainScope()
    private val settingsStore = ServiceSettingsStore(serviceScope)
    private val pinnedScreenshotManager: PinnedScreenshotManager by lazy {
        PinnedScreenshotManager(this)
    }

    private val actionOverlayHost by lazy {
        ActionOverlayHost(this, serviceScope)
    }

    private val environmentMonitor: ServiceEnvironmentMonitor by lazy {
        ServiceEnvironmentMonitor(
            service = this,
            scope = serviceScope,
            settingsStore = settingsStore,
            onScreenLockChanged = { locked ->
                if (locked) actionManager.dismissOverlays()
                pinnedScreenshotManager.setScreenLocked(locked)
                gestureWindowManager.refreshVisibility()
            }
        )
    }

    private val gestureWindowManager: GestureWindowManager by lazy {
        GestureWindowManager(
            service = this,
            scope = serviceScope,
            settingsStore = settingsStore,
            environmentMonitor = environmentMonitor,
            actionOverlayTouchEnabled = actionOverlayHost.touchEnabled,
            onActionRequest = { actionManager.submit(it) },
            onDismissActionOverlays = { actionManager.dismissOverlays() },
            onButtonsChanged = { pinnedScreenshotManager.onEnvironmentChanged(it) }
        )
    }

    private val screenshotCoordinator by lazy {
        CleanScreenshotCoordinator(
            service = this,
            windowVisibilityController = object : WindowVisibilityController {
                override fun hideWindowsForScreenshot() {
                    gestureWindowManager.setTemporarilyHidden(true)
                    actionOverlayHost.setTemporarilyHidden(true)
                }

                override fun restoreWindowsAfterScreenshot() {
                    gestureWindowManager.setTemporarilyHidden(false)
                    actionOverlayHost.setTemporarilyHidden(false)
                }
            }
        )
    }

    private val actionHandlers: List<ActionHandler> by lazy {
        listOf(
            SystemActionHandler(this),
            MediaActionHandler(this),
            DeviceActionHandler(this, serviceScope),
            AppActionHandler(this, settingsStore, environmentMonitor, serviceScope),
            PaymentActionHandler(this, serviceScope),
            ScrollActionHandler(this, settingsStore),
            ShellActionHandler(this),
            HideGestureButtonActionHandler(gestureWindowManager),
            TaskSwitcherActionHandler(this, serviceScope),
            QuickLauncherActionHandler(),
            QuickToolsActionHandler(this, settingsStore),
            SmartScreenshotActionHandler(
                this,
                serviceScope,
                pinnedScreenshotManager,
                screenshotCoordinator
            ),
            MoveScreenActionHandler(
                this,
                settingsStore,
                serviceScope
            )
        )
    }

    private val actionManager: ActionManager by lazy {
        ActionManager(actionHandlers, serviceScope)
    }

    private val volumeButtonController by lazy {
        VolumeButtonController(this, serviceScope, settingsStore)
    }

    private val updateCheckScheduler by lazy {
        UpdateCheckScheduler(this, serviceScope, settingsStore)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (environmentMonitor.onConfigurationChanged(newConfig)) {
            actionManager.onConfigurationChanged()
            gestureWindowManager.onConfigurationChanged()
            actionOverlayHost.onConfigurationChanged()
            settingsStore.currentSnapshotOrNull()?.let { settings ->
                pinnedScreenshotManager.onEnvironmentChanged(settings.buttons)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            actionManager.onForegroundAppChanged(
                ForegroundAppAware.Snapshot(
                    packageName = event.packageName?.toString(),
                    className = event.className?.toString()
                )
            )
        }
        if (event != null && environmentMonitor.onAccessibilityEvent(event)) {
            gestureWindowManager.requestVisibilityRefresh()
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (volumeButtonController.handle(event)) return true
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        actionManager.dismissOverlays()
        updateCheckScheduler.stop()
        environmentMonitor.stop()
        actionOverlayHost.release()
        gestureWindowManager.release()
        pinnedScreenshotManager.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onSetOverlay() {
        actionManager.start()
        environmentMonitor.start()
        gestureWindowManager.startOrReattach()
        actionOverlayHost.attach(actionHandlers.filterIsInstance<OverlayActionHandler>())
        updateCheckScheduler.start()
    }
}
