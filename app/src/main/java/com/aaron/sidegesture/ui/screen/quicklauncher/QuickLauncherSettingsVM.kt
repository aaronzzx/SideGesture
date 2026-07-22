package com.aaron.sidegesture.ui.screen.quicklauncher

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.global.QuickLauncherSettings
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class QuickLauncherSettingsVM :
    BaseComposeVM<QuickLauncherSettingsVM.UiState, QuickLauncherSettingsVM.UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun onRowsChange(value: Float) {
        updateUiState {
            if (!it.loaded) return@updateUiState it
            it.copy(settings = it.settings.copy(rows = value.roundToInt()).normalized())
        }
    }

    fun onColumnsChange(value: Float) {
        updateUiState {
            if (!it.loaded) return@updateUiState it
            it.copy(settings = it.settings.copy(columns = value.roundToInt()).normalized())
        }
    }

    fun onIconSizeChange(value: Float) {
        updateUiState {
            if (!it.loaded) return@updateUiState it
            it.copy(settings = it.settings.copy(iconSizeDp = value.roundToInt()).normalized())
        }
    }

    fun onTextSizeChange(value: Float) {
        updateUiState {
            if (!it.loaded) return@updateUiState it
            it.copy(settings = it.settings.copy(textSizeSp = value.roundToInt()).normalized())
        }
    }

    fun saveSettings() {
        if (!uiState.loaded) return
        val settings = uiState.settings.normalized()
        viewModelScope.launch {
            DataStoreHolder.actionSettings.updateData {
                it.copy(quickLauncher = settings)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.actionSettings.data.take(1).collectLatest { actionSettings ->
                updateUiState {
                    it.copy(
                        settings = actionSettings.quickLauncher.normalized(),
                        loaded = true
                    )
                }
            }
        }
    }

    data class UiState(
        val settings: QuickLauncherSettings = QuickLauncherSettings(),
        val loaded: Boolean = false
    )

    sealed interface UiEvent
}
