package com.aaron.sidegesture.ui.screen.quicktools

import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.constant.ActionSettingsDefaults.QuickToolItems
import com.aaron.sidegesture.entity.global.QuickToolItem
import com.aaron.sidegesture.entity.global.QuickToolsSettings
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuickToolsSettingsVM : BaseComposeVM<QuickToolsSettingsVM.UiState, QuickToolsSettingsVM.UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadData()
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        updateUiState {
            val items = it.items.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }
            it.copy(items = items)
        }
        save()
    }

    fun setEnabled(item: QuickToolItem, enabled: Boolean) {
        updateUiState {
            it.copy(
                items = it.items.map { current ->
                    if (current.type == item.type) {
                        current.copy(enabled = enabled)
                    } else {
                        current
                    }
                }
            )
        }
        save()
    }

    fun resetDefault() {
        updateUiState {
            it.copy(items = QuickToolItems)
        }
        save()
    }

    private fun save() {
        viewModelScope.launch {
            val items = uiState.items
            DataStoreHolder.actionSettings.updateData {
                it.copy(quickTools = QuickToolsSettings(items = items))
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            DataStoreHolder.actionSettings.data.collectLatest { settings ->
                updateUiState {
                    it.copy(items = settings.quickTools.items)
                }
            }
        }
    }

    data class UiState(
        val items: List<QuickToolItem> = QuickToolItems
    )

    sealed interface UiEvent
}
