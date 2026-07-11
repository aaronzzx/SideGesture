package com.aaron.sidegesture.feature.servicesettings

import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.entity.global.InitialSettings
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class ServiceSettingsStore(
    scope: CoroutineScope
) {

    val initialSettings: StateFlow<InitialSettings> = DataStoreHolder.initialSettings.data
        .stateIn(scope, SharingStarted.Eagerly, InitialSettings())

    val advancedSettings: StateFlow<AdvancedSettings> = DataStoreHolder.advancedSettings.data
        .stateIn(scope, SharingStarted.Eagerly, AdvancedSettings())

    val gestureSettings: StateFlow<GestureSettings> = DataStoreHolder.gestureSettings.data
        .stateIn(scope, SharingStarted.Eagerly, GestureSettings())

    val actionSettings: StateFlow<ActionSettings> = DataStoreHolder.actionSettings.data
        .stateIn(scope, SharingStarted.Eagerly, ActionSettings())

    val buttons: StateFlow<List<GestureButton>> = DataStoreHolder.sideGestureButtons.data
        .combine(DataStoreHolder.bottomGestureButtons.data) { sideButtons, bottomButtons ->
            sideButtons + bottomButtons
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
}
