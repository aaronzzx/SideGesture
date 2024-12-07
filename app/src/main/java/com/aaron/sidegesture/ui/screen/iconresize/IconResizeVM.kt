package com.aaron.sidegesture.ui.screen.iconresize

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.AppInfo.Companion.DEFAULT_SCALE
import com.aaron.sidegesture.entity.IconResize
import com.aaron.sidegesture.event.IconResizeEvent
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.ui.screen.iconresize.IconResizeVM.UiEvent
import com.aaron.sidegesture.ui.screen.iconresize.IconResizeVM.UiState
import com.aaron.sidegesture.utils.AppInfoUtils
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.Events
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */
class IconResizeVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    private val iconResize: IconResize = savedStateHandle.toRoute()

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
    }

    fun onIndexChange(index: Int) {
        updateUiState {
            it.copy(index = index)
        }
    }

    fun onScaleChange(scaleFactor: Float) {
        updateUiState {
            it.copy(
                scaleFactors = it.scaleFactors.toMutableMap().apply {
                    put(it.index, scaleFactor)
                }
            )
        }
    }

    fun reset() {
        updateUiState {
            it.copy(scaleFactors = emptyMap())
        }
    }

    fun done() {
        viewModelScope.launch {
            val uiState = uiState
            val appInfos = uiState.appInfos
            val scaleFactors = uiState.scaleFactors
            val newAppInfos = appInfos.toMutableList().apply {
                for (index in appInfos.indices) {
                    val appInfo = appInfos[index]
                    val scaleFactor = scaleFactors[index] ?: DEFAULT_SCALE
                    set(index, appInfo.copy(iconScale = scaleFactor))
                }
            }
            DataStoreHolder.advancedSettings.updateData {
                val newClipApps = it.clipApps.toMutableMap().apply {
                    newAppInfos.forEach { app ->
                        val key = app.qualifiedName
                        if (app.iconScale != DEFAULT_SCALE) {
                            put(key, app.iconScale)
                        } else {
                            remove(key)
                        }
                    }
                }
                it.copy(clipApps = newClipApps)
            }
            Events.post(IconResizeEvent(newAppInfos))
            finish()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.data.collectLatest { item ->
                val selectedQualifiedNames = iconResize.qualifiedNames
                val clipApps = item.clipApps
                val appInfos = withContext(Dispatchers.IO) {
                    AppInfoUtils.getInstalledPackages(App.getContext())
                }
                val normalList = mutableListOf<AppInfo>()
                val modifiedList = mutableListOf<AppInfo>()
                selectedQualifiedNames.forEach { qualifiedName ->
                    val appInfo = appInfos.find { it.qualifiedName == qualifiedName }
                    if (appInfo != null) {
                        if (clipApps.containsKey(qualifiedName)) {
                            modifiedList.add(appInfo)
                        } else {
                            normalList.add(appInfo)
                        }
                    }
                }
                val filters = normalList + modifiedList
                val map = mutableMapOf<Int, Float>()
                for (index in filters.indices) {
                    val appInfo = filters[index]
                    map[index] = clipApps[appInfo.qualifiedName] ?: DEFAULT_SCALE
                }
                updateUiState {
                    it.copy(
                        appInfos = filters,
                        scaleFactors = map
                    )
                }
            }
        }
    }

    data class UiState(
        val appInfos: List<AppInfo> = emptyList(),
        val scaleFactors: Map<Int, Float> = emptyMap(),
        val index: Int = 0,
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent
}