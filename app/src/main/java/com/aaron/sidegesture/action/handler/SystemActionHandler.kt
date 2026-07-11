package com.aaron.sidegesture.action.handler

import android.accessibilityservice.AccessibilityService
import android.os.Build
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.ktx.launchAssist
import com.aaron.sidegesture.utils.showVersionTooLowToast
import kotlinx.coroutines.delay

class SystemActionHandler(
    private val service: SideGestureService
) : ActionHandler {

    override val supportedActions = setOf(
        GlobalActions.BACK,
        GlobalActions.HOME,
        GlobalActions.RECENT,
        GlobalActions.OPEN_NOTIFICATION_PANEL,
        GlobalActions.OPEN_QUICK_PANEL,
        GlobalActions.LOCK_SCREEN,
        GlobalActions.SPLIT_SCREEN,
        GlobalActions.ASSIST_APP,
        GlobalActions.SCREENSHOT,
        GlobalActions.POWER_BUTTON
    )

    override suspend fun handle(request: ActionRequest) {
        when (request.action.value) {
            GlobalActions.BACK -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            GlobalActions.HOME -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            GlobalActions.RECENT -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            GlobalActions.OPEN_NOTIFICATION_PANEL -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            GlobalActions.OPEN_QUICK_PANEL -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            GlobalActions.LOCK_SCREEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                showVersionTooLowToast(service, R.string.action_lock_screen)
            }
            GlobalActions.SPLIT_SCREEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            } else {
                showVersionTooLowToast(service, R.string.action_split_screen)
            }
            GlobalActions.ASSIST_APP -> service.launchAssist()
            GlobalActions.SCREENSHOT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                delay(500)
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                showVersionTooLowToast(service, R.string.action_screenshot)
            }
            GlobalActions.POWER_BUTTON -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
        }
    }
}
