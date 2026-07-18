package com.aaron.sidegesture.action.handler

import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.feature.gesture.GestureWindowManager

class HideGestureButtonActionHandler(
    private val gestureWindowManager: GestureWindowManager
) : ActionHandler {

    override val supportedActions = setOf(GlobalActions.HIDE_GESTURE_BUTTON)

    override suspend fun handle(request: ActionRequest) {
        gestureWindowManager.hide(request.actionContext?.button)
    }
}
