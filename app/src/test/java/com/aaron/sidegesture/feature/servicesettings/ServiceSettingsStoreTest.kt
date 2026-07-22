package com.aaron.sidegesture.feature.servicesettings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import com.aaron.sidegesture.entity.global.RestoreCoordination
import com.aaron.sidegesture.entity.global.RestorePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ServiceSettingsStoreTest {

    @Test
    fun restoreGateHidesRawValueUntilComplete() {
        val blocked = RestoreCoordination(
            generation = 1L,
            phase = RestorePhase.Writing,
            inProgress = true
        )

        assertNull(restoreGatedValue("raw", blocked))
        assertEquals("raw", restoreGatedValue("raw", RestoreCoordination()))
    }

    @Test
    fun storeSnapshotIsNullUntilRealSourceEmits() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val store = ServiceSettingsStore(scope, emptyFlow())

        try {
            assertNull(store.currentSnapshotOrNull())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun readySnapshotAwaitsAndReturnsFirstSourceValue() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val source = Channel<String>(Channel.UNLIMITED)
        val readySnapshot = ServiceSettingsStore.ReadySnapshot(source.receiveAsFlow(), scope)
        val expected = "real snapshot"

        try {
            assertNull(readySnapshot.currentOrNull())

            source.send(expected)

            assertSame(expected, readySnapshot.await())
            assertSame(expected, readySnapshot.currentOrNull())
        } finally {
            source.close()
            scope.cancel()
        }
    }
}
