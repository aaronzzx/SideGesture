package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonLength
import com.aaron.sidegesture.constant.GlobalSettings.MaxGestureButtonStart
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.constant.TriggerDirection.Center
import com.aaron.sidegesture.constant.TriggerDirection.Down
import com.aaron.sidegesture.constant.TriggerDirection.Up
import com.aaron.sidegesture.entity.Actions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.fraction
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/28
 */
class GestureButtonSettingsVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    private val gestureButtonSettings = savedStateHandle.toRoute<GestureButtonSettings>()

    override val initialState: UiState = UiState(gestureButtonSettings)

    val colorPickerDialog = ColorPickerDialog()

    private var actionDialogDirection: TriggerDirection? = null

    init {
        loadData()
    }

    fun showActionDialog(direction: TriggerDirection, selectedAction: Actions) {
        actionDialogDirection = direction
        updateUiState {
            it.copy(
                actionDialog = it.actionDialog.copy(
                    first = true,
                    second = selectedAction.value
                )
            )
        }
    }

    fun onActionSelect(action: String) {
        updateUiState {
            it.copy(actionDialog = it.actionDialog.copy(first = false, second = action))
        }
        val actionDialogDirection = actionDialogDirection ?: return
        val uiState = uiState
        val button = uiState.gestureButton ?: return
        val index = uiState.gestureButtons.indexOf(button)
        if (index == -1) return
        viewModelScope.launch {
            val newActions = when (actionDialogDirection) {
                Center -> button.slideActions.copy(center = Actions.single(action))
                Up -> button.slideActions.copy(up = Actions.single(action))
                Down -> button.slideActions.copy(down = Actions.single(action))
            }
            val newButton = button.copy(slideActions = newActions)
            val newList = uiState.gestureButtons.toMutableList().apply {
                set(index, newButton)
            }
            DataStoreHolder.gestureButtons.updateData {
                newList
            }
        }
    }

    fun hideActionDialog() {
        updateUiState {
            it.copy(actionDialog = it.actionDialog.copy(first = false))
        }
    }

    fun showLongActionDialog(direction: TriggerDirection, selectedActions: Actions) {
        actionDialogDirection = direction
        updateUiState {
            it.copy(
                longActionDialog = it.longActionDialog.copy(
                    first = true,
                    second = selectedActions.values
                )
            )
        }
    }

    fun onLongActionSelect(actions: List<String>) {
        updateUiState {
            it.copy(longActionDialog = it.longActionDialog.copy(second = actions))
        }
    }

    fun onLongActionConfirm(actions: List<String>) {
        val actionDialogDirection = actionDialogDirection ?: return
        val uiState = uiState
        val button = uiState.gestureButton ?: return
        val index = uiState.gestureButtons.indexOf(button)
        if (index == -1) return
        viewModelScope.launch {
            val sortedActions = actions.sorted()
            val newActions = when (actionDialogDirection) {
                Center -> button.longSlideActions.copy(center = Actions.multiple(*sortedActions.toTypedArray()))
                Up -> button.longSlideActions.copy(up = Actions.multiple(*sortedActions.toTypedArray()))
                Down -> button.longSlideActions.copy(down = Actions.multiple(*sortedActions.toTypedArray()))
            }
            val newButton = button.copy(longSlideActions = newActions)
            val newList = uiState.gestureButtons.toMutableList().apply {
                set(index, newButton)
            }
            DataStoreHolder.gestureButtons.updateData {
                newList
            }
        }
    }

    fun hideLongActionDialog() {
        updateUiState {
            it.copy(longActionDialog = it.longActionDialog.copy(first = false))
        }
    }

    fun showDeleteWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showDeleteWarningDialog = show)
        }
    }

    fun deleteGestureButton() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.updateData {
                it.toMutableList().apply {
                    removeAll { item ->
                        item.id == uiState.gestureButton?.id
                    }
                }
            }
        }.invokeOnCompletion {
            finish()
        }
    }

    fun onGestureButtonWidthChange(width: Float) {
        updateUiState {
            val l = it.gestureButtons.toMutableList().also { list ->
                list.forEachIndexed { index, b ->
                    if (b.id != gestureButtonSettings.buttonId) {
                        return@forEachIndexed
                    }
                    if (b.position == gestureButtonSettings.position || it.alignRegion) {
                        list[index] = b.copy(width = width.toInt())
                    }
                }
            }
            it.copy(gestureButtons = l)
        }
        saveSettings()
    }

    fun onGestureButtonLengthChange(fraction: Float) {
        updateUiState {
            val l = it.gestureButtons.toMutableList().also { list ->
                list.forEachIndexed { index, b ->
                    if (b.id != gestureButtonSettings.buttonId) {
                        return@forEachIndexed
                    }
                    if (b.position == gestureButtonSettings.position || it.alignRegion) {
                        list[index] = b.let {
                            val start = b.start
                            val end = b.end
                            val newEnd = start + fraction
                            if (newEnd > MaxGestureButtonLength) {
                                val residue = newEnd - MaxGestureButtonLength
                                val newStart = end - b.fraction - residue
                                return@let b.copy(start = newStart, end = newEnd)
                            }
                            b.copy(start = start, end = newEnd)
                        }
                    }
                }
            }
            it.copy(gestureButtons = l)
        }
    }

    fun onGestureButtonLocationChange(value: Float) {
        updateUiState {
            val l = it.gestureButtons.toMutableList().also { list ->
                list.forEachIndexed { index, b ->
                    if (b.id != gestureButtonSettings.buttonId) {
                        return@forEachIndexed
                    }
                    if (b.position == gestureButtonSettings.position || it.alignRegion) {
                        list[index] = b.let {
                            var startValue = MaxGestureButtonStart - value
                            val fraction = b.fraction
                            var end = startValue + fraction
                            if (end >= MaxGestureButtonLength) {
                                end = MaxGestureButtonLength
                                startValue = end - fraction
                            }
                            b.copy(start = startValue, end = end)
                        }
                    }
                }

            }
            it.copy(gestureButtons = l)
        }
    }

    fun onGestureButtonAlignChange(value: Boolean) {
        updateUiState {
            val button = it.gestureButton
            val list = if (!value || button == null) it.gestureButtons else {
                it.gestureButtons.toMutableList().apply {
                    forEachIndexed { index, b ->
                        if (button.id == b.id) {
                            val newB = b.copy(
                                width = button.width,
                                start = button.start,
                                end = button.end
                            )
                            set(index, newB)
                        }
                    }
                }
            }
            it.copy(
                gestureButtons = list,
                alignRegion = value
            )
        }
        saveSettings()
    }

    fun saveSettings() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.updateData {
                uiState.gestureButtons
            }
        }
    }

    inner class ColorPickerDialog {

        fun show(show: Boolean) {
            updateUiState {
                val color = it.gestureButton?.let { b -> Color(b.color) } ?: it.colorPickerDialog.second
                it.copy(
                    colorPickerDialog = it.colorPickerDialog.copy(first = show, second = color)
                )
            }
        }

        fun onColorChange(color: Color) {
            updateUiState {
                it.copy(colorPickerDialog = it.colorPickerDialog.copy(second = color))
            }
        }

        fun confirm() {
            updateUiState {
                val pickedColor = it.colorPickerDialog.second
                val l = it.gestureButtons.toMutableList().also { list ->
                    val gestureButtonSettings = gestureButtonSettings
                    list.forEachIndexed { index, b ->
                        if (b.id != gestureButtonSettings.buttonId) {
                            return@forEachIndexed
                        }
                        if (b.position == gestureButtonSettings.position || it.alignRegion) {
                            list[index] = b.copy(color = pickedColor.toArgb())
                        }
                    }
                }
                it.copy(gestureButtons = l)
            }
            saveSettings()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.data.collectLatest { items ->
                val button = items.find {
                    it.id == gestureButtonSettings.buttonId &&
                            it.position == gestureButtonSettings.position
                }
                updateUiState {
                    it.copy(
                        gestureButtons = items,
                        alignRegion = button != null && items.filter { b ->
                            b.id == gestureButtonSettings.buttonId
                        }.all { b ->
                            b.width == button.width &&
                                    b.start == button.start &&
                                    b.end == button.end
                        }
                    )
                }
            }
        }
    }

    data class UiState(
        val gestureButtonSettings: GestureButtonSettings,
        val gestureButtons: List<GestureButton> = emptyList(),
        val alignRegion: Boolean = true,
        val showDeleteWarningDialog: Boolean = false,
        val colorPickerDialog: Pair<Boolean, Color> = Pair(false, Color.Red.copy(alpha = 0.1f)),
        val actionDialog: Pair<Boolean, String> = Pair(false, GlobalActions.NONE),
        val longActionDialog: Pair<Boolean, List<String>> = Pair(false, emptyList())
    ) {
        val gestureButton: GestureButton? = gestureButtons.find {
            it.id == gestureButtonSettings.buttonId &&
                    it.position == gestureButtonSettings.position
        }
    }

    sealed interface UiEvent
}