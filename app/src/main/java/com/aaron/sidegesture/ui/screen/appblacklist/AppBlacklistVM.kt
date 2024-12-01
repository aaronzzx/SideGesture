package com.aaron.sidegesture.ui.screen.appblacklist

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.screen.appblacklist.AppBlacklistVM.UiEvent
import com.aaron.sidegesture.ui.screen.appblacklist.AppBlacklistVM.UiState
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
                getInstalledPackages(App.getContext())
            }
            updateUiState {
                val selectedList = mutableListOf<UiState.AppInfo>()
                val unselectedList = mutableListOf<UiState.AppInfo>()
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

    private fun getInstalledPackages(context: Context): List<UiState.AppInfo> {
        val list = mutableListOf<UiState.AppInfo>()
        val intent = Intent().apply {
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val packageManager = context.packageManager
        val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (info in apps) {
            val item = UiState.AppInfo(
                packageName = info.activityInfo?.packageName ?: "",
                label = info.loadLabel(packageManager).toString(),
                icon = info.loadIcon(packageManager)
            )
            list.add(item)
        }
        return list
    }

    data class UiState(
        val selectedAppInfos: List<AppInfo> = emptyList(),
        val unselectedAppInfos: List<AppInfo> = emptyList(),
        val excludeApps: List<String> = emptyList(),
        val showResetWarningDialog: Boolean = false
    ) {
        data class AppInfo(
            val packageName: String,
            val label: String,
            val icon: Any?
        )
    }

    sealed interface UiEvent
}