package com.aaron.sidegesture.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionManagerTest {

    @Test
    fun startCollectsProducersOnlyOnce() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val producer = TestProducer()
        val manager = ActionManager(listOf(producer), scope)

        try {
            assertEquals(0, producer.collectionCount)

            manager.start()
            manager.start()
            yield()

            assertEquals(1, producer.collectionCount)
        } finally {
            scope.cancel()
        }
    }

    private class TestProducer : ActionHandler, ActionRequestProducer {

        override val supportedActions = setOf("test")
        var collectionCount = 0

        override val flow: Flow<ActionRequest> = flow {
            collectionCount++
            awaitCancellation()
        }

        override suspend fun handle(request: ActionRequest) = Unit
    }
}
