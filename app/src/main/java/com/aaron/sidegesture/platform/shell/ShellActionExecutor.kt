package com.aaron.sidegesture.platform.shell

import com.aaron.sidegesture.platform.shizuku.ShizukuShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShellActionExecutor {

    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (command.isBlank()) {
            return@withContext ShellResult(errorMessage = "Command is blank")
        }
        val rootResult = ShellCommandRunner.execute(listOf("su", "-c", command))
        if (rootResult.isSuccess) {
            return@withContext rootResult
        }
        ShizukuShellManager.execute(command)
    }
}
