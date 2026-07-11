package com.aaron.sidegesture

import android.content.res.Configuration
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.action.ActionManager
import com.aaron.sidegesture.action.ForegroundAppAware
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
import com.aaron.sidegesture.feature.environment.ServiceEnvironmentMonitor
import com.aaron.sidegesture.feature.gesture.GestureWindowManager
import com.aaron.sidegesture.feature.screenshot.PinnedScreenshotManager
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
            onActionRequest = { actionManager.submit(it) },
            onDismissActionOverlays = { actionManager.dismissOverlays() },
            onButtonsChanged = { pinnedScreenshotManager.onEnvironmentChanged(it) }
        )
    }

    private val actionManager: ActionManager by lazy {
        ActionManager(
            handlers = listOf(
                SystemActionHandler(this),
                MediaActionHandler(this),
                DeviceActionHandler(this, serviceScope),
                AppActionHandler(this, settingsStore, environmentMonitor),
                PaymentActionHandler(this, serviceScope),
                ScrollActionHandler(this, settingsStore),
                ShellActionHandler(this),
                HideGestureButtonActionHandler(gestureWindowManager),
                TaskSwitcherActionHandler(this, serviceScope),
                QuickLauncherActionHandler(this),
                QuickToolsActionHandler(this, settingsStore),
                SmartScreenshotActionHandler(this, serviceScope, pinnedScreenshotManager),
                MoveScreenActionHandler(this, settingsStore, serviceScope)
            ),
            coroutineScope = serviceScope
        )
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
            actionManager.dismissOverlays()
            gestureWindowManager.onConfigurationChanged()
            pinnedScreenshotManager.onEnvironmentChanged(settingsStore.buttons.value)
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
            gestureWindowManager.refreshVisibility()
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
        gestureWindowManager.release()
        pinnedScreenshotManager.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onSetOverlay() {
        actionManager.run { Unit }
        environmentMonitor.start()
        gestureWindowManager.startOrReattach()
        updateCheckScheduler.start()
    }
}
