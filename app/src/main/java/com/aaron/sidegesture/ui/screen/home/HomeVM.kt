package com.aaron.sidegesture.ui.screen.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.isAccessibilitySettingsOn
import com.aaron.sidegesture.ktx.isIgnoringBatteryOptimizations
import com.aaron.sidegesture.shizuku.ShizukuShellManager
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiEvent
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiState
import com.aaron.sidegesture.utils.BackupHelper
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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

    fun backup(context: Context, saveTo: Uri) {
        viewModelScope.launchWithLoading(
            Dispatchers.IO + CoroutineExceptionHandler { _, ex ->
                ex.printStackTrace()
                toast(R.string.backup_failed)
            },
            cancelable = false
        ) {
            BackupHelper.backup(context, saveTo)
            toast(R.string.backup_success)
        }
    }

    fun restore(context: Context, restoreFrom: Uri) {
        viewModelScope.launchWithLoading(
            Dispatchers.IO + CoroutineExceptionHandler { _, ex ->
                ex.printStackTrace()
                toast(R.string.restore_failed)
            },
            cancelable = false
        ) {
            BackupHelper.restore(context, restoreFrom)
            toast(R.string.restore_success)
        }
    }

    fun addBottomGestureButton() {
        if (uiState.bottomGestureButtons.size >= 10) {
            toast(R.string.gesture_button_size_max)
            return
        }
        viewModelScope.launch {
            DataStoreHolder.bottomGestureButtons.updateData {
                it.toMutableList().apply {
                    add(GestureButton.createBottom())
                }
            }
            delay(50)
            sendUiEvent(UiEvent.ScrollToBottom)
        }
    }

    fun addSideGestureButton() {
        if (uiState.sideGestureButtons.size >= 20) {
            toast(R.string.gesture_button_size_max)
            return
        }
        viewModelScope.launch {
            DataStoreHolder.sideGestureButtons.updateData {
                it.toMutableList().apply {
                    addAll(GestureButton.createSidePair())
                }
            }
            delay(50)
            sendUiEvent(UiEvent.ScrollToBottom)
        }
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
    }

    fun showBackupRestoreDialog(show: Boolean) {
        updateUiState {
            it.copy(showBackupRestoreDialog = show)
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

    fun expandBottomGestureButtonList(expanded: Boolean, scrollOffset: Int = Int.MAX_VALUE) {
        updateUiState {
            it.copy(
                isBottomGestureButtonListExpanded = expanded,
                isSideGestureButtonListExpanded = it.isSideGestureButtonListExpanded && !expanded
            )
        }
        if (expanded && scrollOffset != Int.MAX_VALUE) {
            sendUiEvent(UiEvent.ScrollToEvent(scrollOffset))
        }
    }

    fun expandSideGestureButtonList(expanded: Boolean, scrollOffset: Int = Int.MAX_VALUE) {
        updateUiState {
            it.copy(
                isSideGestureButtonListExpanded = expanded,
                isBottomGestureButtonListExpanded = it.isBottomGestureButtonListExpanded && !expanded
            )
        }
        if (expanded && scrollOffset != Int.MAX_VALUE) {
            sendUiEvent(UiEvent.ScrollToEvent(scrollOffset))
        }
    }

    fun onAppGestureEnabledChange(enabled: Boolean) {
        if (!uiState.isAccessibilityEnabled) {
            toast(R.string.please_enable_accessibility_service_first)
            return
        }
        updateUiState {
            it.copy(isGestureEnabled = enabled)
        }
        saveSettings()
    }

    fun onBottomGestureButtonEnabledChange(button: GestureButton, enabled: Boolean) {
        updateUiState {
            val buttons = it.bottomGestureButtons
            val index = buttons.indexOf(button)
            if (index < 0) it else {
                val list = buttons.toMutableList().apply {
                    set(index, button.copy(enabled = enabled))
                }
                it.copy(bottomGestureButtons = list)
            }
        }
        saveSettings()
    }

    fun onSideGestureButtonEnabledChange(button: GestureButton, enabled: Boolean) {
        updateUiState {
            val buttons = it.sideGestureButtons
            val index = buttons.indexOf(button)
            if (index < 0) it else {
                val list = buttons.toMutableList().apply {
                    set(index, button.copy(enabled = enabled))
                }
                it.copy(sideGestureButtons = list)
            }
        }
        saveSettings()
    }

    fun updatePermissionState() {
        viewModelScope.launch {
            syncPermissionState()
        }
    }

    fun onHomeResumed() {
        viewModelScope.launch {
            syncPermissionState()
            ShizukuShellManager.autoRequestPermissionIfNeeded()
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
                DataStoreHolder.sideGestureButtons.updateData {
                    uiState.sideGestureButtons
                }
            }
            launch {
                DataStoreHolder.bottomGestureButtons.updateData {
                    uiState.bottomGestureButtons
                }
            }
        }
    }

    private suspend fun syncPermissionState() {
        val app = App.getContext()
        val isGestureEnabled = DataStoreHolder.initialSettings.data.first().gestureEnabled
        val isAccessibilityEnabled = app.isAccessibilitySettingsOn(SideGestureService::class.java)
        val isIgnoringBatteryOptimizations = app.isIgnoringBatteryOptimizations()
        updateUiState {
            it.copy(
                isGestureEnabled = isAccessibilityEnabled && isGestureEnabled,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                val app = App.getContext()
                DataStoreHolder.initialSettings.data.collectLatest { initialSettings ->
                    // 实时重读无障碍状态，避免闭包冻结旧值在后续发射时把开关错误打回
                    val isAccessibilityEnabled = app.isAccessibilitySettingsOn(SideGestureService::class.java)
                    updateUiState {
                        it.copy(isGestureEnabled = isAccessibilityEnabled && initialSettings.gestureEnabled)
                    }
                }
            }
            launch {
                DataStoreHolder.sideGestureButtons.data.collectLatest { buttons ->
                    updateUiState {
                        it.copy(sideGestureButtons = buttons.sortedBy { b -> b.id })
                    }
                }
            }
            launch {
                DataStoreHolder.bottomGestureButtons.data.collectLatest { buttons ->
                    updateUiState {
                        it.copy(bottomGestureButtons = buttons.sortedBy { b -> b.id })
                    }
                }
            }
        }
    }

    data class UiState(
        val sideGestureButtons: List<GestureButton> = emptyList(),
        val bottomGestureButtons: List<GestureButton> = emptyList(),
        val isGestureEnabled: Boolean = false,
        val isAccessibilityEnabled: Boolean = false,
        val isIgnoringBatteryOptimizations: Boolean = false,
        val isDrawOverlayEnabled: Boolean = false,
        val isPopBackgroundEnabled: Boolean = false,
        val isBottomGestureButtonListExpanded: Boolean = false,
        val isSideGestureButtonListExpanded: Boolean = false,
        val showMoreMenu: Boolean = false,
        val showResetWarningDialog: Boolean = false,
        val showBackupRestoreDialog: Boolean = false
    )

    sealed interface UiEvent {

        data object ScrollToBottom : UiEvent
        data class ScrollToEvent(val offsetY: Int) : UiEvent
    }
}
