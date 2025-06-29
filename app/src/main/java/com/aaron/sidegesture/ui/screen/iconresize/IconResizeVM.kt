package com.aaron.sidegesture.ui.screen.iconresize

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.constant.ScaleableDefaults.DEFAULT_SCALE
import com.aaron.sidegesture.entity.IconResize
import com.aaron.sidegesture.event.IconResizeEvent
import com.aaron.sidegesture.ui.screen.iconresize.IconResizeVM.UiEvent
import com.aaron.sidegesture.ui.screen.iconresize.IconResizeVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.Events
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */
class IconResizeVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    private val iconResize: IconResize = savedStateHandle.toRoute()

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun showColorPickerDialog(show: Boolean) {
        updateUiState {
            it.copy(showColorPickerDialog = show)
        }
    }

    fun onBgColorEnabled(enabled: Boolean, defaultColor: Color) {
        updateUiState {
            it.copy(
                bgColors = it.bgColors.toMutableMap().apply {
                    val bgColor = get(it.selectedId)
                    if (bgColor != null) {
                        put(it.selectedId, bgColor.copy(enabled = enabled))
                    } else {
                        put(it.selectedId, UiState.BgColor(enabled = enabled, color = defaultColor))
                    }
                }
            )
        }
    }

    fun onBgColorChange(color: Color) {
        updateUiState {
            it.copy(
                bgColors = it.bgColors.toMutableMap().apply {
                    val bgColor = get(it.selectedId)
                    if (bgColor != null) {
                        put(it.selectedId, bgColor.copy(enabled = true, color = color))
                    } else {
                        put(it.selectedId, UiState.BgColor(enabled = true, color = color))
                    }
                }
            )
        }
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
    }

    fun onSelectedIdChange(id: String) {
        updateUiState {
            it.copy(selectedId = id)
        }
    }

    fun onScaleChange(scaleFactor: Float) {
        updateUiState {
            it.copy(
                scaleFactors = it.scaleFactors.toMutableMap().apply {
                    put(it.selectedId, scaleFactor)
                }
            )
        }
    }

    fun reset() {
        updateUiState {
            it.copy(
                scaleFactors = it.scaleFactors.toMutableMap().apply {
                    keys.forEach { id ->
                        put(id, DEFAULT_SCALE)
                    }
                },
                bgColors = emptyMap()
            )
        }
    }

    fun done() {
        viewModelScope.launch {
            val uiState = uiState
            val ids = uiState.ids
            val scaleFactors = uiState.scaleFactors
            DataStoreHolder.advancedSettings.updateData {
                val newClipApps = it.clipApps.toMutableMap()
                val newClipShortcuts = it.clipShortcuts.toMutableMap()
                ids.forEach { id ->
                    val scaleFactor = scaleFactors[id]
                    if (scaleFactor != null && scaleFactor != DEFAULT_SCALE) {
                        if (id.contains("intent")) {
                            newClipShortcuts[id] = scaleFactor
                        } else {
                            newClipApps[id] = scaleFactor
                        }
                    } else {
                        newClipApps.remove(id)
                        newClipShortcuts.remove(id)
                    }
                }
                it.copy(
                    clipApps = newClipApps,
                    clipShortcuts = newClipShortcuts
                )
            }
            val bgColors = mutableMapOf<String, Int>()
            uiState.bgColors.forEach { (id, bgColor) ->
                bgColors[id] = bgColor.color?.toArgb() ?: 0
            }
            Events.post(IconResizeEvent(scaleFactors, bgColors))
            finish()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.advancedSettings.data.collectLatest { advancedSettings ->
                val clipApps = advancedSettings.clipApps
                val clipShortcuts = advancedSettings.clipShortcuts
                val map = mutableMapOf<String, Float>()
                val ids = iconResize.ids
                for (id in ids) {
                    map[id] = clipApps[id] ?: clipShortcuts[id] ?: DEFAULT_SCALE
                }
                updateUiState {
                    val icons = IconResize.iconCache.toMap()
                    val bgColors = mutableMapOf<String, UiState.BgColor>()
                    IconResize.iconBgColorCache.forEach { (id, bgColor) ->
                        if (bgColor != 0) {
                            bgColors[id] = UiState.BgColor(true, Color(bgColor))
                        }
                    }
                    IconResize.iconCache.clear()
                    IconResize.iconBgColorCache.clear()
                    it.copy(
                        ids = ids,
                        icons = icons,
                        scaleFactors = map,
                        selectedId = ids.first(),
                        bgColors = bgColors
                    )
                }
            }
        }
    }

    data class UiState(
        val ids: List<String> = emptyList(),
        val icons: Map<String, Drawable> = emptyMap(),
        val scaleFactors: Map<String, Float> = emptyMap(),
        val selectedId: String = "",
        val bgColors: Map<String, BgColor> = emptyMap(),
        val showResetWarningDialog: Boolean = false,
        val showColorPickerDialog: Boolean = false
    ) {
        val selectedBgColor: BgColor? get() = bgColors[selectedId]

        data class BgColor(
            val enabled: Boolean = false,
            val color: Color? = null
        )
    }

    sealed interface UiEvent
}