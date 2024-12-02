package com.aaron.sidegesture.ui.screen.appblacklist

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AppInfo
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

    fun selectApp(packageName: String, selected: Boolean) {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.updateData {
                val mutableList = it.excludeApps.toMutableList()
                if (selected) {
                    mutableList.add(packageName)
                } else {
                    mutableList.remove(packageName)
                }
                it.copy(excludeApps = mutableList)
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
        viewModelScope.launch {
            val appInfos = withContext(Dispatchers.IO) {
                AppInfoUtils.getInstalledPackages(App.getContext())
            }
            updateUiState {
                val selectedList = mutableListOf<AppInfo>()
                val unselectedList = mutableListOf<AppInfo>()
                appInfos.forEach { info ->
                    if (info.packageName in it.excludeApps) {
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
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.data.collectLatest { item ->
                val excludeApps = item.excludeApps
                updateUiState {
                    it.copy(excludeApps = excludeApps)
                }
            }
        }
    }

    data class UiState(
        val selectedAppInfos: List<AppInfo> = emptyList(),
        val unselectedAppInfos: List<AppInfo> = emptyList(),
        val excludeApps: List<String> = emptyList(),
        val showResetWarningDialog: Boolean = false,
        val needRequestGetAppPermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    )

    sealed interface UiEvent
}