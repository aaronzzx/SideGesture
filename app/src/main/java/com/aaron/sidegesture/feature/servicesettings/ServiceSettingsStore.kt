package com.aaron.sidegesture.feature.servicesettings

import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.entity.global.InitialSettings
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

data class ServiceSettingsSnapshot(
    val initialSettings: InitialSettings,
    val advancedSettings: AdvancedSettings,
    val gestureSettings: GestureSettings,
    val actionSettings: ActionSettings,
    val buttons: List<GestureButton>
)

class ServiceSettingsStore(
    scope: CoroutineScope,
    snapshotSource: Flow<ServiceSettingsSnapshot> = serviceSettingsSnapshotFlow()
) {

    private val readySnapshot = ReadySnapshot(snapshotSource, scope)
    val snapshot: StateFlow<ServiceSettingsSnapshot?> = readySnapshot.state

    fun currentSnapshotOrNull(): ServiceSettingsSnapshot? = readySnapshot.currentOrNull()

    suspend fun awaitSnapshot(): ServiceSettingsSnapshot = readySnapshot.await()

    class ReadySnapshot<T : Any>(
        source: Flow<T>,
        scope: CoroutineScope
    ) {

        val state: StateFlow<T?> = source.stateIn(scope, SharingStarted.Eagerly, null)

        fun currentOrNull(): T? = state.value

        suspend fun await(): T = state.filterNotNull().first()
    }
}

private fun serviceSettingsSnapshotFlow(): Flow<ServiceSettingsSnapshot> {
    val buttons = DataStoreHolder.sideGestureButtons.data
        .combine(DataStoreHolder.bottomGestureButtons.data) { sideButtons, bottomButtons ->
            sideButtons + bottomButtons
        }
    return combine(
        DataStoreHolder.initialSettings.data,
        DataStoreHolder.advancedSettings.data,
        DataStoreHolder.gestureSettings.data,
        DataStoreHolder.actionSettings.data,
        buttons
    ) { initialSettings, advancedSettings, gestureSettings, actionSettings, buttons ->
        ServiceSettingsSnapshot(
            initialSettings = initialSettings,
            advancedSettings = advancedSettings,
            gestureSettings = gestureSettings,
            actionSettings = actionSettings,
            buttons = buttons
        )
    }
}
