package com.aaron.sidegesture.action

import com.aaron.sidegesture.entity.Action
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ActionDispatcherTest {

    @Test
    fun constructorRejectsHandlerWithNoSupportedActions() {
        try {
            ActionDispatcher(listOf(FakeActionHandler(emptySet())))
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun constructorRejectsDuplicateActionMapping() {
        try {
            ActionDispatcher(
                listOf(
                    FakeActionHandler(setOf("duplicate")),
                    FakeActionHandler(setOf("duplicate"))
                )
            )
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun dispatchReturnsFalseWithoutInvokingHandlerForUnknownAction() = runBlocking {
        val handler = FakeActionHandler(setOf("known"))
        val dispatcher = ActionDispatcher(listOf(handler))

        val dispatched = dispatcher.dispatch(ActionRequest(Action("unknown")))

        assertFalse(dispatched)
        assertTrue(handler.requests.isEmpty())
    }

    @Test
    fun dispatchForwardsExactRequestAndReturnsTrueForKnownAction() = runBlocking {
        val handler = FakeActionHandler(setOf("known"))
        val dispatcher = ActionDispatcher(listOf(handler))
        val request = ActionRequest(Action("known", "payload"))

        val dispatched = dispatcher.dispatch(request)

        assertTrue(dispatched)
        assertSame(request, handler.requests.single())
    }

    private class FakeActionHandler(
        override val supportedActions: Set<String>
    ) : ActionHandler {

        val requests = mutableListOf<ActionRequest>()

        override suspend fun handle(request: ActionRequest) {
            requests += request
        }
    }
}
