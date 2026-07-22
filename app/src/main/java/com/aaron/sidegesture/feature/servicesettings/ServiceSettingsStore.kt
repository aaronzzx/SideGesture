package com.aaron.sidegesture.feature.servicesettings

import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.entity.global.InitialSettings
import com.aaron.sidegesture.entity.global.RestoreCoordination
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ServiceSettingsSnapshot(
    val initialSettings: InitialSettings,
    val advancedSettings: AdvancedSettings,
    val gestureSettings: GestureSettings,
    val actionSettings: ActionSettings,
    val buttons: List<GestureButton>
)

fun <T> restoreGatedValue(value: T?, coordination: RestoreCoordination): T? {
    return value?.takeUnless { coordination.blocksRuntime }
}

class ServiceSettingsStore(
    scope: CoroutineScope,
    snapshotSource: Flow<ServiceSettingsSnapshot> = serviceSettingsSnapshotFlow(),
    coordinationSource: Flow<RestoreCoordination> = flowOf(RestoreCoordination())
) {

    private val rawReadySnapshot = ReadySnapshot(snapshotSource, scope)
    val rawSnapshot: StateFlow<ServiceSettingsSnapshot?> = rawReadySnapshot.state
    val snapshot: StateFlow<ServiceSettingsSnapshot?> = combine(
        rawSnapshot,
        coordinationSource
    ) { value, coordination ->
        restoreGatedValue(value, coordination)
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun currentSnapshotOrNull(): ServiceSettingsSnapshot? = snapshot.value

    fun currentRawSnapshotOrNull(): ServiceSettingsSnapshot? = rawReadySnapshot.currentOrNull()

    suspend fun awaitSnapshot(): ServiceSettingsSnapshot = snapshot.filterNotNull().first()

    suspend fun awaitRawSnapshot(): ServiceSettingsSnapshot = rawReadySnapshot.await()

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
        .combine(DataStoreHolder.topGestureButtons.data) { buttons, topButtons ->
            buttons + topButtons
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
