package com.aaron.sidegesture.platform.shizuku

import androidx.annotation.Keep
import com.aaron.sidegesture.platform.shell.ShellCommandRunner
import com.aaron.sidegesture.platform.shell.ShellResult

@Keep
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
