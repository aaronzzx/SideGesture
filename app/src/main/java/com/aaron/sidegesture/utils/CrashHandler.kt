package com.aaron.sidegesture.utils

import com.aaron.sidegesture.constant.Paths
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.TimeUtils
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/10/30
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val SEPARATOR = "gulugulu_CRASH_REPORT"
    private const val CRASH_FILE_NAME = "crashlog"

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    private val crashDir = "${Paths.AppCache}/crash"
    private val crashFilePath = "$crashDir/$CRASH_FILE_NAME"

    override fun uncaughtException(t: Thread, e: Throwable) {
        saveErrorInfo(e)
        defaultHandler?.uncaughtException(t, e)
    }

    fun getCrashFile(): File? {
        val path = crashFilePath
        if (!FileUtils.isFileExists(path)) {
            return null
        }
        return File(path)
    }

    fun reset() {
        FileUtils.delete(crashFilePath)
    }

    fun getCrashList(): List<String> {
        val string = FileIOUtils.readFile2String(crashFilePath)
        if (string.isNullOrEmpty()) {
            return emptyList()
        }
        return string.split(SEPARATOR).map { it.trimIndent() }.filter { it.isNotEmpty() }
    }

    private fun saveErrorInfo(e: Throwable) {
        val stringBuffer = StringBuffer()
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        //获取到堆栈信息
        e.printStackTrace(printWriter)
        printWriter.close()
        //转换异常信息
        val errorStackInfo = stringWriter.toString()
        stringBuffer.append("Time: ${TimeUtils.millis2String(System.currentTimeMillis())}\n")
        stringBuffer.append(errorStackInfo)
        stringBuffer.append("$SEPARATOR\n")

        val cache = FileIOUtils.readFile2String(crashFilePath)
        if (!cache.isNullOrEmpty()) {
            stringBuffer.append(cache)
        }

        FileUtils.createOrExistsDir(crashDir)
        FileUtils.createOrExistsFile(crashFilePath)
        FileIOUtils.writeFileFromString(crashFilePath, stringBuffer.toString(), false)
    }
}