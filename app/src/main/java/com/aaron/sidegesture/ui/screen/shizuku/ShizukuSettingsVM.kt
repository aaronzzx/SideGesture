package com.aaron.sidegesture.ui.screen.shizuku

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.shizuku.ShizukuShellManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShizukuSettingsVM : BaseComposeVM<ShizukuSettingsVM.UiState, ShizukuSettingsVM.UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        observeStatus()
    }

    fun requestPermission() {
        viewModelScope.launch {
            ShizukuShellManager.requestPermission()
        }
    }

    fun refreshStatus() {
        ShizukuShellManager.updateStatus()
    }

    private fun observeStatus() {
        viewModelScope.launch {
            ShizukuShellManager.statusFlow.collectLatest { status ->
                updateUiState {
                    it.copy(status = status)
                }
            }
        }
    }

    data class UiState(
        val status: ShizukuShellManager.ShizukuStatus = ShizukuShellManager.currentStatus()
    )

    sealed interface UiEvent
}
