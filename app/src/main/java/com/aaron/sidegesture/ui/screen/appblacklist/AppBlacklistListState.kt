package com.aaron.sidegesture.ui.screen.appblacklist

import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.utils.PinyinSearchUtils

data class AppBlacklistListState(
    val entryExcludeApps: List<String> = emptyList(),
    val excludeApps: List<String> = emptyList(),
    val orderedAppInfos: List<AppInfo> = emptyList(),
    val visibleAppInfos: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val savedSelectionLoaded: Boolean = false
) {

    fun loadSavedSelection(packageNames: List<String>): AppBlacklistListState {
        val canonicalPackageNames = packageNames.distinct()
        val orderedAppInfos = orderForEntry(
            appInfos = orderedAppInfos,
            entryExcludeApps = canonicalPackageNames
        )
        return copy(
            entryExcludeApps = canonicalPackageNames,
            excludeApps = canonicalPackageNames,
            orderedAppInfos = orderedAppInfos,
            visibleAppInfos = filterAppInfos(orderedAppInfos, searchQuery),
            savedSelectionLoaded = true
        )
    }

    fun loadAppInfos(appInfos: List<AppInfo>): AppBlacklistListState {
        val orderedAppInfos = orderForEntry(
            appInfos = appInfos,
            entryExcludeApps = entryExcludeApps
        )
        return copy(
            orderedAppInfos = orderedAppInfos,
            visibleAppInfos = if (savedSelectionLoaded) {
                filterAppInfos(orderedAppInfos, searchQuery)
            } else {
                emptyList()
            }
        )
    }

    fun selectApp(packageName: String, selected: Boolean): AppBlacklistListState {
        val updatedExcludeApps = when {
            selected && packageName !in excludeApps -> excludeApps + packageName
            !selected -> excludeApps.filterNot { it == packageName }
            else -> excludeApps
        }
        return copy(excludeApps = updatedExcludeApps)
    }

    fun clearSelection(): AppBlacklistListState = copy(excludeApps = emptyList())

    fun updateSearchQuery(query: String): AppBlacklistListState {
        return copy(
            searchQuery = query,
            visibleAppInfos = filterAppInfos(orderedAppInfos, query)
        )
    }

    fun isSelected(packageName: String): Boolean = packageName in excludeApps

    private fun orderForEntry(
        appInfos: List<AppInfo>,
        entryExcludeApps: List<String>
    ): List<AppInfo> {
        val (selected, unselected) = appInfos.partition { appInfo ->
            appInfo.packageName in entryExcludeApps
        }
        return selected + unselected
    }

    private fun filterAppInfos(appInfos: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return appInfos
        return appInfos.filter { appInfo ->
            PinyinSearchUtils.matches(
                query = query,
                label = appInfo.label,
                packageName = appInfo.packageName
            )
        }
    }
}
