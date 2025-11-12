package com.aaron.sidegesture.ui.dialog

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.ui.dialog.ActionSettingsVM.UiEvent
import com.aaron.sidegesture.ui.dialog.ActionSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    fun onPreviousAppOperation(pkgName: String, add: Boolean) {
        updateUiState {
            val pkgNames = it.actionSettings.previousApp.packageNames
            val newPkgNames = if (add) {
                pkgNames + pkgName
            } else {
                pkgNames - pkgName
            }
            it.copy(
                actionSettings = it.actionSettings.copy(
                    previousApp = it.actionSettings.previousApp.copy(packageNames = newPkgNames)
                )
            )
        }
        saveSettings()
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
        viewModelScope.launchWithLoading {
            DataStoreHolder.actionSettings.updateData {
                uiState.actionSettings
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.actionSettings.data.collectLatest { actionSettings ->
                updateUiState {
                    it.copy(actionSettings = actionSettings)
                }
            }
        }
    }

    data class UiState(
        val actionSettings: ActionSettings = ActionSettings()
    )

    sealed interface UiEvent
}