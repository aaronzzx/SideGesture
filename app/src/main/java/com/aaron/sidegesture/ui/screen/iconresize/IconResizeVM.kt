package com.aaron.sidegesture.ui.screen.iconresize

import androidx.lifecycle.SavedStateHandle
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.AppInfo.Companion.DEFAULT_SCALE
import com.aaron.sidegesture.entity.IconResize
import com.aaron.sidegesture.event.IconResizeEvent
import com.aaron.sidegesture.ui.screen.iconresize.IconResizeVM.UiEvent
import com.aaron.sidegesture.ui.screen.iconresize.IconResizeVM.UiState
import com.aaron.sidegesture.utils.Events

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */
class IconResizeVM(savedStateHandle: SavedStateHandle) : BaseComposeVM<UiState, UiEvent>() {

    private val iconResize: IconResize = IconResize.toRoute(savedStateHandle)

    override val initialState: UiState = run {
        val appInfos = iconResize.appInfos.sortedBy { it.iconScale }
        UiState(
            appInfos = appInfos,
            scaleFactors = run {
                val map = mutableMapOf<Int, Float>()
                for (index in appInfos.indices) {
                    val appInfo = appInfos[index]
                    map[index] = appInfo.iconScale
                }
                map
            }
        )
    }

    fun showResetWarningDialog(show: Boolean) {
        updateUiState {
            it.copy(showResetWarningDialog = show)
        }
    }

    fun onIndexChange(index: Int) {
        updateUiState {
            it.copy(index = index)
        }
    }

    fun onScaleChange(scaleFactor: Float) {
        updateUiState {
            it.copy(
                scaleFactors = it.scaleFactors.toMutableMap().apply {
                    put(it.index, scaleFactor)
                }
            )
        }
    }

    fun reset() {
        updateUiState {
            it.copy(scaleFactors = emptyMap())
        }
    }

    fun done() {
        updateUiState {
            val appInfos = it.appInfos
            val newAppInfos = appInfos.toMutableList().apply {
                for (index in appInfos.indices) {
                    val appInfo = appInfos[index]
                    val scaleFactor = it.scaleFactors[index] ?: DEFAULT_SCALE
                    set(index, appInfo.copy(iconScale = scaleFactor))
                }
            }
            it.copy(appInfos = newAppInfos)
        }
        Events.post(IconResizeEvent(uiState.appInfos))
        finish()
    }

    data class UiState(
        val appInfos: List<AppInfo> = emptyList(),
        val scaleFactors: Map<Int, Float> = emptyMap(),
        val index: Int = 0,
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent
}