package com.aaron.sidegesture.feature.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.aaron.sidegesture.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * apk 下载 + 拉起系统安装器。
 *
 * 下载失败给可读结果（返回 false），不抛异常、不卡死，由上层 toast/通知提示。
 * 下载写入 `<version>.apk.part`，完成且 size 校验通过后才 rename 为正式 `<version>.apk`，
 * 避免坏文件被提升为可安装包。网络中断保留 `.part` 以便断点续传。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/18
 */
object ApkInstaller {

    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 20000
    private const val BUFFER_SIZE = 8 * 1024

    private const val UPDATE_DIR_NAME = "update"

    /** 下载目录：优先 externalCacheDir，为 null 时回退 cacheDir。 */
    fun updateDir(context: Context): File {
        val base = context.externalCacheDir ?: context.cacheDir
        return File(base, UPDATE_DIR_NAME)
    }

    /** 按版本号命名，做版本隔离，避免不同版本同名 asset 复用到错误缓存。 */
    fun apkFileName(version: String): String {
        val safe = version.trim().ifBlank { "update" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "$safe.apk"
    }

    fun apkFile(context: Context, version: String): File {
        return File(updateDir(context), apkFileName(version))
    }

    /**
     * 断点续传下载到 [destFile]（实际先写 `destFile.part`），通过 [onProgress] 回调 0..100 进度。
     *
     * - 已有完整正式包（size 命中）→ 直接成功。
     * - `.part` 已存在且 `< expectedSize` → `Range: bytes=N-` 续传；服务器 206 续 / 200 从头覆盖。
     * - 完成后 `length == expectedSize` 才 rename 转正；不匹配删除 + 返回失败。
     * - 网络异常保留 `.part` 以便下次续传。
     */
    suspend fun download(
        url: String,
        destFile: File,
        expectedSize: Long,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val partFile = File(destFile.parentFile, destFile.name + ".part")
        var conn: HttpURLConnection? = null
        try {
            destFile.parentFile?.mkdirs()
            // 已有完整正式包：跳过下载
            if (isDownloaded(destFile, expectedSize)) {
                onProgress(100)
                return@withContext true
            }
            var existing = if (partFile.exists()) partFile.length() else 0L
            // .part 大小超出期望（坏残留）→ 删除重下；恰好等于期望的完整 part 留给下面转正
            if (expectedSize > 0 && existing > expectedSize) {
                partFile.delete()
                existing = 0L
            }
            // .part 已完整但尚未转正：直接校验转正，省一次请求
            if (expectedSize > 0 && existing == expectedSize) {
                return@withContext finalizePart(partFile, destFile, expectedSize, onProgress)
            }

            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                if (existing > 0) {
                    setRequestProperty("Range", "bytes=$existing-")
                }
            }
            val append: Boolean
            when (conn.responseCode) {
                HttpURLConnection.HTTP_PARTIAL -> append = true          // 206 续传
                HttpURLConnection.HTTP_OK -> {                           // 200 不支持 Range，从头覆盖
                    append = false
                    existing = 0L
                    if (partFile.exists()) partFile.delete()
                }
                else -> return@withContext false
            }
            val remaining = conn.contentLength.toLong()
            val total = when {
                expectedSize > 0 -> expectedSize
                remaining > 0 -> existing + remaining
                else -> -1L
            }
            var downloaded = existing
            var lastPercent = -1
            conn.inputStream.use { input ->
                FileOutputStream(partFile, append).use { output ->
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
            finalizePart(partFile, destFile, expectedSize, onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 网络中断保留 .part 以便续传，不删除
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 完成校验并把 `.part` 转为正式 apk。
     * size 已知则必须匹配，否则删除 `.part` 并返回失败（防坏包转正）。
     */
    private fun finalizePart(
        partFile: File,
        destFile: File,
        expectedSize: Long,
        onProgress: (Int) -> Unit
    ): Boolean {
        if (expectedSize > 0 && partFile.length() != expectedSize) {
            partFile.delete()
            return false
        }
        if (destFile.exists()) destFile.delete()
        if (!partFile.renameTo(destFile)) {
            // rename 失败（跨卷等）兜底复制
            partFile.copyTo(destFile, overwrite = true)
            partFile.delete()
        }
        onProgress(100)
        return true
    }

    /**
     * 缓存命中判断：文件已存在且大小与 release asset 声明一致，视为已完整下载，可直接安装。
     */
    fun isDownloaded(file: File, expectedSize: Long): Boolean {
        return expectedSize > 0 && file.exists() && file.length() == expectedSize
    }

    /**
     * 清理下载目录下除 [keepFileName]（及其 `.part`）外的其它 apk / 残留 part，避免缓存目录堆积。
     */
    fun clearOutdatedApks(dir: File, keepFileName: String) {
        val files = dir.listFiles() ?: return
        val keepPart = "$keepFileName.part"
        for (file in files) {
            if (!file.isFile) continue
            val name = file.name
            val isApk = name.endsWith(".apk", ignoreCase = true)
            val isPart = name.endsWith(".apk.part", ignoreCase = true)
            if ((isApk || isPart) && name != keepFileName && name != keepPart) {
                file.delete()
            }
        }
    }

    /** 拉起系统安装器；启动失败（无安装器等）返回 false，由上层兜底提示。 */
    fun installApk(context: Context, file: File): Boolean {
        return try {
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
            true
        } catch (e: Exception) {
            false
        }
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
