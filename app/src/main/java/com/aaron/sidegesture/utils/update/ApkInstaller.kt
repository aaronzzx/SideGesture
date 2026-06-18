package com.aaron.sidegesture.utils.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.aaron.sidegesture.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * apk 下载 + 拉起系统安装器。
 *
 * 下载失败给可读结果（返回 false），不抛异常、不卡死，由上层 toast 提示。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/18
 */
object ApkInstaller {

    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 20000
    private const val BUFFER_SIZE = 8 * 1024

    /**
     * 流式下载到 [destFile]，通过 [onProgress] 回调 0..100 进度（仅在百分比变化时回调）。
     */
    suspend fun download(
        url: String,
        destFile: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            destFile.parentFile?.mkdirs()
            if (destFile.exists()) destFile.delete()
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext false
            }
            val total = conn.contentLength.toLong()
            var downloaded = 0L
            var lastPercent = -1
            conn.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            if (destFile.exists()) destFile.delete()
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 缓存命中判断：文件已存在且大小与 release asset 声明一致，视为已完整下载，可直接安装。
     */
    fun isDownloaded(file: File, expectedSize: Long): Boolean {
        return expectedSize > 0 && file.exists() && file.length() == expectedSize
    }

    /**
     * 清理下载目录下除 [keepFileName] 外的其它 apk（旧版本残留），避免缓存目录堆积。
     */
    fun clearOutdatedApks(dir: File, keepFileName: String) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isFile && file.name.endsWith(".apk", ignoreCase = true) && file.name != keepFileName) {
                file.delete()
            }
        }
    }

    fun installApk(context: Context, file: File) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canInstall(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun gotoUnknownSourceSetting(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (ignored: Exception) {
            }
        }
    }
}
