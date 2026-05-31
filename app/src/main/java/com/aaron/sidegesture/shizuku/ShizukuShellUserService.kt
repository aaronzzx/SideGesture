package com.aaron.sidegesture.shizuku

import com.aaron.sidegesture.utils.ShellCommandRunner

class ShizukuShellUserService : IShizukuShellService.Stub() {

    override fun execute(command: String): ShellResult {
        if (command.isBlank()) {
            return ShellResult(errorMessage = "Command is blank")
        }
        return ShellCommandRunner.execute(listOf("sh", "-c", command))
    }

    override fun destroy() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
