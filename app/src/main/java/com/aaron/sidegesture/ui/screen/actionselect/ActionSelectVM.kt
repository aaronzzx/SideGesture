package com.aaron.sidegesture.ui.screen.actionselect

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.Paths
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionSelect
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.IconResize
import com.aaron.sidegesture.entity.LauncherInfo
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.QuickLauncherActionData
import com.aaron.sidegesture.entity.ShellCommandActionData
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.event.IconResizeEvent
import com.aaron.sidegesture.event.QuickLauncherConfigEvent
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.coerceTimeMillis
import com.aaron.sidegesture.ktx.getIcon
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.ktx.qualifiedNameWithIntents
import com.aaron.sidegesture.ktx.quickLauncherActionData
import com.aaron.sidegesture.ktx.shellCommandActionData
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ktx.subscribeEvent
import com.aaron.sidegesture.shizuku.ShellResult
import com.aaron.sidegesture.shizuku.ShizukuShellManager
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.Events
import com.aaron.sidegesture.utils.JsonHelper
import com.aaron.sidegesture.utils.PinyinSearchUtils
import com.aaron.sidegesture.utils.ShellActionExecutor
import com.aaron.sidegesture.utils.ShortcutUtils
import com.blankj.utilcode.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */
class ActionSelectVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    companion object {
        var pendingQuickLauncherAction: Action? = null
    }

    private val actionSelect = savedStateHandle.toRoute<ActionSelect>()

    override val initialState: UiState = UiState(
        title = createTitle(),
        selectSingle = !actionSelect.isLongSlide,
        isQuickLauncher = actionSelect.isQuickLauncher
    )

    private val eventHandler = EventHandler()

    val actionSettingsDialog = ActionSettingsDialog()
    val shellActionDialog = ShellActionDialog()

    init {
        eventHandler.init()
        observeShizukuStatus()
        loadData()
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

    fun addNewShortcut(launcherInfo: LauncherInfo, shortcutInfo: LauncherInfo.ShortcutInfo) {
        updateUiState {
            val index = it.rawCreateShortcuts.indexOfFirst { info ->
                info.qualifiedName == launcherInfo.qualifiedName
            }
            if (index < 0) {
                return@updateUiState it
            }
            val newList = it.rawCreateShortcuts.toMutableList().apply {
                val cache = it.rawCreateShortcuts[index]
                set(index, cache.copy(shortcuts = cache.shortcuts + shortcutInfo))
            }
            applySearchResult(it.copy(rawCreateShortcuts = newList))
        }
    }

    fun clearSelected() {
        updateUiState {
            it.copy(selectedRecord = it.selectedRecord.clear())
        }
    }

    fun select(obj: Any, selected: Boolean) {
        val uiState = uiState
        if (obj is AppInfo) {
            selectAppInfo(obj, selected)
            if (uiState.selectSingle) {
                saveSettings()
            }
        } else if (obj is LauncherInfo.ShortcutInfo) {
            selectShortcutInfo(obj, selected)
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

    fun reorder(newList: List<Any>) {
        updateUiState {
            it.copy(
                selectedRecord = it.selectedRecord.copy(list = newList)
            )
        }
    }

    fun toggleMiniWindow(appInfo: AppInfo) {
        val switchToMiniWindow = !appInfo.miniWindow
        updateUiState {
            val updatedApp = appInfo.copy(miniWindow = switchToMiniWindow)
            val newRawApps = it.rawApps.map { item ->
                if (item.qualifiedName == appInfo.qualifiedName) {
                    updatedApp
                } else {
                    item
                }
            }
            val newSelectedList = it.selectedRecord.list.map { item ->
                if (item is AppInfo && item.qualifiedName == appInfo.qualifiedName) {
                    updatedApp
                } else {
                    item
                }
            }
            applySearchResult(
                it.copy(
                    rawApps = newRawApps,
                    selectedRecord = it.selectedRecord.copy(list = newSelectedList)
                )
            )
        }
        if (switchToMiniWindow) {
            toast(R.string.enable_mini_window)
        } else {
            toast(R.string.disable_mini_window)
        }
    }

    fun toggleMiniWindow(shortcutInfo: LauncherInfo.ShortcutInfo) {
        val switchToMiniWindow = !shortcutInfo.miniWindow
        updateUiState {
            val updatedShortcut = shortcutInfo.copy(miniWindow = switchToMiniWindow)
            val newRawCreateShortcuts = it.rawCreateShortcuts.replaceShortcutInfo(updatedShortcut)
            val newRawLaunchShortcuts = it.rawLaunchShortcuts.replaceShortcutInfo(updatedShortcut)
            val newSelectedList = it.selectedRecord.list.map { item ->
                if (item is LauncherInfo.ShortcutInfo &&
                    item.qualifiedNameWithIntents == shortcutInfo.qualifiedNameWithIntents
                ) {
                    updatedShortcut
                } else {
                    item
                }
            }
            applySearchResult(
                it.copy(
                    rawCreateShortcuts = newRawCreateShortcuts,
                    rawLaunchShortcuts = newRawLaunchShortcuts,
                    selectedRecord = it.selectedRecord.copy(list = newSelectedList)
                )
            )
        }
        if (switchToMiniWindow) {
            toast(R.string.enable_mini_window)
        } else {
            toast(R.string.disable_mini_window)
        }
    }

    private fun List<LauncherInfo>.replaceShortcutInfo(
        updatedShortcut: LauncherInfo.ShortcutInfo
    ): List<LauncherInfo> {
        return map { launcherInfo ->
            launcherInfo.copy(
                shortcuts = launcherInfo.shortcuts.map { shortcutInfo ->
                    if (shortcutInfo.qualifiedNameWithIntents == updatedShortcut.qualifiedNameWithIntents) {
                        updatedShortcut
                    } else {
                        shortcutInfo
                    }
                }
            )
        }
    }

    private fun applySearchResult(uiState: UiState): UiState {
        return uiState.copy(
            actions = filterActions(uiState.rawActions, uiState.searchQuery),
            apps = filterAppInfos(uiState.rawApps, uiState.searchQuery),
            createShortcuts = filterLauncherInfos(uiState.rawCreateShortcuts, uiState.searchQuery),
            launchShortcuts = filterLauncherInfos(uiState.rawLaunchShortcuts, uiState.searchQuery)
        )
    }

    private fun filterActions(actions: List<Action>, query: String): List<Action> {
        if (query.isBlank()) return actions
        val context = App.getContext()
        return actions.filter { action ->
            PinyinSearchUtils.matches(
                query = query,
                label = context.actionText(action, emptyIfNone = false)
            )
        }
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

    private fun filterLauncherInfos(
        launcherInfos: List<LauncherInfo>,
        query: String
    ): List<LauncherInfo> {
        if (query.isBlank()) return launcherInfos
        return launcherInfos.mapNotNull { launcherInfo ->
            val filteredShortcuts = launcherInfo.shortcuts.filter { shortcutInfo ->
                PinyinSearchUtils.matches(
                    query = query,
                    label = shortcutInfo.label,
                    packageName = shortcutInfo.packageName
                )
            }
            val launcherMatched = PinyinSearchUtils.matches(
                query = query,
                label = launcherInfo.label,
                packageName = launcherInfo.packageName
            )
            if (!launcherMatched && filteredShortcuts.isEmpty()) {
                null
            } else {
                launcherInfo.copy(shortcuts = filteredShortcuts)
            }
        }
    }

    private fun selectShortcutInfo(shortcutInfo: LauncherInfo.ShortcutInfo, selected: Boolean) {
        updateUiState {
            it.copy(selectedRecord = it.selectedRecord.selectShortcutInfo(shortcutInfo, selected))
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

    fun updateShellCommand(command: String) {
        updateUiState {
            it.copy(shellActionDialog = it.shellActionDialog.copy(command = command))
        }
    }

    fun testShellCommand() {
        val command = uiState.shellActionDialog.command
        if (command.isBlank()) {
            toast(R.string.shell_command_empty)
            return
        }
        viewModelScope.launch {
            updateUiState {
                it.copy(
                    shellActionDialog = it.shellActionDialog.copy(
                        isTesting = true,
                        testResult = null
                    )
                )
            }
            val result = ShellActionExecutor.execute(command)
            updateUiState {
                it.copy(
                    shellActionDialog = it.shellActionDialog.copy(
                        isTesting = false,
                        testResult = result
                    )
                )
            }
        }
    }

    fun saveShellAction() {
        val command = uiState.shellActionDialog.command
        if (command.isBlank()) {
            toast(R.string.shell_command_empty)
            return
        }
        val action = Action(
            value = GlobalActions.SHIZUKU_SHELL,
            data = JsonHelper.encodeToString(ShellCommandActionData(command = command))
        )
        updateUiState {
            it.copy(
                selectedRecord = it.selectedRecord.upsertAction(action),
                shellActionDialog = UiState.ShellActionDialogValue()
            )
        }
        assembleData()
        if (uiState.selectSingle) {
            saveSettings()
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            val status = ShizukuShellManager.currentStatus()
            when {
                !status.installed -> toast(R.string.shizuku_not_installed_hint)
                !status.binderAlive -> toast(R.string.shizuku_not_running_hint)
                status.permissionGranted -> toast(R.string.shizuku_status_ready)
                ShizukuShellManager.requestPermission() -> Unit
                else -> toast(R.string.shizuku_permission_required)
            }
        }
    }

    private fun observeShizukuStatus() {
        viewModelScope.launch {
            ShizukuShellManager.statusFlow.collectLatest { status ->
                updateUiState {
                    it.copy(
                        shellActionDialog = it.shellActionDialog.copy(status = status)
                    )
                }
            }
        }
    }

    fun getQuickLauncherRoute(): ActionSelect {
        pendingQuickLauncherAction = uiState.selectedRecord.findAction(GlobalActions.QUICK_LAUNCHER)
        return actionSelect.copy(isQuickLauncher = true)
    }

    fun done() {
        if (actionSelect.isQuickLauncher) {
            saveQuickLauncherSettings()
            return
        }
        val appInfos = uiState
            .selectedRecord
            .list
            .filterIsInstance<AppInfo>()
        val shortcutInfos = uiState
            .selectedRecord
            .list
            .filterIsInstance<LauncherInfo.ShortcutInfo>()
        if (appInfos.isNotEmpty() || shortcutInfos.isNotEmpty()) {
            val ids = mutableListOf<String>()
            appInfos.forEach { appInfo ->
                val icon = appInfo.getIcon(App.getContext()) ?: return@forEach
                ids.add(appInfo.qualifiedName)
                IconResize.iconCache[appInfo.qualifiedName] = icon
                IconResize.iconBgColorCache[appInfo.qualifiedName] = appInfo.iconBgColor
            }
            shortcutInfos.forEach { shortcutInfo ->
                val icon = shortcutInfo.getIcon(App.getContext()) ?: return@forEach
                ids.add(shortcutInfo.qualifiedNameWithIntents)
                IconResize.iconCache[shortcutInfo.qualifiedNameWithIntents] = icon
                IconResize.iconBgColorCache[shortcutInfo.qualifiedNameWithIntents] = shortcutInfo.iconBgColor
            }

            sendUiEvent(UiEvent.GotoIconResize(IconResize(ids)))
        } else {
            saveSettings()
        }
    }

    private fun LauncherInfo.withSelectedShortcutInfoCache(
        selectedShortcutInfos: List<LauncherInfo.ShortcutInfo>
    ): LauncherInfo {
        return copy(
            shortcuts = shortcuts.map { shortcutInfo ->
                val cache = selectedShortcutInfos.find { selected ->
                    selected.qualifiedNameWithIntents == shortcutInfo.qualifiedNameWithIntents
                } ?: return@map shortcutInfo
                shortcutInfo.copy(
                    iconPath = cache.iconPath,
                    iconScale = cache.iconScale,
                    miniWindow = cache.miniWindow,
                    iconBgColor = cache.iconBgColor
                )
            }
        )
    }

    fun updateShortcutInfos() {
        viewModelScope.launchWithLoading {
            val createLauncherInfos = withContext(Dispatchers.IO) {
                coerceTimeMillis(250) {
                    PinyinSearchUtils.sortLauncherInfos(
                        AppInfoUtils.queryCreateShortcutActivities(App.getContext())
                    )
                }
            }
            val launchLauncherInfos = withContext(Dispatchers.IO) {
                coerceTimeMillis(250) {
                    PinyinSearchUtils.sortLauncherInfos(
                        ShortcutUtils.getAllAppsWithShortcut(App.getContext())
                    )
                }
            }
            if (uiState.selectSingle) {
                updateUiState {
                    applySearchResult(
                        it.copy(
                            rawCreateShortcuts = createLauncherInfos,
                            rawLaunchShortcuts = launchLauncherInfos
                        )
                    )
                }
                return@launchWithLoading
            }
            val selectedRecord = withContext(Dispatchers.Default) {
                uiState.selectedRecord.let { selectedRecord ->
                    // 检查是否有被卸载的，然后从选中中移除
                    val uninstalledList = mutableListOf<LauncherInfo.ShortcutInfo>()
                    selectedRecord
                        .list
                        .filterIsInstance<LauncherInfo.ShortcutInfo>()
                        .forEach { selected ->
                            val uninstalled = !createLauncherInfos.any { launcher ->
                                launcher.qualifiedName == selected.qualifiedName
                            } && !launchLauncherInfos.any { launcher ->
                                launcher.shortcuts.any { shortcut ->
                                    shortcut.qualifiedNameWithIntents == selected.qualifiedNameWithIntents
                                }
                            }
                            if (uninstalled) {
                                uninstalledList.add(selected)
                            }
                        }
                    selectedRecord.removeAllShortcutInfos(uninstalledList)
                }
            }
            val finalCreateList = withContext(Dispatchers.Default) {
                val list1 = mutableListOf<LauncherInfo>()
                val list2 = mutableListOf<LauncherInfo>()
                val selectedShortcutInfos = selectedRecord.list.filterIsInstance<LauncherInfo.ShortcutInfo>()
                createLauncherInfos.forEach { launcherInfo ->
                    val cache = selectedShortcutInfos.find { info ->
                        info.packageName == launcherInfo.packageName
                    }
                    if (cache != null) {
                        list1.add(launcherInfo.withSelectedShortcutInfoCache(selectedShortcutInfos))
                    } else {
                        list2.add(launcherInfo)
                    }
                }
                PinyinSearchUtils.sortLauncherInfos(list1) + PinyinSearchUtils.sortLauncherInfos(list2)
            }
            val finalLaunchList = withContext(Dispatchers.Default) {
                val list1 = mutableListOf<LauncherInfo>()
                val list2 = mutableListOf<LauncherInfo>()
                val selectedShortcutInfos = selectedRecord.list.filterIsInstance<LauncherInfo.ShortcutInfo>()
                launchLauncherInfos.forEach { launcherInfo ->
                    val cache = selectedShortcutInfos.find { info ->
                        info.packageName == launcherInfo.packageName
                    }
                    if (cache != null) {
                        list1.add(launcherInfo.withSelectedShortcutInfoCache(selectedShortcutInfos))
                    } else {
                        list2.add(launcherInfo)
                    }
                }
                PinyinSearchUtils.sortLauncherInfos(list1) + PinyinSearchUtils.sortLauncherInfos(list2)
            }
            updateUiState {
                applySearchResult(
                    it.copy(
                        rawCreateShortcuts = finalCreateList,
                        rawLaunchShortcuts = finalLaunchList,
                        selectedRecord = selectedRecord
                    )
                )
            }
            uiState
                .selectedRecord
                .list
                .filterIsInstance<LauncherInfo.ShortcutInfo>()
                .forEach { shortcut ->
                    val launcherInfo = createLauncherInfos.find {
                        it.qualifiedName == shortcut.qualifiedName
                    }
                    if (launcherInfo != null) {
                        addNewShortcut(launcherInfo, shortcut)
                    }
                }
        }
    }

    fun updateAppInfos() {
        viewModelScope.launchWithLoading {
            val appInfos = withContext(Dispatchers.IO) {
                coerceTimeMillis(500) {
                    PinyinSearchUtils.sortAppInfos(
                        AppInfoUtils.queryLauncherActivities(App.getContext())
                    )
                }
            }
            if (uiState.selectSingle) {
                updateUiState {
                    applySearchResult(it.copy(rawApps = appInfos))
                }
                return@launchWithLoading
            }
            val selectedRecord = withContext(Dispatchers.Default) {
                uiState.selectedRecord.let { selectedRecord ->
                    // 检查是否有被卸载的，然后从选中中移除
                    val uninstalledList = mutableListOf<AppInfo>()
                    selectedRecord
                        .list
                        .filterIsInstance<AppInfo>()
                        .forEach { selectedApp ->
                            val uninstalled = !appInfos.any { app ->
                                selectedApp.qualifiedName == app.qualifiedName
                            }
                            if (uninstalled) {
                                uninstalledList.add(selectedApp)
                            }
                        }
                    selectedRecord.removeAllAppInfos(uninstalledList)
                }
            }
            val finalList = withContext(Dispatchers.Default) {
                val list1 = mutableListOf<AppInfo>()
                val list2 = mutableListOf<AppInfo>()
                val selectedAppInfos = selectedRecord.list.filterIsInstance<AppInfo>()
                appInfos.forEach { appInfo ->
                    val cache = selectedAppInfos.find { app ->
                        app.qualifiedName == appInfo.qualifiedName
                    }
                    if (cache != null) {
                        val appInfo2 = appInfo.copy(
                            iconScale = cache.iconScale,
                            miniWindow = cache.miniWindow
                        )
                        list1.add(appInfo2)
                    } else {
                        list2.add(appInfo)
                    }
                }
                PinyinSearchUtils.sortAppInfos(list1) + PinyinSearchUtils.sortAppInfos(list2)
            }
            updateUiState {
                applySearchResult(
                    it.copy(
                        rawApps = finalList,
                        selectedRecord = selectedRecord
                    )
                )
            }
        }
    }

    private fun createTitle(): String {
        val context = App.getContext()
        if (actionSelect.isQuickLauncher) {
            return context.getString(R.string.action_quick_launcher)
        }
        val actionSelect = actionSelect
        val str1 = when (actionSelect.direction) {
            TriggerDirection.Center -> when (actionSelect.position) {
                Position.Left -> context.getString(R.string.slide_to_right)
                Position.Right -> context.getString(R.string.slide_to_left)
                Position.Bottom -> context.getString(R.string.slide_to_top)
            }
            TriggerDirection.Up -> when (actionSelect.position) {
                Position.Left -> context.getString(R.string.slide_to_top_right)
                Position.Right -> context.getString(R.string.slide_to_top_left)
                Position.Bottom -> context.getString(R.string.slide_to_top_left)
            }
            TriggerDirection.Down -> when (actionSelect.position) {
                Position.Left -> context.getString(R.string.slide_to_bottom_right)
                Position.Right -> context.getString(R.string.slide_to_bottom_left)
                Position.Bottom -> context.getString(R.string.slide_to_top_right)
            }
            TriggerDirection.Center2 -> context.getString(R.string.long_press)
            TriggerDirection.Up2 -> when (actionSelect.position) {
                Position.Left, Position.Right -> context.getString(R.string.slide_to_top)
                Position.Bottom -> context.getString(R.string.slide_to_left)
            }
            TriggerDirection.Down2 -> when (actionSelect.position) {
                Position.Left, Position.Right -> context.getString(R.string.slide_to_bottom)
                Position.Bottom -> context.getString(R.string.slide_to_right)
            }
        }
        if (actionSelect.direction == TriggerDirection.Center2) {
            return str1
        }
        val str2 = when (actionSelect.isLongSlide) {
            true -> context.getString(R.string.long1)
            else -> context.getString(R.string.short1)
        }
        return "$str1($str2)"
    }

    private fun loadData() {
        viewModelScope.launch {
            val buttons = if (actionSelect.isSideButton) {
                DataStoreHolder.sideGestureButtons
            } else {
                DataStoreHolder.bottomGestureButtons
            }
            DataStoreHolder
                .gestureSettings
                .data
                .combine(buttons.data) { f1, f2 ->
                    f1 to f2
                }
                .take(1)
                .collectLatest { (gestureSettings, gestureButtons) ->
                    updateUiState {
                        val selectSingle = if (actionSelect.isQuickLauncher) {
                            false
                        } else {
                            !actionSelect.isLongSlide || !gestureSettings.longSlideTriggerImmediately
                        }
                        it.copy(selectSingle = selectSingle)
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
                            TriggerDirection.Center2 -> gestureActions.center2
                            TriggerDirection.Up2 -> gestureActions.up2
                            TriggerDirection.Down2 -> gestureActions.down2
                        }
                        updateUiState {
                            if (actionSelect.isQuickLauncher) {
                                val pending = pendingQuickLauncherAction
                                pendingQuickLauncherAction = null
                                val quickLauncherAction = pending
                                    ?: actions.find { a ->
                                        a.value == GlobalActions.QUICK_LAUNCHER
                                    }
                                val quickLauncherData = quickLauncherAction
                                    ?.let { a ->
                                        runCatching {
                                            JsonHelper.decodeFromString<QuickLauncherActionData>(a.data)
                                        }.getOrNull()
                                    }
                                val selectedActions = quickLauncherData?.items ?: emptyList()
                                val newSelectedRecord = it.selectedRecord.selectAll(selectedActions)
                                it.copy(selectedRecord = newSelectedRecord)
                            } else {
                                val newSelectedRecord = it.selectedRecord.selectAll(actions)
                                it.copy(selectedRecord = newSelectedRecord)
                            }
                        }
                        assembleData()
                    }
                }
        }
    }

    private fun assembleData() {
        updateUiState {
            val allActions = GlobalActions.all.toMutableList().apply {
                if (it.selectSingle) {
                    removeAll { action ->
                        action.value == GlobalActions.MOVE_SCREEN
                    }
                }
            }
            if (it.selectSingle) {
                return@updateUiState applySearchResult(it.copy(rawActions = allActions))
            }
            val allWithoutNone = allActions.apply { removeAt(0) }
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
            applySearchResult(it.copy(rawActions = finalList))
        }
    }

    private fun saveQuickLauncherSettings() {
        val selectedList = uiState.selectedRecord.list
        val items = selectedList.map { obj ->
            when (obj) {
                is AppInfo -> {
                    val data = JsonHelper.encodeToString(obj)
                    Action(value = GlobalActions.EXTRA_LAUNCH_APP, data = data)
                }
                is LauncherInfo.ShortcutInfo -> {
                    val data = JsonHelper.encodeToString(obj)
                    Action(value = GlobalActions.EXTRA_LAUNCH_SHORTCUT, data = data)
                }
                else -> obj as Action
            }
        }
        val quickLauncherData = QuickLauncherActionData(items = items)
        val quickLauncherAction = Action(
            value = GlobalActions.QUICK_LAUNCHER,
            data = JsonHelper.encodeToString(quickLauncherData)
        )
        Events.post(QuickLauncherConfigEvent(quickLauncherAction))
        finish()
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val buttons = if (actionSelect.isSideButton) {
                DataStoreHolder.sideGestureButtons
            } else {
                DataStoreHolder.bottomGestureButtons
            }
            buttons.updateData { list ->
                val mutableList = list.toMutableList()
                val actionSelect = actionSelect
                var button: GestureButton? = null
                var index = -1
                for (i in mutableList.indices) {
                    index = i
                    val b = mutableList[i]
                    if (b.id == actionSelect.gestureButtonId &&
                        b.position == actionSelect.position
                    ) {
                        button = b
                        break
                    }
                }
                if (button == null) {
                    return@updateData mutableList
                }
                val selectedRecord = uiState.selectedRecord
                val selectedList = selectedRecord.list.map { obj ->
                    when (obj) {
                        is AppInfo -> {
                            val data = JsonHelper.encodeToString(obj)
                            Action(value = GlobalActions.EXTRA_LAUNCH_APP, data = data)
                        }
                        is LauncherInfo.ShortcutInfo -> {
                            val data = JsonHelper.encodeToString(obj)
                            Action(value = GlobalActions.EXTRA_LAUNCH_SHORTCUT, data = data)
                        }
                        else -> {
                            obj as Action
                        }
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
                fun tryDeleteShortcutIcons(old: List<Action>, new: List<Action>) {
                    old.forEach { action ->
                        val shortcutInfo = action.shortcutInfo ?: return@forEach
                        if (shortcutInfo.iconPath.isNullOrEmpty()) return@forEach
                        if (new.any { it.shortcutInfo?.iconPath == shortcutInfo.iconPath }) return@forEach
                        FileUtils.delete(shortcutInfo.iconPath)
                    }
                }
                val newGestureActions = when (actionSelect.direction) {
                    TriggerDirection.Center -> {
                        val oldActions = gestureActions.center
                        tryDeleteShortcutIcons(oldActions, newActions)
                        gestureActions.copy(center = newActions)
                    }
                    TriggerDirection.Up -> {
                        val oldActions = gestureActions.up
                        tryDeleteShortcutIcons(oldActions, newActions)
                        gestureActions.copy(up = newActions)
                    }
                    TriggerDirection.Down -> {
                        val oldActions = gestureActions.down
                        tryDeleteShortcutIcons(oldActions, newActions)
                        gestureActions.copy(down = newActions)
                    }
                    TriggerDirection.Center2 -> {
                        val oldActions = gestureActions.center2
                        tryDeleteShortcutIcons(oldActions, newActions)
                        gestureActions.copy(center2 = newActions)
                    }
                    TriggerDirection.Up2 -> {
                        val oldActions = gestureActions.up2
                        tryDeleteShortcutIcons(oldActions, newActions)
                        gestureActions.copy(up2 = newActions)
                    }
                    TriggerDirection.Down2 -> {
                        val oldActions = gestureActions.down2
                        tryDeleteShortcutIcons(oldActions, newActions)
                        gestureActions.copy(down2 = newActions)
                    }
                }
                button = if (actionSelect.isLongSlide) {
                    button.copy(longSlideActions = newGestureActions)
                } else {
                    button.copy(slideActions = newGestureActions)
                }
                mutableList.apply {
                    set(index, button)
                }
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
            subscribeEvent(QuickLauncherConfigEvent::class) { event ->
                if (actionSelect.isQuickLauncher) return@subscribeEvent
                val hasItems = event.quickLauncherAction.quickLauncherActionData
                    ?.items?.isNotEmpty() == true
                updateUiState {
                    val newList = it.selectedRecord.list.toMutableList()
                    val existingIndex = newList.indexOfFirst { obj ->
                        obj is Action && obj.value == GlobalActions.QUICK_LAUNCHER
                    }
                    if (hasItems) {
                        if (existingIndex != -1) {
                            newList[existingIndex] = event.quickLauncherAction
                        } else {
                            newList.add(event.quickLauncherAction)
                        }
                    } else {
                        if (existingIndex != -1) {
                            newList.removeAt(existingIndex)
                        }
                    }
                    it.copy(selectedRecord = UiState.SelectedRecord(newList))
                }
                if (uiState.selectSingle) {
                    saveSettings()
                }
            }
            subscribeEvent(IconResizeEvent::class) { event ->
                val scaleFactors = event.scaleFactors
                val bgColors = event.bgColors
                updateUiState {
                    val selectedList = it.selectedRecord.list.toMutableList()
                    scaleFactors.forEach { (id, scaleFactor) ->
                        val index = selectedList.indexOfFirst { obj ->
                            obj is AppInfo && obj.qualifiedName == id
                        }
                        if (index != -1) {
                            val old = selectedList[index] as AppInfo
                            selectedList[index] = old.copy(iconScale = scaleFactor)
                            return@forEach
                        }
                        val index2 = selectedList.indexOfFirst { obj ->
                            obj is LauncherInfo.ShortcutInfo && obj.qualifiedNameWithIntents == id
                        }
                        if (index2 != -1) {
                            val old = selectedList[index2] as LauncherInfo.ShortcutInfo
                            selectedList[index2] = old.copy(iconScale = scaleFactor)
                        }
                    }
                    bgColors.forEach { (id, color) ->
                        val index = selectedList.indexOfFirst { obj ->
                            obj is AppInfo && obj.qualifiedName == id
                        }
                        if (index != -1) {
                            val old = selectedList[index] as AppInfo
                            selectedList[index] = old.copy(iconBgColor = color)
                            return@forEach
                        }
                        val index2 = selectedList.indexOfFirst { obj ->
                            obj is LauncherInfo.ShortcutInfo && obj.qualifiedNameWithIntents == id
                        }
                        if (index2 != -1) {
                            val old = selectedList[index2] as LauncherInfo.ShortcutInfo
                            selectedList[index2] = old.copy(iconBgColor = color)
                        }
                    }

                    // 保存Bitmap到本地
                    val shortcutInfos = mutableMapOf<Int, LauncherInfo.ShortcutInfo>()
                    selectedList.forEachIndexed { index, obj ->
                        if (obj !is LauncherInfo.ShortcutInfo) return@forEachIndexed
                        val iconBitmap = obj.iconBitmap ?: return@forEachIndexed
                        val iconPath = "${Paths.Image}/${System.currentTimeMillis()}"
                        val fos = FileOutputStream(iconPath)
                        iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        shortcutInfos[index] = obj.copy(iconPath = iconPath)
                    }
                    shortcutInfos.forEach { (index, shortcutInfo) ->
                        selectedList[index] = shortcutInfo
                    }

                    it.copy(selectedRecord = UiState.SelectedRecord(selectedList))
                }
                saveSettings()
            }
        }
    }

    inner class ActionSettingsDialog {

        fun show(show: Boolean, action: Action = Action.NONE) {
            updateUiState {
                it.copy(actionSettingsDialog = it.actionSettingsDialog.copy(show = show, action = action))
            }
        }
    }

    inner class ShellActionDialog {

        fun show(show: Boolean, action: Action = Action(value = GlobalActions.SHIZUKU_SHELL)) {
            updateUiState {
                if (!show) {
                    return@updateUiState it.copy(shellActionDialog = UiState.ShellActionDialogValue())
                }
                val selectedAction = it.selectedRecord.findAction(action.value) ?: action
                it.copy(
                    shellActionDialog = UiState.ShellActionDialogValue(
                        show = true,
                        action = selectedAction,
                        command = selectedAction.shellCommandActionData?.command.orEmpty(),
                        status = ShizukuShellManager.currentStatus()
                    )
                )
            }
        }
    }

    data class UiState(
        val title: String = "",
        val selectSingle: Boolean = true,
        val isQuickLauncher: Boolean = false,
        val isSearching: Boolean = false,
        val searchQuery: String = "",
        val rawActions: List<Action> = emptyList(),
        val rawApps: List<AppInfo> = emptyList(),
        val rawCreateShortcuts: List<LauncherInfo> = emptyList(),
        val rawLaunchShortcuts: List<LauncherInfo> = emptyList(),
        val actions: List<Action> = emptyList(),
        val apps: List<AppInfo> = emptyList(),
        val createShortcuts: List<LauncherInfo> = emptyList(),
        val launchShortcuts: List<LauncherInfo> = emptyList(),
        val selectedRecord: SelectedRecord = SelectedRecord(),
        val actionSettingsDialog: ActionSettingsDialogValue = ActionSettingsDialogValue(false, Action.NONE),
        val shellActionDialog: ShellActionDialogValue = ShellActionDialogValue(),
    ) {
        data class SelectedRecord(val list: List<Any> = emptyList()) {

            val size: Int get() = list.size

            fun clear(): SelectedRecord {
                return this.copy(list = emptyList())
            }

            fun selectAll(actions: List<Action>): SelectedRecord {
                val newList = list.toMutableList().apply {
                    actions.forEach { action ->
                        if (action.appInfo != null) {
                            add(action.appInfo!!)
                        } else if (action.shortcutInfo != null) {
                            add(action.shortcutInfo!!)
                        } else {
                            add(action)
                        }
                    }
                }
                return this.copy(list = newList)
            }

            fun selectAction(action: Action, selected: Boolean): SelectedRecord {
                return if (selected) {
                    upsertAction(action)
                } else {
                    this.copy(
                        list = list.filterNot {
                            it is Action && it.value == action.value
                        }
                    )
                }
            }

            fun selectAppInfo(app: AppInfo, selected: Boolean): SelectedRecord {
                val newList = list.toMutableList().apply {
                    if (selected) {
                        add(app)
                    } else {
                        removeAll { it is AppInfo && it.qualifiedName == app.qualifiedName }
                    }
                }
                return this.copy(list = newList)
            }

            fun selectShortcutInfo(shortcut: LauncherInfo.ShortcutInfo, selected: Boolean): SelectedRecord {
                val newList = list.toMutableList().apply {
                    if (selected) {
                        add(shortcut)
                    } else {
                        removeAll {
                            it is LauncherInfo.ShortcutInfo &&
                                    it.qualifiedNameWithIntents == shortcut.qualifiedNameWithIntents
                        }
                    }
                }
                return this.copy(list = newList)
            }

            fun removeAllAppInfos(list: List<AppInfo>): SelectedRecord {
                val newList = this.list.toMutableList().apply {
                    removeAll(list)
                    removeAll {
                        it is AppInfo &&
                                list.any { selected ->
                                    it.qualifiedName == selected.qualifiedName
                                }
                    }
                }
                return this.copy(list = newList)
            }

            fun removeAllShortcutInfos(list: List<LauncherInfo.ShortcutInfo>): SelectedRecord {
                val newList = this.list.toMutableList().apply {
                    removeAll {
                        it is LauncherInfo.ShortcutInfo &&
                                list.any { selected ->
                                    it.qualifiedNameWithIntents == selected.qualifiedNameWithIntents
                                }
                    }
                }
                return this.copy(list = newList)
            }

            fun isSelected(obj: Any): Boolean {
                if (obj is AppInfo) {
                    return list.find {
                        it is AppInfo && it.qualifiedName == obj.qualifiedName
                    } != null
                } else if (obj is LauncherInfo.ShortcutInfo) {
                    return list.find {
                        it is LauncherInfo.ShortcutInfo &&
                                it.qualifiedNameWithIntents == obj.qualifiedNameWithIntents
                    } != null
                } else if (obj is Action) {
                    return list.any {
                        it is Action && it.value == obj.value
                    }
                }
                return obj in list
            }

            fun findAction(value: String): Action? {
                return list.filterIsInstance<Action>().find { it.value == value }
            }

            fun upsertAction(action: Action): SelectedRecord {
                val index = list.indexOfFirst { it is Action && it.value == action.value }
                if (index == -1) {
                    return copy(list = list + action)
                }
                val newList = list.toMutableList().apply {
                    set(index, action)
                }
                return copy(list = newList)
            }
        }

        data class ActionSettingsDialogValue(
            val show: Boolean,
            val action: Action
        )

        data class ShellActionDialogValue(
            val show: Boolean = false,
            val action: Action = Action(value = GlobalActions.SHIZUKU_SHELL),
            val command: String = "",
            val status: ShizukuShellManager.ShizukuStatus = ShizukuShellManager.currentStatus(),
            val isTesting: Boolean = false,
            val testResult: ShellResult? = null
        )
    }

    sealed interface UiEvent {
        data class GotoIconResize(val iconResize: IconResize) : UiEvent
        data class GotoQuickLauncherConfig(val route: ActionSelect) : UiEvent
    }
}
