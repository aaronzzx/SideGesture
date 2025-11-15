package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.constant.GlobalSettings.MinGestureButtonLength
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButtonSettings
import com.aaron.sidegesture.ktx.fraction
import com.aaron.sidegesture.ktx.rootSize
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/28
 */
class GestureButtonSettingsVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    private val gestureButtonSettings = savedStateHandle.toRoute<GestureButtonSettings>()

    override val initialState: UiState = UiState(gestureButtonSettings)

    val colorPickerDialog = ColorPickerDialog()

    private val maxExcludeSystemGestureFraction: Float by lazy {
        val rootSize = rootSize
        val maxExcludeSystemGestureHeight = ConvertUtils.dp2px(200f)
        maxExcludeSystemGestureHeight.toFloat() / rootSize.height.toFloat()
    }

    init {
        loadData()
    }

    fun showDeleteWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showDeleteWarningDialog = show)
        }
    }

    fun deleteGestureButton() {
        viewModelScope.launch {
            if (gestureButtonSettings.isSideButton) {
                DataStoreHolder.sideGestureButtons.updateData {
                    it.toMutableList().apply {
                        removeAll { item ->
                            item.id == uiState.gestureButton?.id
                        }
                    }
                }
            } else {
                DataStoreHolder.bottomGestureButtons.updateData {
                    it.toMutableList().apply {
                        removeAll { item ->
                            item.id == uiState.gestureButton?.id
                        }
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
            it.copy(
                gestureButtons = l,
                isGestureButtonAdjusting = true
            )
        }
    }

    fun onGestureButtonPositionChange(start: Float, end: Float) {
        val button = uiState.gestureButton ?: return
        val fraction = end - start
        if (fraction < MinGestureButtonLength ||
            (button.excludeSystemGestureRects &&
                    button.limitMaxExcludeSystemGestureLength &&
                    fraction > maxExcludeSystemGestureFraction)
        ) {
            return
        }
        updateUiState {
            val l = it.gestureButtons.toMutableList().also { list ->
                list.forEachIndexed { index, b ->
                    if (b.id != gestureButtonSettings.buttonId) {
                        return@forEachIndexed
                    }
                    if (b.position == gestureButtonSettings.position || it.alignRegion) {
                        list[index] = b.copy(
                            start = start,
                            end = end
                        )
                    }
                }
            }
            it.copy(
                gestureButtons = l,
                isGestureButtonAdjusting = true
            )
        }
    }

    fun onGestureButtonAdjustFinish() {
        updateUiState {
            it.copy(isGestureButtonAdjusting = false)
        }
        saveSettings()
    }

    fun onGestureButtonAlignChange(value: Boolean) {
        updateUiState {
            val button = it.gestureButton
            val list = if (button == null) it.gestureButtons else {
                it.gestureButtons.toMutableList().apply {
                    forEachIndexed { index, b ->
                        if (button.id == b.id) {
                            if (value) {
                                val newB = b.copy(
                                    width = button.width,
                                    start = button.start,
                                    end = button.end,
                                    alignRegion = true
                                )
                                set(index, newB)
                            } else {
                                val newB = b.copy(alignRegion = false)
                                set(index, newB)
                            }
                        }
                    }
                }
            }
            it.copy(gestureButtons = list)
        }
        saveSettings()
    }

    fun onExcludeSystemGestureRectsChange(value: Boolean) {
        updateUiState {
            val l = it.gestureButtons.toMutableList().also { list ->
                list.forEachIndexed { index, b ->
                    if (b.id != gestureButtonSettings.buttonId) {
                        return@forEachIndexed
                    }
                    if (b.position == gestureButtonSettings.position || it.alignRegion) {
                        val (start, end) = if (value && b.limitMaxExcludeSystemGestureLength) {
                            val maxFraction = maxExcludeSystemGestureFraction
                            val half = maxFraction / 2f
                            val center = b.start + (b.fraction / 2f)
                            val st = center - half
                            val ed = center + half
                            Pair(st, ed)
                        } else {
                            Pair(b.start, b.end)
                        }
                        list[index] = b.copy(
                            start = start,
                            end = end,
                            excludeSystemGestureRects = value
                        )
                    }
                }
            }
            it.copy(gestureButtons = l)
        }
        saveSettings()
    }

    fun onLimitMaxExcludeSystemGestureLengthChange(value: Boolean) {
        updateUiState {
            val l = it.gestureButtons.toMutableList().also { list ->
                list.forEachIndexed { index, b ->
                    if (b.id != gestureButtonSettings.buttonId) {
                        return@forEachIndexed
                    }
                    if (b.position == gestureButtonSettings.position || it.alignRegion) {
                        val (start, end) = if (value) {
                            val maxFraction = maxExcludeSystemGestureFraction
                            val half = maxFraction / 2f
                            val center = b.start + (b.fraction / 2f)
                            val st = center - half
                            val ed = center + half
                            Log.d("zzx", "${ed - st}, $maxFraction")
                            Pair(st, ed)
                        } else {
                            Pair(b.start, b.end)
                        }
                        list[index] = b.copy(
                            start = start,
                            end = end,
                            limitMaxExcludeSystemGestureLength = value
                        )
                    }
                }
            }
            it.copy(gestureButtons = l)
        }
        saveSettings()
    }

    fun saveSettings() {
        viewModelScope.launch {
            launch {
                if (gestureButtonSettings.isSideButton) {
                    DataStoreHolder.sideGestureButtons.updateData {
                        uiState.gestureButtons
                    }
                } else {
                    DataStoreHolder.bottomGestureButtons.updateData {
                        uiState.gestureButtons
                    }
                }
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
        val gestureButtonSettings = gestureButtonSettings
        viewModelScope.launch {
            updateUiState {
                val canShowExcludeSystemGestureRects = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        gestureButtonSettings.isSideButton &&
                        gestureButtonSettings.buttonId == GestureButton.ID_DEFAULT
                it.copy(
                    canShowExcludeSystemGestureRects = canShowExcludeSystemGestureRects
                )
            }
            launch {
                if (gestureButtonSettings.isSideButton) {
                    DataStoreHolder
                        .sideGestureButtons
                        .data
                        .take(1)
                        .collectLatest { items ->
                            val button = items.find {
                                it.id == gestureButtonSettings.buttonId &&
                                        it.position == gestureButtonSettings.position
                            }
                            updateUiState {
                                it.copy(
                                    gestureButtons = items,
                                    alignRegion = button?.alignRegion ?: true
                                )
                            }
                        }
                } else {
                    DataStoreHolder
                        .bottomGestureButtons
                        .data
                        .take(1)
                        .collectLatest { items ->
                            updateUiState {
                                it.copy(gestureButtons = items)
                            }
                        }
                }
            }
        }
    }

    data class UiState(
        val gestureButtonSettings: GestureButtonSettings,
        val gestureButtons: List<GestureButton> = emptyList(),
        val alignRegion: Boolean = true,
        val showDeleteWarningDialog: Boolean = false,
        val colorPickerDialog: Pair<Boolean, Color> = Pair(false, Color.Transparent),
        val isGestureButtonAdjusting: Boolean = false,
        val canShowExcludeSystemGestureRects: Boolean = false
    ) {
        val gestureButton: GestureButton? = gestureButtons.find {
            it.id == gestureButtonSettings.buttonId &&
                    it.position == gestureButtonSettings.position
        }
    }

    sealed interface UiEvent
}