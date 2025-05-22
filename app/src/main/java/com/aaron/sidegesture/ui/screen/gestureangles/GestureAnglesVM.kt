package com.aaron.sidegesture.ui.screen.gestureangles

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.GestureAngles
import com.aaron.sidegesture.entity.Position
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
    private lateinit var bottomAngle: GestureAngle

    init {
        loadData()
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
    }

    fun switchPosition(position: Position) {
        updateUiState {
            it.copy(position = position, angle = getGestureAngle(position))
        }
    }

    fun updateGestureAngle(angle: GestureAngle) {
        updateUiState {
            when (it.position) {
                Position.Left -> leftAngle = angle
                Position.Right -> rightAngle = angle
                Position.Bottom -> bottomAngle = angle
            }
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
                DataStoreHolder.sideGestureButtons.updateData {
                    it.toMutableList().apply {
                        forEachIndexed { index, gestureButton ->
                            val newButton = gestureButton.copy(
                                angle = when (gestureButton.position) {
                                    Position.Left -> leftAngle
                                    Position.Right -> rightAngle
                                    Position.Bottom -> bottomAngle
                                }
                            )
                            set(index, newButton)
                        }
                    }
                }
            }
            launch {
                DataStoreHolder.bottomGestureButtons.updateData {
                    it.toMutableList().apply {
                        forEachIndexed { index, gestureButton ->
                            val newButton = gestureButton.copy(
                                angle = when (gestureButton.position) {
                                    Position.Left -> leftAngle
                                    Position.Right -> rightAngle
                                    Position.Bottom -> bottomAngle
                                }
                            )
                            set(index, newButton)
                        }
                    }
                }
            }
        }.invokeOnCompletion { ex ->
            if (ex == null) {
                toast(R.string.save_success)
                finish()
            } else {
                toast(R.string.save_failure)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            val angles = GestureAngles()
            launch {
                DataStoreHolder.gestureSettings.updateData {
                    it.copy(angles = angles)
                }
            }
            launch {
                DataStoreHolder.sideGestureButtons.updateData {
                    it.toMutableList().apply {
                        forEachIndexed { index, gestureButton ->
                            val newButton = gestureButton.copy(
                                angle = when (gestureButton.position) {
                                    Position.Left -> angles.left
                                    Position.Right -> angles.right
                                    Position.Bottom -> angles.bottom
                                }
                            )
                            set(index, newButton)
                        }
                    }
                }
            }
            launch {
                DataStoreHolder.bottomGestureButtons.updateData {
                    it.toMutableList().apply {
                        forEachIndexed { index, gestureButton ->
                            val newButton = gestureButton.copy(
                                angle = when (gestureButton.position) {
                                    Position.Left -> angles.left
                                    Position.Right -> angles.right
                                    Position.Bottom -> angles.bottom
                                }
                            )
                            set(index, newButton)
                        }
                    }
                }
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
                bottomAngle = item.angles.bottom
                updateUiState {
                    it.copy(angle = getGestureAngle(it.position))
                }
            }
        }
    }

    private fun getGestureAngle(position: Position): GestureAngle {
        return when (position) {
            Position.Left -> leftAngle
            Position.Right -> rightAngle
            Position.Bottom -> bottomAngle
        }
    }

    data class UiState(
        val angle: GestureAngle = GestureAngle(),
        val position: Position = Position.Left,
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent
}