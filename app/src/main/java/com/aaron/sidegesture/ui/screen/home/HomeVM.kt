package com.aaron.sidegesture.ui.screen.home

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.isAccessibilitySettingsOn
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiEvent
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.showToast
import com.blankj.utilcode.util.PermissionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */
class HomeVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun addGestureButton() {
        if (uiState.gestureButtons.size >= 20) {
            showToast(R.string.gesture_button_size_max)
            return
        }
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.updateData {
                it.toMutableList().apply {
                    addAll(GestureButton.createPair())
                }
            }
        }
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
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

    fun expandGestureButtonList(expanded: Boolean, scrollOffset: Float = Float.NaN) {
        updateUiState {
            it.copy(isGestureButtonListExpanded = expanded)
        }
        if (expanded) {
            sendUiEvent(UiEvent.ScrollEvent(scrollOffset))
        }
    }

    fun onAppGestureEnabledChange(enabled: Boolean) {
        updateUiState {
            it.copy(isGestureEnabled = enabled)
        }
        saveSettings()
    }

    fun onGestureButtonEnabledChange(button: GestureButton, enabled: Boolean) {
        updateUiState {
            val buttons = it.gestureButtons
            val index = buttons.indexOf(button)
            if (index < 0) it else {
                val list = buttons.toMutableList().apply {
                    set(index, button.copy(enabled = enabled))
                }
                it.copy(gestureButtons = list)
            }
        }
        saveSettings()
    }

    fun updatePermissionState() {
        viewModelScope.launch {
            DataStoreHolder.initialSettings.data.collectLatest { item ->
                val app = App.getContext()
                val isAccessibilityEnabled = app.isAccessibilitySettingsOn(SideGestureService::class.java)
                updateUiState {
                    it.copy(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isDrawOverlayEnabled = PermissionUtils.isGrantedDrawOverlays(),
                        isGestureEnabled = item.gestureEnabled
                    )
                }
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            DataStoreHolder.resetAll()
        }
    }

    private fun saveSettings() {
        viewModelScope.launch {
            launch {
                DataStoreHolder.initialSettings.updateData {
                    it.copy(gestureEnabled = uiState.isGestureEnabled)
                }
            }
            launch {
                DataStoreHolder.gestureButtons.updateData {
                    uiState.gestureButtons
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.data.collectLatest { buttons ->
                updateUiState {
                    it.copy(gestureButtons = buttons.sorted())
                }
            }
        }
    }

    data class UiState(
        val gestureButtons: List<GestureButton> = emptyList(),
        val isGestureEnabled: Boolean = false,
        val isAccessibilityEnabled: Boolean = false,
        val isDrawOverlayEnabled: Boolean = false,
        val isGestureButtonListExpanded: Boolean = false,
        val showMoreMenu: Boolean = false,
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent {

        data class ScrollEvent(val offsetY: Float) : UiEvent
    }
}