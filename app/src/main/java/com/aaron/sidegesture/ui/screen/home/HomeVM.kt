package com.aaron.sidegesture.ui.screen.home

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.isAccessibilitySettingsOn
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiEvent
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
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
        updatePermissionState()
        loadData()
    }

    fun addGestureButton() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.updateData {
                GestureButton.Defaults.toMutableList().apply {
                    val id = SystemClock.uptimeMillis().toString()
                    val b1 = GestureButton(
                        id = id,
                        position = GestureButton.LEFT,
                        start = 0.0f,
                        end = 0.3f,
                        color = android.graphics.Color.YELLOW
                    )
                    val b2 = GestureButton(
                        id = id,
                        position = GestureButton.RIGHT,
                        start = 0.0f,
                        end = 0.3f,
                        color = android.graphics.Color.YELLOW
                    )
                    add(b1)
                    add(b2)
                }
            }
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

    fun expandGestureButtonList(expanded: Boolean) {
        updateUiState {
            it.copy(isGestureButtonListExpanded = expanded)
        }
    }

    fun onGestureEnabledChange(enabled: Boolean) {
        if (enabled && !uiState.isAccessibilityEnabled) {
            // TODO: show toast
            return
        }
        updateUiState {
            it.copy(isGestureEnabled = enabled)
        }
    }

    fun updatePermissionState() {
        val app = App.getContext()
        val isAccessibilityEnabled = app.isAccessibilitySettingsOn(SideGestureService::class.java)
        updateUiState {
            it.copy(
                isAccessibilityEnabled = isAccessibilityEnabled,
                isDrawOverlayEnabled = PermissionUtils.isGrantedDrawOverlays(),
                isGestureEnabled = isAccessibilityEnabled
            )
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
        val showMoreMenu: Boolean = false
    )

    sealed interface UiEvent
}