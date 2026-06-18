package com.aaron.sidegesture.utils.update

import com.aaron.sidegesture.entity.GithubRelease
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查更新：拉取 GitHub 最新 release 并比对版本。
 *
 * 用户群是国内小米/酷安用户，api.github.com 访问不稳，因此用短超时（5s）+ 静默失败：
 * 任何异常都返回 null，由上层决定「静默」还是「toast」。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/18
 */
object UpdateChecker {

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/aaronzzx/gulugulu/releases/latest"
    private const val TIMEOUT_MS = 5000

    /** release 列表页，html_url 缺失时的回退跳转地址。 */
    const val RELEASES_PAGE_URL = "https://github.com/aaronzzx/gulugulu/releases"

    suspend fun fetchLatestRelease(): GithubRelease? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JsonHelper.decodeFromString<GithubRelease>(body)
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * 比较远端 tag 与本地版本号，去掉前缀 v，按 x.y.z 逐段数值比较。
     */
    fun isRemoteNewer(remoteTag: String, localName: String): Boolean {
        val remote = parseVersion(remoteTag)
        val local = parseVersion(localName)
        val size = maxOf(remote.size, local.size)
        for (i in 0 until size) {
            val r = remote.getOrElse(i) { 0 }
            val l = local.getOrElse(i) { 0 }
            if (r != l) return r > l
        }
        return false
    }

    private fun parseVersion(version: String): List<Int> {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split(".")
            .map { segment -> segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    }

    fun pickApkAsset(release: GithubRelease): GithubRelease.Asset? {
        return release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    }
}
