package com.aaron.sidegesture.utils

import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.LauncherInfo
import com.github.promeg.pinyinhelper.Pinyin
import java.util.Locale

/**
 * @author aaronzzxup@gmail.com
 * @since 2026/5/28
 */
object PinyinSearchUtils {

    fun matches(
        query: String,
        label: String?,
        packageName: String? = null
    ): Boolean {
        val normalizedQuery = query.normalizeForSearch()
        if (normalizedQuery.isEmpty()) return true

        val normalizedLabel = label.normalizeForSearch()
        val normalizedPackageName = packageName.normalizeForSearch()
        val labelFullPinyin = label.toFullPinyin()
        val labelPinyinInitials = label.toPinyinInitials()

        return normalizedLabel.contains(normalizedQuery) ||
                normalizedPackageName.contains(normalizedQuery) ||
                labelFullPinyin.contains(normalizedQuery) ||
                labelPinyinInitials.contains(normalizedQuery)
    }

    fun sortAppInfos(appInfos: List<AppInfo>): List<AppInfo> {
        return appInfos.sortedWith(
            compareBy<AppInfo> { it.label.toSortPinyin() }
                .thenBy { it.label.normalizeForSearch() }
                .thenBy { it.packageName.normalizeForSearch() }
        )
    }

    fun sortLauncherInfos(launcherInfos: List<LauncherInfo>): List<LauncherInfo> {
        return launcherInfos
            .map { launcherInfo ->
                launcherInfo.copy(shortcuts = sortShortcutInfos(launcherInfo.shortcuts))
            }
            .sortedWith(
                compareBy<LauncherInfo> { it.label.toSortPinyin() }
                    .thenBy { it.label.normalizeForSearch() }
                    .thenBy { it.packageName.normalizeForSearch() }
            )
    }

    private fun sortShortcutInfos(
        shortcutInfos: List<LauncherInfo.ShortcutInfo>
    ): List<LauncherInfo.ShortcutInfo> {
        return shortcutInfos.sortedWith(
            compareBy<LauncherInfo.ShortcutInfo> { it.label.toSortPinyin() }
                .thenBy { it.label.normalizeForSearch() }
                .thenBy { it.packageName.normalizeForSearch() }
        )
    }

    private fun String?.normalizeForSearch(): String {
        return orEmpty().trim().lowercase(Locale.ROOT)
    }

    private fun String?.toSortPinyin(): String {
        return toFullPinyin().ifEmpty { normalizeForSearch() }
    }

    private fun String?.toFullPinyin(): String {
        return orEmpty().buildPinyinText { char ->
            if (Pinyin.isChinese(char)) {
                append(Pinyin.toPinyin(char))
            } else if (char.isLetterOrDigit()) {
                append(char.lowercaseChar())
            }
        }
    }

    private fun String?.toPinyinInitials(): String {
        return orEmpty().buildPinyinText { char ->
            if (Pinyin.isChinese(char)) {
                Pinyin.toPinyin(char).firstOrNull()?.let(::append)
            } else if (char.isLetterOrDigit()) {
                append(char.lowercaseChar())
            }
        }
    }

    private inline fun String.buildPinyinText(
        appendText: StringBuilder.(Char) -> Unit
    ): String {
        return buildString {
            this@buildPinyinText.forEach { char ->
                appendText(char)
            }
        }.lowercase(Locale.ROOT)
    }
}
