package com.aaron.sidegesture.ui.screen.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.global.Backup
import com.aaron.sidegesture.ktx.isAccessibilitySettingsOn
import com.aaron.sidegesture.ktx.isIgnoringBatteryOptimizations
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiEvent
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiState
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.DataStoreHolder.advancedSettings
import com.aaron.sidegesture.utils.DataStoreHolder.gestureButtons
import com.aaron.sidegesture.utils.DataStoreHolder.gestureSettings
import com.aaron.sidegesture.utils.DataStoreHolder.initialSettings
import com.aaron.sidegesture.utils.JsonHelper
import com.aaron.sidegesture.utils.showToastLong
import com.blankj.utilcode.util.EncodeUtils
import com.blankj.utilcode.util.TimeUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    fun backup(context: Context, uri: Uri) {
        viewModelScope.launchWithLoading(
            Dispatchers.IO + CoroutineExceptionHandler { _, _ ->
                toast(R.string.backup_failed)
            },
            cancelable = false
        ) {
            val backup = Backup(
                initialSettings = async { initialSettings.data.first() }.await(),
                advancedSettings = async { advancedSettings.data.first() }.await(),
                gestureSettings = async { gestureSettings.data.first() }.await(),
                gestureButtons = async { gestureButtons.data.first() }.await(),
                timestamp = System.currentTimeMillis(),
                version = BuildConfig.VERSION_NAME
            )
            val json = JsonHelper.encodeToString(backup)
            val encoded = EncodeUtils.base64Encode(json.toByteArray())
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(encoded)
                outputStream.flush()
            }
            toast(R.string.backup_success)
        }
    }

    fun restore(context: Context, uri: Uri) {
        viewModelScope.launchWithLoading(
            Dispatchers.IO + CoroutineExceptionHandler { _, ex ->
                ex.printStackTrace()
                toast(R.string.restore_failed)
            },
            cancelable = false
        ) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val input = inputStream.readBytes()
                val decoded = EncodeUtils.base64Decode(input)
                val backup = JsonHelper.decodeFromString<Backup>(String(decoded))
                coroutineScope {
                    listOf(
                        async {
                            initialSettings.updateData {
                                backup.initialSettings ?: it
                            }
                        },
                        async {
                            advancedSettings.updateData {
                                backup.advancedSettings ?: it
                            }
                        },
                        async {
                            gestureSettings.updateData {
                                backup.gestureSettings ?: it
                            }
                        },
                        async {
                            gestureButtons.updateData {
                                backup.gestureButtons ?: it
                            }
                        }
                    ).awaitAll()
                }
                var postfix = ""
                val version = backup.version?.let {
                    "v$it"
                }
                val date = backup.timestamp?.let {
                    TimeUtils.millis2String(backup.timestamp, "yyyy/MM/dd HH:mm:ss")
                }
                if (version != null) {
                    postfix += version
                }
                if (date != null) {
                    postfix += "-$date"
                }
                if (postfix.isNotBlank()) {
                    showToastLong(context.getString(R.string.restore_success_with_date, postfix))
                } else {
                    toast(R.string.restore_success)
                }
            }
        }
    }

    fun addGestureButton() {
        if (uiState.gestureButtons.size >= 20) {
            toast(R.string.gesture_button_size_max)
            return
        }
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.updateData {
                it.toMutableList().apply {
                    addAll(GestureButton.createPair())
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

    fun expandGestureButtonList(expanded: Boolean, scrollOffset: Int = Int.MAX_VALUE) {
        updateUiState {
            it.copy(isGestureButtonListExpanded = expanded)
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

    fun onGestureButtonEnabledChange(button: GestureButton, enabled: Boolean) {
        updateUiState {
            val buttons = it.gestureButtons
            val index = buttons.indexOf(button)
            if (index < 0) it else {
                val list = buttons.toMutableList().apply {
                    set(index, button.copy(enabled = enabled))
                }
                it.copy(gestureButtons = list)
            }
        }
        saveSettings()
    }

    fun updatePermissionState() {
        viewModelScope.launch {
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
                DataStoreHolder.gestureButtons.updateData {
                    uiState.gestureButtons
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.gestureButtons.data.collectLatest { buttons ->
                updateUiState {
                    it.copy(gestureButtons = buttons.sorted())
                }
            }
        }
    }

    data class UiState(
        val gestureButtons: List<GestureButton> = emptyList(),
        val isGestureEnabled: Boolean = false,
        val isAccessibilityEnabled: Boolean = false,
        val isIgnoringBatteryOptimizations: Boolean = false,
        val isDrawOverlayEnabled: Boolean = false,
        val isPopBackgroundEnabled: Boolean = false,
        val isGestureButtonListExpanded: Boolean = false,
        val showMoreMenu: Boolean = false,
        val showResetWarningDialog: Boolean = false
    )

    sealed interface UiEvent {

        data object ScrollToBottom : UiEvent
        data class ScrollToEvent(val offsetY: Int) : UiEvent
    }
}