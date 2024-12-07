package com.aaron.sidegesture.ui.screen.actionselect

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionSelect
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.event.IconResizeEvent
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.subscribeEvent
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.JsonHelper
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

    private val eventHandler = EventHandler()

    init {
        eventHandler.init()
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
            it.copy(selectedRecord = it.selectedRecord.selectAppInfo(appInfo, selected))
        }
    }

    private fun selectAction(action: Action, selected: Boolean) {
        updateUiState {
            it.copy(selectedRecord = it.selectedRecord.selectAction(action, selected))
        }
    }

    fun done() {
        val appInfos = uiState
            .selectedRecord
            .list
            .filterIsInstance<AppInfo>()
        if (appInfos.isNotEmpty()) {
            sendUiEvent(UiEvent.GotoIconResize(appInfos))
        } else {
            saveSettings()
        }
    }

    fun updateAppInfos() {
        viewModelScope.launchWithLoading {
            val appInfos = withContext(Dispatchers.IO) {
                AppInfoUtils.getInstalledPackages(App.getContext())
            }
            updateUiState {
                if (it.selectSingle) {
                    return@updateUiState it.copy(apps = appInfos)
                }
                val list1 = mutableListOf<AppInfo>()
                val list2 = mutableListOf<AppInfo>()
                appInfos.forEach { appInfo ->
                    val cache = it.selectedRecord.getAppInfo(appInfo)
                    if (cache != null) {
                        list1.add(cache)
                    } else {
                        list2.add(appInfo)
                    }
                }
                val finalList = list1 + list2
                it.copy(apps = finalList)
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
                                true -> emptyList()
                                else -> actions
                            }
                            val newSelectedRecord = it.selectedRecord.selectAll(selectedActions)
                            it.copy(selectedRecord = newSelectedRecord)
                        }
                        assembleData()
                    }
                }
        }
    }

    private fun assembleData() {
        updateUiState {
            val allActions = GlobalActions.all
            if (it.selectSingle) {
                return@updateUiState it.copy(actions = allActions)
            }
            val allWithoutNone = allActions.toMutableList().apply { removeAt(0) }
            val list1 = mutableListOf<Action>()
            val list2 = mutableListOf<Action>()
            allWithoutNone.forEach { action ->
                if (it.selectedRecord.isSelected(action) || action == Action.NONE) {
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
                    val selectedRecord = uiState.selectedRecord
                    val selectedList = selectedRecord.list.map { obj ->
                        if (obj !is AppInfo) obj as Action else {
                            val data = JsonHelper.encodeToString(obj)
                            Action(value = GlobalActions.EXTRA_LAUNCH_APP, data = data)
                        }
                    }
                    val newActions = when (uiState.selectSingle) {
                        true -> selectedList.takeLast(1)
                        else -> selectedList
                    }
                    val gestureActions = when (actionSelect.isLongSlide) {
                        true -> button.longSlideActions
                        else -> button.slideActions
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

    private inner class EventHandler {

        fun init() {
            subscribeEvent(IconResizeEvent::class) { event ->
                val resizedAppInfos = event.appInfos
                updateUiState {
                    val selectedList = it.selectedRecord.list.toMutableList()
                    resizedAppInfos.forEach { appInfo ->
                        val index = selectedList.indexOfFirst { obj ->
                            obj is AppInfo &&
                                    obj.packageName == appInfo.packageName &&
                                    obj.className == appInfo.className
                        }
                        if (index != -1) {
                            selectedList[index] = appInfo
                        }
                    }
                    it.copy(selectedRecord = UiState.SelectedRecord(selectedList))
                }
                saveSettings()
            }
        }
    }

    data class UiState(
        val title: String = "",
        val selectSingle: Boolean = true,
        val actions: List<Action> = emptyList(),
        val apps: List<AppInfo> = emptyList(),
        val selectedRecord: SelectedRecord = SelectedRecord(),
        val needRequestGetAppPermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        data class SelectedRecord(val list: List<Any> = emptyList()) {

            val size: Int get() = list.size

            fun selectAll(actions: List<Action>): SelectedRecord {
                val newList = list.toMutableList().apply {
                    actions.forEach { action ->
                        val appInfo = action.appInfo
                        if (appInfo != null) {
                            add(appInfo)
                        } else {
                            add(action)
                        }
                    }
                }
                return this.copy(list = newList)
            }

            fun selectAction(action: Action, selected: Boolean): SelectedRecord {
                val newList = list.toMutableList().apply {
                    if (selected) {
                        add(action)
                    } else {
                        remove(action)
                    }
                }
                return this.copy(list = newList)
            }

            fun selectAppInfo(app: AppInfo, selected: Boolean): SelectedRecord {
                val newList = list.toMutableList().apply {
                    if (selected) {
                        add(app)
                    } else {
                        remove(app)
                    }
                }
                return this.copy(list = newList)
            }

            fun isSelected(obj: Any): Boolean {
                if (obj is AppInfo) {
                    return list.find {
                        it is AppInfo &&
                                it.packageName == obj.packageName &&
                                it.className == obj.className
                    } != null
                }
                return obj in list
            }

            fun getAppInfo(appInfo: AppInfo): AppInfo? {
                return list.find {
                    it is AppInfo &&
                            it.packageName == appInfo.packageName &&
                            it.className == appInfo.className
                } as? AppInfo
            }
        }
    }

    sealed interface UiEvent {
        data class GotoIconResize(val appInfos: List<AppInfo>) : UiEvent
    }
}