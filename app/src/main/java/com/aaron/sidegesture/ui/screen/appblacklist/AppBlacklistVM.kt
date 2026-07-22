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
import com.aaron.sidegesture.utils.PinyinSearchUtils
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
            it.copy(
                isSearching = false,
                appList = it.appList.updateSearchQuery("")
            )
        }
    }

    fun updateSearchQuery(query: String) {
        updateUiState {
            it.copy(appList = it.appList.updateSearchQuery(query))
        }
    }

    fun selectApp(appInfo: AppInfo, selected: Boolean) {
        updateUiState {
            it.copy(
                appList = it.appList.selectApp(
                    packageName = appInfo.packageName,
                    selected = selected
                )
            )
        }
    }

    fun done() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                it.copy(excludeApps = uiState.appList.excludeApps)
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
                    uiState.copy(appList = uiState.appList.clearSelection())
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
                        .let { appInfos -> PinyinSearchUtils.sortAppInfos(appInfos) }
                }
            }
            updateUiState {
                it.copy(appList = it.appList.loadAppInfos(appInfos))
            }
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
                        it.copy(appList = it.appList.loadSavedSelection(item.excludeApps))
                    }
                }
        }
    }

    data class UiState(
        val isSearching: Boolean = false,
        val appList: AppBlacklistListState = AppBlacklistListState(),
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent
}
