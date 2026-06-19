package com.aaron.sidegesture.ui.screen.miniwindowsettings

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindow.Bounds
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindowMode
import com.aaron.sidegesture.ui.screen.miniwindowsettings.MiniWindowSettingsVM.UiEvent
import com.aaron.sidegesture.ui.screen.miniwindowsettings.MiniWindowSettingsVM.UiState
import com.aaron.sidegesture.miniwindow.RomDetector
import com.aaron.sidegesture.miniwindow.RomType
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.MiniWindowUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/6/16
 */

class MiniWindowSettingsVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun showModeDropdownMenu(show: Boolean) {
        updateUiState { it.copy(showModeDropdownMenu = show) }
    }

    fun onModeChange(mode: MiniWindowMode) {
        updateUiState { it.copy(mode = mode) }
        persist()
    }

    fun onUseMiWindowChange(value: Boolean) {
        updateUiState { it.copy(useMiWindow = value) }
        persist()
    }

    fun onEditingPortraitChange(portrait: Boolean) {
        updateUiState { it.copy(editingPortrait = portrait) }
    }

    /** 拖拽过程中实时更新当前朝向的窗口矩形，不落盘。 */
    fun onBoundsChange(bounds: Bounds) {
        updateUiState {
            if (it.editingPortrait) it.copy(portrait = bounds) else it.copy(landscape = bounds)
        }
    }

    /** 拖拽结束统一落盘，避免每帧写 DataStore。 */
    fun onBoundsChangeFinished() {
        persist()
    }

    /** 拖动过程中实时更新当前朝向的缩放补偿，不落盘。 */
    fun onCurrentScaleChange(scale: Float) {
        updateUiState {
            if (it.editingPortrait) it.copy(portraitScale = scale) else it.copy(landscapeScale = scale)
        }
    }

    fun onScaleChangeFinished() {
        persist()
    }

    fun showResetDialog(show: Boolean) {
        updateUiState { it.copy(showResetDialog = show) }
    }

    /** 用户确认：关闭提示弹窗并记录已提示，下次不再弹。 */
    fun dismissVivoShareHintDialog() {
        updateUiState { it.copy(showVivoShareHintDialog = false) }
        viewModelScope.launch {
            DataStoreHolder.initialSettings.updateData {
                it.copy(miniWindowVivoShareHintShown = true)
            }
        }
    }

    /** 恢复整个小窗设置为默认值：启动模式、小窗助手开关、横竖屏尺寸位置与缩放补偿。 */
    fun resetAll() {
        val default = ActionSettings.MiniWindow()
        updateUiState {
            it.copy(
                mode = default.mode,
                useMiWindow = default.useMiWindow,
                portrait = default.portrait,
                landscape = default.landscape,
                portraitScale = default.portraitScale,
                landscapeScale = default.landscapeScale
            )
        }
        persist()
    }

    private fun persist() {
        val state = uiState
        viewModelScope.launch {
            DataStoreHolder.actionSettings.updateData {
                it.copy(
                    miniWindow = it.miniWindow.copy(
                        mode = state.mode,
                        useMiWindow = state.useMiWindow,
                        portrait = state.portrait,
                        landscape = state.landscape,
                        portraitScale = state.portraitScale,
                        landscapeScale = state.landscapeScale
                    )
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // RomDetector 走 getprop，放 IO 线程算自动补偿默认值与 vivo 判定
            val (autoP, autoL, isVivo) = withContext(Dispatchers.IO) {
                Triple(
                    MiniWindowUtils.autoScale(portrait = true),
                    MiniWindowUtils.autoScale(portrait = false),
                    RomDetector.detect().type == RomType.VIVO
                )
            }
            DataStoreHolder
                .actionSettings
                .data
                .take(1)
                .collectLatest { settings ->
                    val miniWindow = settings.miniWindow
                    updateUiState {
                        it.copy(
                            mode = miniWindow.mode,
                            useMiWindow = miniWindow.useMiWindow,
                            portrait = miniWindow.portrait,
                            landscape = miniWindow.landscape,
                            portraitScale = miniWindow.portraitScale,
                            landscapeScale = miniWindow.landscapeScale,
                            autoPortraitScale = autoP,
                            autoLandscapeScale = autoL
                        )
                    }
                }
            maybeShowVivoShareHint(isVivo)
        }
    }

    /** vivo 设备首次进入小窗设置时，弹窗提示去系统开启小窗分享开关。 */
    private suspend fun maybeShowVivoShareHint(isVivo: Boolean) {
        if (!isVivo) return
        if (DataStoreHolder.initialSettings.data.first().miniWindowVivoShareHintShown) return
        viewModelScope.launch {
            delay(200)
            updateUiState { it.copy(showVivoShareHintDialog = true) }
        }
    }

    data class UiState(
        val mode: MiniWindowMode = MiniWindowMode.Auto,
        val useMiWindow: Boolean = false,
        val editingPortrait: Boolean = true,
        val portrait: Bounds = ActionSettings.MiniWindow().portrait,
        val landscape: Bounds = ActionSettings.MiniWindow().landscape,
        // 横竖屏缩放补偿：null 表示按 ROM 自动，autoXxxScale 为自动模式下的生效值
        val portraitScale: Float? = null,
        val landscapeScale: Float? = null,
        val autoPortraitScale: Float = 1f,
        val autoLandscapeScale: Float = 1f,
        val showModeDropdownMenu: Boolean = false,
        val showResetDialog: Boolean = false,
        val showVivoShareHintDialog: Boolean = false
    ) {
        val currentBounds: Bounds get() = if (editingPortrait) portrait else landscape
        val currentScale: Float
            get() = if (editingPortrait) (portraitScale ?: autoPortraitScale)
            else (landscapeScale ?: autoLandscapeScale)
    }

    sealed interface UiEvent
}
