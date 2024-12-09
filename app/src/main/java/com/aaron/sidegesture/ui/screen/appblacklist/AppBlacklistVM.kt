package com.aaron.sidegesture.ui.screen.appblacklist

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.coerceTimeMillis
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.ui.screen.appblacklist.AppBlacklistVM.UiEvent
import com.aaron.sidegesture.ui.screen.appblacklist.AppBlacklistVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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

    fun selectApp(qualifiedName: String, selected: Boolean) {
        updateUiState {
            val mutableList = it.excludeApps.toMutableList()
            if (selected) {
                mutableList.add(qualifiedName)
            } else {
                mutableList.remove(qualifiedName)
            }
            it.copy(excludeApps = mutableList)
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
                        .getInstalledPackages(App.getContext())
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
            DataStoreHolder.advancedSettings.data.collectLatest { item ->
                updateUiState {
                    it.copy(excludeApps = item.excludeApps)
                }
            }
        }
    }

    private fun arrangeAppInfos(appInfos: List<AppInfo>) {
        updateUiState {
            val selectedList = mutableListOf<AppInfo>()
            val unselectedList = mutableListOf<AppInfo>()
            appInfos.forEach { info ->
                if (info.qualifiedName in it.excludeApps) {
                    selectedList.add(info)
                } else {
                    unselectedList.add(info)
                }
            }
            it.copy(
                selectedAppInfos = selectedList,
                unselectedAppInfos = unselectedList
            )
        }
    }

    data class UiState(
        val selectedAppInfos: List<AppInfo> = emptyList(),
        val unselectedAppInfos: List<AppInfo> = emptyList(),
        val excludeApps: List<String> = emptyList(),
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent
}