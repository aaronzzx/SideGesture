package com.aaron.sidegesture.feature.servicesettings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ServiceSettingsStoreTest {

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
