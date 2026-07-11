package com.aaron.sidegesture.action.handler

import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.constant.GlobalActions

class HideGestureButtonActionHandler(
    private val service: SideGestureService
) : ActionHandler {

    override val supportedActions = setOf(GlobalActions.HIDE_GESTURE_BUTTON)

    override suspend fun handle(request: ActionRequest) {
        service.hideGestureButton(request.actionContext?.button)
    }
}
