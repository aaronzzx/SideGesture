package com.aaron.sidegesture.feature.toast

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.ui.widget.ToastMessage
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiceToastOverlayInstrumentedTest {

    @Test
    fun latestToastReplacesVisibleWindowAndStartIsIdempotent() = runBlocking {
        val events = CopyOnWriteArrayList<String>()
        val messages = Channel<ToastMessage>(Channel.UNLIMITED)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val lifecycle = ToastOverlayLifecycle(
            scope = scope,
            messages = messages.receiveAsFlow(),
            onShow = { message -> events += "show:${message.text}" },
            onHide = { events += "hide" }
        )

        try {
            lifecycle.start()
            lifecycle.start()
            messages.send(ToastMessage(text = "first", durationMillis = 5_000L))
            waitUntil { events == listOf("show:first") }

            messages.send(ToastMessage(text = "second", durationMillis = 30L))
            waitUntil {
                events == listOf(
                    "show:first",
                    "hide",
                    "show:second",
                    "hide"
                )
            }

            lifecycle.release()
            messages.send(ToastMessage(text = "ignored", durationMillis = 30L))
            delay(50L)

            assertEquals(
                listOf("show:first", "hide", "show:second", "hide"),
                events
            )
        } finally {
            lifecycle.release()
            messages.close()
            scope.cancel()
        }
    }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        withTimeout(2_000L) {
            while (!predicate()) {
                delay(5L)
            }
        }
    }
}
