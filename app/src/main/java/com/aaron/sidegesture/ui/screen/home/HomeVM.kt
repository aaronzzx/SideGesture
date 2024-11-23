package com.aaron.sidegesture.ui.screen.home

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.ktx.isAccessibilitySettingsOn
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiEvent
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiState
import com.blankj.utilcode.util.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */
class HomeVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        updateSystemPermissions()
    }

    fun showMoreMenu(show: Boolean, delayBlock: (() -> Unit)? = null) {
        viewModelScope.launch {
            updateUiState {
                it.copy(showMoreMenu = show)
            }
            if (delayBlock != null) {
                delay(100)
                delayBlock()
            }
        }
    }

    fun expandGestureButtonList(expanded: Boolean) {
        updateUiState {
            it.copy(isGestureButtonListExpanded = expanded)
        }
    }

    fun onGestureEnabledChange(enabled: Boolean) {
        updateUiState {
            it.copy(isGestureEnabled = enabled)
        }
    }

    fun updateSystemPermissions() {
        val app = App.getContext()
        val isAccessibilityEnabled = app.isAccessibilitySettingsOn(SideGestureService::class.java)
        updateUiState {
            it.copy(
                isAccessibilityEnabled = isAccessibilityEnabled,
                isDrawOverlayEnabled = PermissionUtils.isGrantedDrawOverlays()
            )
        }
    }

    data class UiState(
        val isGestureEnabled: Boolean = true,
        val isAccessibilityEnabled: Boolean = false,
        val isDrawOverlayEnabled: Boolean = false,
        val isGestureButtonListExpanded: Boolean = false,
        val showMoreMenu: Boolean = false
    )

    sealed interface UiEvent
}