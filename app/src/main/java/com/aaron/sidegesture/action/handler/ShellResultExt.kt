package com.aaron.sidegesture.action.handler

import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.platform.shell.ShellResult
import com.aaron.sidegesture.utils.showToast

fun SideGestureService.showShellFailureToast(result: ShellResult) {
    val message = when {
        result.timedOut -> getString(R.string.shell_execute_timeout)
        result.errorMessage == "Shizuku permission denied" ||
            result.errorMessage == "Shizuku binder unavailable" -> {
            getString(R.string.shizuku_permission_required)
        }
        result.errorMessage.isNotBlank() -> {
            getString(R.string.shell_execute_failed, result.errorMessage)
        }
        else -> {
            getString(R.string.shell_execute_failed, result.stderr.ifBlank { "-" })
        }
    }
    showToast(message)
}
