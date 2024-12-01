package com.aaron.sidegesture.ui.screen.gestureangles

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.GestureAngles
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.whenPosition
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAnglesVM.UiEvent
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAnglesVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/26
 */
class GestureAnglesVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    private lateinit var leftAngle: GestureAngle
    private lateinit var rightAngle: GestureAngle

    init {
        loadData()
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
    }

    fun switchPosition(position: Int) {
        updateUiState {
            it.copy(position = position, angle = getGestureAngle(position))
        }
    }

    fun updateGestureAngle(angle: GestureAngle) {
        updateUiState {
            whenPosition(
                onLeft = { leftAngle = angle },
                onRight = { rightAngle = angle },
                position = it.position
            )
            it.copy(angle = angle)
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            launch {
                DataStoreHolder.gestureSettings.updateData {
                    it.copy(angles = GestureAngles(left = leftAngle, right = rightAngle))
                }
            }
            launch {
                DataStoreHolder.gestureButtons.updateData {
                    it.toMutableList().apply {
                        forEachIndexed { index, gestureButton ->
                            val newButton = gestureButton.copy(
                                angle = whenPosition(
                                    onLeft = { leftAngle },
                                    onRight = { rightAngle },
                                    position = gestureButton.position
                                )
                            )
                            set(index, newButton)
                        }
                    }
                }
            }
        }.invokeOnCompletion { ex ->
            if (ex == null) {
                toast(R.string.save_success)
            } else {
                toast(R.string.save_failure)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            DataStoreHolder.gestureSettings.updateData {
                it.copy(angles = GestureAngles())
            }
        }.invokeOnCompletion { ex ->
            if (ex == null) {
                toast(R.string.reset_success)
            } else {
                toast(R.string.reset_failure)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureSettings.data.collectLatest { item ->
                leftAngle = item.angles.left
                rightAngle = item.angles.right
                updateUiState {
                    it.copy(angle = getGestureAngle(it.position))
                }
            }
        }
    }

    private fun getGestureAngle(position: Int): GestureAngle {
        return whenPosition(
            onLeft = { leftAngle },
            onRight = { rightAngle },
            position = position
        )
    }

    data class UiState(
        val angle: GestureAngle = GestureAngle(),
        val position: Int = GestureButton.LEFT,
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent
}