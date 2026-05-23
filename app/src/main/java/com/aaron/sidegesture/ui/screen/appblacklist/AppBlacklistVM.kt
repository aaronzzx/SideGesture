package com.aaron.sidegesture.ui.screen.appblacklist

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.coerceTimeMillis
import com.aaron.sidegesture.ui.screen.appblacklist.AppBlacklistVM.UiEvent
import com.aaron.sidegesture.ui.screen.appblacklist.AppBlacklistVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/1
 */
class AppBlacklistVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
    }

    fun showSearch() {
        updateUiState {
            it.copy(isSearching = true)
        }
    }

    fun hideSearch() {
        updateUiState {
            applySearchResult(
                it.copy(
                    isSearching = false,
                    searchQuery = ""
                )
            )
        }
    }

    fun updateSearchQuery(query: String) {
        updateUiState {
            applySearchResult(it.copy(searchQuery = query))
        }
    }

    fun selectApp(appInfo: AppInfo, selected: Boolean) {
        updateUiState {
            val mutableList = it.excludeApps.toMutableList()
            if (selected) {
                mutableList.add(appInfo.packageName)
            } else {
                mutableList.remove(appInfo.packageName)
            }
            applySearchResult(it.copy(excludeApps = mutableList))
        }
    }

    fun done() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                it.copy(excludeApps = uiState.excludeApps)
            }
        }.invokeOnCompletion {
            if (it == null) {
                toast(R.string.save_success)
                finish()
            } else {
                toast(R.string.save_failure)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                it.copy(excludeApps = emptyList())
            }
        }.invokeOnCompletion {
            if (it == null) {
                updateUiState { uiState ->
                    applySearchResult(uiState.copy(excludeApps = emptyList()))
                }
                toast(R.string.reset_success)
            } else {
                toast(R.string.reset_failure)
            }
        }
    }

    fun updateAppInfos() {
        viewModelScope.launchWithLoading {
            val appInfos = withContext(Dispatchers.IO) {
                coerceTimeMillis(500) {
                    AppInfoUtils
                        .queryLauncherActivities(App.getContext())
                        .filter {
                            // 排除自己
                            it.packageName != App.getContext().packageName
                        }
                }
            }
            arrangeAppInfos(appInfos)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder
                .advancedSettings
                .data
                .take(1)
                .collectLatest { item ->
                    updateUiState {
                        applySearchResult(it.copy(excludeApps = item.excludeApps))
                    }
                }
        }
    }

    private fun applySearchResult(uiState: UiState): UiState {
        return arrangeAppInfos(uiState.rawAppInfos, uiState.excludeApps, uiState.searchQuery).let {
            uiState.copy(
                excludeApps = it.excludeApps,
                selectedAppInfos = it.selectedAppInfos,
                unselectedAppInfos = it.unselectedAppInfos
            )
        }
    }

    private fun filterAppInfos(appInfos: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return appInfos
        return appInfos.filter { appInfo ->
            matchesSearchQuery(
                query = query,
                values = arrayOf(appInfo.label, appInfo.packageName)
            )
        }
    }

    private fun matchesSearchQuery(
        query: String,
        values: Array<String?>
    ): Boolean {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return true
        return values.any { value ->
            value?.contains(normalizedQuery, ignoreCase = true) == true
        }
    }

    private suspend fun arrangeAppInfos(appInfos: List<AppInfo>) {
        val arrangedState = withContext(Dispatchers.Default) {
            val arranged = arrangeAppInfos(
                appInfos = appInfos,
                excludeApps = uiState.excludeApps,
                query = uiState.searchQuery
            )
            arranged
        }
        updateUiState {
            it.copy(
                rawAppInfos = appInfos,
                excludeApps = arrangedState.excludeApps,
                selectedAppInfos = arrangedState.selectedAppInfos,
                unselectedAppInfos = arrangedState.unselectedAppInfos
            )
        }
    }

    private fun arrangeAppInfos(
        appInfos: List<AppInfo>,
        excludeApps: List<String>,
        query: String
    ): ArrangedAppInfos {
        val selectedList = mutableListOf<AppInfo>()
        val unselectedList = mutableListOf<AppInfo>()
        val validExcludeApps = excludeApps.toMutableList().apply {
            val packageNames = appInfos.map { app -> app.packageName }
            removeAll { packageName -> packageName !in packageNames }
        }
        filterAppInfos(appInfos, query).forEach { info ->
            if (info.packageName in validExcludeApps) {
                selectedList.add(info)
            } else {
                unselectedList.add(info)
            }
        }
        return ArrangedAppInfos(
            excludeApps = validExcludeApps,
            selectedAppInfos = selectedList,
            unselectedAppInfos = unselectedList
        )
    }

    data class UiState(
        val isSearching: Boolean = false,
        val searchQuery: String = "",
        val rawAppInfos: List<AppInfo> = emptyList(),
        val selectedAppInfos: List<AppInfo> = emptyList(),
        val unselectedAppInfos: List<AppInfo> = emptyList(),
        val excludeApps: List<String> = emptyList(),
        val showResetWarningDialog: Boolean = false
    )

    private data class ArrangedAppInfos(
        val excludeApps: List<String>,
        val selectedAppInfos: List<AppInfo>,
        val unselectedAppInfos: List<AppInfo>
    )

    sealed interface UiEvent
}
