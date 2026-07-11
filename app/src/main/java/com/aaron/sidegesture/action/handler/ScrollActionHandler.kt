package com.aaron.sidegesture.action.handler

import android.os.Build
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.utils.AccessibilityUtils
import com.aaron.sidegesture.utils.showVersionTooLowToast

class ScrollActionHandler internal constructor(
    private val service: SideGestureService,
    private val settingsStore: ServiceSettingsStore
) : ActionHandler {

    override val supportedActions = setOf(
        GlobalActions.BACK_TO_TOP,
        GlobalActions.GOTO_BOTTOM
    )

    override suspend fun handle(request: ActionRequest) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val text = if (request.action.value == GlobalActions.BACK_TO_TOP) {
                R.string.action_back_to_top
            } else {
                R.string.action_goto_bottom
            }
            showVersionTooLowToast(service, text)
            return
        }
        when (request.action.value) {
            GlobalActions.BACK_TO_TOP -> AccessibilityUtils.fastVerticalScroll(service, true)
            GlobalActions.GOTO_BOTTOM -> AccessibilityUtils.fastVerticalScroll(
                service,
                false,
                settingsStore.actionSettings.value.gotoBottom.strength
            )
        }
    }
}
