package com.aaron.sidegesture.ui.screen.actionselect

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.whenPosition
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */
class ActionSelectVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    private val actionSelect = savedStateHandle.toRoute<ActionSelect>()

    override val initialState: UiState = UiState(
        title = createTitle(),
        selectSingle = !actionSelect.isLongSlide,
        actions = when (actionSelect.isLongSlide) {
            true -> GlobalActions.allWithoutNone
            else -> GlobalActions.all
        }
    )

    fun select(obj: Any, selected: Boolean) {
        if (obj is AppInfo) {
            updateUiState {
                val list = it.selectedItem.apps.toMutableList()
                if (selected) {
                    list.add(obj)
                } else {
                    list.remove(obj)
                }
                it.copy(
                    selectedItem = it.selectedItem.copy(apps = list)
                )
            }
        } else if (obj is String) {
            updateUiState {
                val list = it.selectedItem.actions.toMutableList()
                if (selected) {
                    list.add(obj)
                } else {
                    list.remove(obj)
                }
                it.copy(
                    selectedItem = it.selectedItem.copy(actions = list)
                )
            }
        }
    }

    fun done() {

    }

    fun updateAppInfos() {
        viewModelScope.launch {
            val appInfos = withContext(Dispatchers.IO) {
                AppInfoUtils.getInstalledPackages(App.getContext())
            }
            updateUiState {
                val thirdApps = mutableListOf<AppInfo>()
                val systemApps = mutableListOf<AppInfo>()
                appInfos.forEach { info ->
                    if (info.isUserApp) {
                        thirdApps.add(info)
                    } else {
                        systemApps.add(info)
                    }
                }
                it.copy(
                    systemApps = systemApps,
                    userApps = thirdApps
                )
            }
        }
    }

    private fun createTitle(): String {
        val context = App.getContext()
        val str1 = when (actionSelect.direction) {
            TriggerDirection.Center -> whenPosition(
                onLeft = { context.getString(R.string.slide_to_right) },
                onRight = { context.getString(R.string.slide_to_left) },
                position = actionSelect.position
            )
            TriggerDirection.Up -> whenPosition(
                onLeft = { context.getString(R.string.slide_to_top_right) },
                onRight = { context.getString(R.string.slide_to_top_left) },
                position = actionSelect.position
            )
            TriggerDirection.Down -> whenPosition(
                onLeft = { context.getString(R.string.slide_to_bottom_right) },
                onRight = { context.getString(R.string.slide_to_bottom_left) },
                position = actionSelect.position
            )
        }
        val str2 = when (actionSelect.isLongSlide) {
            true -> context.getString(R.string.slider_long)
            else -> context.getString(R.string.slider_short)
        }
        return "$str1($str2)"
    }

    data class UiState(
        val title: String = "",
        val selectSingle: Boolean = true,
        val actions: List<String> = emptyList(),
        val userApps: List<AppInfo> = emptyList(),
        val systemApps: List<AppInfo> = emptyList(),
        val selectedItem: SelectItem = SelectItem(),
        val needRequestGetAppPermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        data class SelectItem(
            val actions: List<String> = emptyList(),
            val apps: List<AppInfo> = emptyList()
        ) {
            val size: Int get() = actions.size + apps.size

            fun isSelected(obj: Any): Boolean {
                if (obj is AppInfo) {
                    return obj in apps
                }
                return obj in actions
            }
        }
    }

    sealed interface UiEvent
}