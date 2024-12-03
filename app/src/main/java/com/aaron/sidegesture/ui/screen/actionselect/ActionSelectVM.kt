package com.aaron.sidegesture.ui.screen.actionselect

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.Position
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
        selectSingle = !actionSelect.isLongSlide
    )

    init {
        loadData()
    }

    fun select(obj: Any, selected: Boolean) {
        val uiState = uiState
        if (obj is AppInfo) {
            selectAppInfo(obj, selected)
            if (uiState.selectSingle) {
                saveSettings()
            }
        } else if (obj is Action) {
            selectAction(obj, selected)
            if (uiState.selectSingle) {
                saveSettings()
            }
        }
    }

    private fun selectAppInfo(appInfo: AppInfo, selected: Boolean) {
        updateUiState {
            val list = it.selectedItem.apps.toMutableList()
            if (selected) {
                list.add(appInfo)
            } else {
                list.remove(appInfo)
            }
            it.copy(selectedItem = it.selectedItem.copy(apps = list))
        }
    }

    private fun selectAction(action: Action, selected: Boolean) {
        updateUiState {
            val list = it.selectedItem.actions.toMutableList()
            if (selected) {
                list.add(action)
            } else {
                list.remove(action)
            }
            it.copy(selectedItem = it.selectedItem.copy(actions = list))
        }
    }

    fun done() {
        saveSettings()
    }

    fun updateAppInfos() {
        viewModelScope.launch {
            val appInfos = withContext(Dispatchers.IO) {
                AppInfoUtils.getInstalledPackages(App.getContext())
            }
            updateUiState {
                it.copy(apps = appInfos)
            }
        }
    }

    private fun createTitle(): String {
        val context = App.getContext()
        val str1 = when (actionSelect.direction) {
            TriggerDirection.Center -> when (actionSelect.position) {
                Position.Left -> context.getString(R.string.slide_to_right)
                Position.Right -> context.getString(R.string.slide_to_left)
            }
            TriggerDirection.Up -> when (actionSelect.position) {
                Position.Left -> context.getString(R.string.slide_to_top_right)
                Position.Right -> context.getString(R.string.slide_to_top_left)
            }
            TriggerDirection.Down -> when (actionSelect.position) {
                Position.Left -> context.getString(R.string.slide_to_bottom_right)
                Position.Right -> context.getString(R.string.slide_to_bottom_left)
            }
        }
        val str2 = when (actionSelect.isLongSlide) {
            true -> context.getString(R.string.slider_long)
            else -> context.getString(R.string.slider_short)
        }
        return "$str1($str2)"
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureSettings.data
                .combine(DataStoreHolder.gestureButtons.data) { f1, f2 ->
                    f1 to f2
                }
                .collectLatest { (gestureSettings, gestureButtons) ->
                    updateUiState {
                        it.copy(selectSingle = !actionSelect.isLongSlide
                                || !gestureSettings.longSlideTriggerImmediately)
                    }

                    val button = gestureButtons.find {
                        it.id == actionSelect.gestureButtonId && it.position == actionSelect.position
                    }
                    if (button != null) {
                        val actionSelect = actionSelect
                        val gestureActions = when (actionSelect.isLongSlide) {
                            true -> button.longSlideActions
                            else -> button.slideActions
                        }
                        val actions = when (actionSelect.direction) {
                            TriggerDirection.Center -> gestureActions.center
                            TriggerDirection.Up -> gestureActions.up
                            TriggerDirection.Down -> gestureActions.down
                        }
                        updateUiState {
                            val selectedActions = when (it.selectSingle) {
                                true -> actions.take(1)
                                else -> actions
                            }
                            val newSelectedItem = it.selectedItem.copy(actions = selectedActions)
                            it.copy(selectedItem = newSelectedItem)
                        }
                        assembleData()
                    }
                }
        }
    }

    private fun assembleData() {
        updateUiState {
            if (it.selectSingle) {
                return@updateUiState it.copy(actions = GlobalActions.all)
            }
            val globalActions = GlobalActions.allWithoutNone
            val list1 = mutableListOf<Action>()
            val list2 = mutableListOf<Action>()
            globalActions.forEach { action ->
                if (it.selectedItem.isSelected(action) || action == Action.NONE) {
                    list1.add(action)
                } else {
                    list2.add(action)
                }
            }
            val finalList = list1 + list2
            it.copy(actions = finalList)
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.updateData { list ->
                val actionSelect = actionSelect
                var button: GestureButton? = null
                var index = -1
                for (i in list.indices) {
                    index = i
                    val b = list[i]
                    if (b.id == actionSelect.gestureButtonId &&
                        b.position == actionSelect.position
                    ) {
                        button = b
                        break
                    }
                }
                if (button != null) {
                    val selectedItem = uiState.selectedItem
                    val selectedActions = selectedItem.actions
                    val gestureActions = when (actionSelect.isLongSlide) {
                        true -> button.longSlideActions
                        else -> button.slideActions
                    }
                    val newActions = when (uiState.selectSingle) {
                        true -> selectedActions.takeLast(1)
                        else -> selectedActions
                    }
                    val newGestureActions = when (actionSelect.direction) {
                        TriggerDirection.Center -> gestureActions.copy(center = newActions)
                        TriggerDirection.Up -> gestureActions.copy(up = newActions)
                        TriggerDirection.Down -> gestureActions.copy(down = newActions)
                    }
                    button = if (actionSelect.isLongSlide) {
                        button.copy(longSlideActions = newGestureActions)
                    } else {
                        button.copy(slideActions = newGestureActions)
                    }
                    return@updateData list.toMutableList().apply {
                        set(index, button)
                    }
                }
                list
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

    data class UiState(
        val title: String = "",
        val selectSingle: Boolean = true,
        val actions: List<Action> = emptyList(),
        val apps: List<AppInfo> = emptyList(),
        val selectedItem: SelectItem = SelectItem(),
        val needRequestGetAppPermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        data class SelectItem(
            val actions: List<Action> = emptyList(),
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