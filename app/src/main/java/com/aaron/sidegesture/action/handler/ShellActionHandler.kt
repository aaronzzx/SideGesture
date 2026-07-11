package com.aaron.sidegesture.action.handler

import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.ktx.shellCommandActionData
import com.aaron.sidegesture.platform.shell.ShellActionExecutor
import com.aaron.sidegesture.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShellActionHandler(
    private val service: SideGestureService
) : ActionHandler {

    override val supportedActions = setOf(GlobalActions.SHIZUKU_SHELL)

    override suspend fun handle(request: ActionRequest) {
        val command = request.action.shellCommandActionData?.command.orEmpty()
        if (command.isBlank()) {
            showToast(R.string.shell_command_empty)
            return
        }
        val result = withContext(Dispatchers.IO) {
            ShellActionExecutor.execute(command)
        }
        if (!result.isSuccess) {
            service.showShellFailureToast(result)
        }
    }
}
