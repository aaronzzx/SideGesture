package com.aaron.sidegesture.ui.screen.bug

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.screen.bug.BugVM.UiEvent
import com.aaron.sidegesture.ui.screen.bug.BugVM.UiState
import com.aaron.sidegesture.utils.CrashHandler
import com.aaron.sidegesture.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author DS-Z
 * @since 2025/10/30
 */
class BugVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun reset() {
        viewModelScope.launchWithLoading(Dispatchers.IO) {
            CrashHandler.reset()
        }.invokeOnCompletion {
            if (it == null) {
                updateUiState {
                    it.copy(
                        bugs = emptyList(),
                        selectedBug = null
                    )
                }
                showToast(R.string.reset_success)
            } else {
                showToast(R.string.reset_failure)
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

    fun onClickItem(item: String) {
        updateUiState {
            val newSelected = if (item == it.selectedBug) {
                null
            } else {
                item
            }
            it.copy(selectedBug = newSelected)
        }
    }

    private fun loadData() {
        viewModelScope.launchWithLoading(cancelable = false) {
            updateUiState {
                it.copy(bugs = CrashHandler.getCrashList())
            }
        }
    }

    data class UiState(
        val bugs: List<String> = emptyList(),
        val selectedBug: String? = null,
        val showMoreMenu: Boolean = false,
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent
}