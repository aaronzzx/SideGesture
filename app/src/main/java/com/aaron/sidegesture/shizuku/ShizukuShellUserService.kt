package com.aaron.sidegesture.shizuku

import com.aaron.sidegesture.constant.ActionSettingsDefaults.ShellCommandMaxOutputLength
import com.aaron.sidegesture.constant.ActionSettingsDefaults.ShellCommandTimeoutMs
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ShizukuShellUserService : IShizukuShellService.Stub() {

    override fun execute(command: String): ShellResult {
        if (command.isBlank()) {
            return ShellResult(errorMessage = "Command is blank")
        }
        return try {
            val process = ProcessBuilder("sh", "-c", command).start()
            val stdoutCollector = StreamCollector(process.inputStream)
            val stderrCollector = StreamCollector(process.errorStream)
            val stdoutThread = stdoutCollector.start()
            val stderrThread = stderrCollector.start()
            val finished = waitFor(process, ShellCommandTimeoutMs)
            if (!finished) {
                process.destroy()
            }
            stdoutThread.join(300L)
            stderrThread.join(300L)
            val stdout = stdoutCollector.content()
            val stderr = stderrCollector.content()
            if (!finished) {
                return ShellResult(
                    exitCode = -1,
                    stdout = stdout,
                    stderr = stderr,
                    timedOut = true,
                    errorMessage = "Command timed out"
                )
            }
            ShellResult(
                exitCode = process.exitValue(),
                stdout = stdout,
                stderr = stderr
            )
        } catch (t: Throwable) {
            ShellResult(errorMessage = t.message ?: t.javaClass.simpleName)
        }
    }

    override fun destroy() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun waitFor(process: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(50L)
            }
        }
        return false
    }

    private class StreamCollector(inputStream: InputStream) {
        private val reader = BufferedReader(InputStreamReader(inputStream))
        private val output = StringBuilder()

        fun start(): Thread {
            return Thread {
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (output.isNotEmpty()) {
                            append("\n")
                        }
                        append(line)
                    }
                }
            }.apply { start() }
        }

        fun content(): String {
            return output.toString()
        }

        private fun append(value: String) {
            if (output.length >= ShellCommandMaxOutputLength) {
                return
            }
            val remaining = ShellCommandMaxOutputLength - output.length
            output.append(value.take(remaining))
        }
    }
}
