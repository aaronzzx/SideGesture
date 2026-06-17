package com.aaron.sidegesture.ui.dialog

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.ktx.coerceTimeMillis
import com.aaron.sidegesture.ui.dialog.ActionSettingsVM.UiEvent
import com.aaron.sidegesture.ui.dialog.ActionSettingsVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.PinyinSearchUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * @author DS-Z
 * @since 2025/6/30
 */
class ActionSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun onMoveScreenRateChange(rate: Float) {
        updateUiState {
            it.copy(
                actionSettings = it.actionSettings.copy(
                    moveScreen = it.actionSettings.moveScreen.copy(rate = rate)
                )
            )
        }
    }

    fun onMoveScreenHoverChange(hoverDelayMs: Float) {
        updateUiState {
            it.copy(
                actionSettings = it.actionSettings.copy(
                    moveScreen = it.actionSettings.moveScreen.copy(hoverDelayMs = hoverDelayMs.toLong())
                )
            )
        }
    }

    fun onMoveScreenStyleChange(style: ActionSettings.MoveScreen.Style) {
        val newActionSettings = uiState.actionSettings.copy(
            moveScreen = uiState.actionSettings.moveScreen.copy(style = style)
        )
        updateUiState { it.copy(actionSettings = newActionSettings) }
        persistActionSettings(newActionSettings)
    }

    fun onMoveScreenPopupEnabledChange(enabled: Boolean) {
        val newActionSettings = uiState.actionSettings.copy(
            moveScreen = uiState.actionSettings.moveScreen.copy(popupEnabled = enabled)
        )
        updateUiState { it.copy(actionSettings = newActionSettings) }
        persistActionSettings(newActionSettings)
    }

    fun updatePreviousAppInfos() {
        viewModelScope.launchWithLoading {
            val appInfos = withContext(Dispatchers.IO) {
                coerceTimeMillis(500) {
                    AppInfoUtils
                        .queryLauncherActivities(App.getContext(), allowRepeatPackage = false)
                        .filter {
                            it.packageName != App.getContext().packageName
                        }
                        .let(PinyinSearchUtils::sortAppInfos)
                }
            }
            arrangePreviousAppInfos(appInfos)
        }
    }

    fun updatePreviousAppSearchQuery(query: String) {
        updateUiState {
            applyPreviousAppSearchResult(it.copy(previousAppSearchQuery = query))
        }
    }

    fun selectPreviousApp(appInfo: AppInfo, selected: Boolean) {
        val currentPackageNames = uiState.actionSettings.previousApp.packageNames
        val newPackageNames = if (selected) {
            (currentPackageNames + appInfo.packageName).distinct()
        } else {
            currentPackageNames - appInfo.packageName
        }
        val newActionSettings = uiState.actionSettings.copy(
            previousApp = uiState.actionSettings.previousApp.copy(packageNames = newPackageNames)
        )
        updateUiState {
            applyPreviousAppSearchResult(it.copy(actionSettings = newActionSettings))
        }
        persistActionSettings(newActionSettings)
    }

    fun onGotoBottomStrengthChange(strength: Float) {
        updateUiState {
            it.copy(
                actionSettings = it.actionSettings.copy(
                    gotoBottom = it.actionSettings.gotoBottom.copy(strength = strength.roundToInt())
                )
            )
        }
    }

    fun saveSettings() {
        persistActionSettings(uiState.actionSettings)
    }

    private fun persistActionSettings(actionSettings: ActionSettings) {
        viewModelScope.launchWithLoading {
            DataStoreHolder.actionSettings.updateData {
                actionSettings
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder
                .actionSettings
                .data
                .take(1)
                .collectLatest { actionSettings ->
                    updateUiState {
                        applyPreviousAppSearchResult(
                            it.copy(
                                actionSettings = actionSettings,
                                actionSettingsLoaded = true
                            )
                        )
                    }
                }
        }
    }

    private fun applyPreviousAppSearchResult(uiState: UiState): UiState {
        if (!uiState.previousAppInfosLoaded) {
            return uiState
        }
        return uiState.copy(
            previousAppVisibleAppInfos = filterPreviousAppInfos(
                appInfos = uiState.previousAppRawAppInfos,
                query = uiState.previousAppSearchQuery
            )
        )
    }

    private suspend fun arrangePreviousAppInfos(appInfos: List<AppInfo>) {
        val packageNames = uiState.actionSettings.previousApp.packageNames
        val query = uiState.previousAppSearchQuery
        val arranged = withContext(Dispatchers.Default) {
            arrangePreviousAppInfos(
                appInfos = appInfos,
                packageNames = packageNames,
                query = query
            )
        }
        val newActionSettings = uiState.actionSettings.copy(
            previousApp = uiState.actionSettings.previousApp.copy(packageNames = arranged.packageNames)
        )
        updateUiState {
            it.copy(
                actionSettings = newActionSettings,
                previousAppInfosLoaded = true,
                previousAppRawAppInfos = arranged.orderedAppInfos,
                previousAppVisibleAppInfos = filterPreviousAppInfos(
                    appInfos = arranged.orderedAppInfos,
                    query = query
                )
            )
        }
        if (arranged.packageNames != packageNames) {
            persistActionSettings(newActionSettings)
        }
    }

    private fun arrangePreviousAppInfos(
        appInfos: List<AppInfo>,
        packageNames: List<String>,
        query: String
    ): ArrangedPreviousAppInfos {
        val validPackageNames = packageNames.toMutableList().apply {
            val launcherPackageNames = appInfos.map { it.packageName }
            removeAll { packageName -> packageName !in launcherPackageNames }
        }
        val selectedAppInfos = mutableListOf<AppInfo>()
        val unselectedAppInfos = mutableListOf<AppInfo>()
        filterPreviousAppInfos(appInfos, query).forEach { appInfo ->
            if (appInfo.packageName in validPackageNames) {
                selectedAppInfos.add(appInfo)
            } else {
                unselectedAppInfos.add(appInfo)
            }
        }
        return ArrangedPreviousAppInfos(
            packageNames = validPackageNames,
            orderedAppInfos = buildList {
                addAll(PinyinSearchUtils.sortAppInfos(selectedAppInfos))
                addAll(PinyinSearchUtils.sortAppInfos(unselectedAppInfos))
            }
        )
    }

    private fun filterPreviousAppInfos(appInfos: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return appInfos
        return appInfos.filter { appInfo ->
            PinyinSearchUtils.matches(
                query = query,
                label = appInfo.label,
                packageName = appInfo.packageName
            )
        }
    }

    data class UiState(
        val actionSettings: ActionSettings = ActionSettings(),
        val actionSettingsLoaded: Boolean = false,
        val previousAppInfosLoaded: Boolean = false,
        val previousAppSearchQuery: String = "",
        val previousAppRawAppInfos: List<AppInfo> = emptyList(),
        val previousAppVisibleAppInfos: List<AppInfo> = emptyList()
    )

    private data class ArrangedPreviousAppInfos(
        val packageNames: List<String>,
        val orderedAppInfos: List<AppInfo>
    )

    sealed interface UiEvent
}
