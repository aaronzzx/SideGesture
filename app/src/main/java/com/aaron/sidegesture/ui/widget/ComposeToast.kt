package com.aaron.sidegesture.ui.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaron.compose.ktx.clipToBackground
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */

@Composable
fun ComposeToast(
    modifier: Modifier = Modifier,
    messages: Flow<ToastMessage> = observeComposeToastMessages()
) {
    Box(modifier = modifier.fillMaxSize()) {
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        var toastData by remember { mutableStateOf(ToastMessage.None) }
        LaunchedEffect(messages) {
            messages.collect { data ->
                if (!data.isEmpty) {
                    toastData = data
                }
            }
        }
        LaunchedEffect(snackbarHostState, toastData) {
            if (!toastData.isEmpty) {
                val text = when (toastData.resId != 0) {
                    true -> context.getString(toastData.resId)
                    else -> toastData.text
                }
                withTimeoutOrNull(toastData.durationMillis) {
                    snackbarHostState.showSnackbar(text)
                }
            }
        }

        SnackbarHost(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 100.dp),
            hostState = snackbarHostState
        ) { snackbarData ->
            Text(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clipToBackground(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(
                        vertical = 8.dp,
                        horizontal = 16.dp
                    )
                    .wrapContentSize(),
                text = snackbarData.visuals.message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun showComposeToast(@StringRes resId: Int, duration: ToastDuration = ToastDuration.Short) {
    toastMessageChannel.trySend(
        ToastMessage(resId = resId, durationMillis = getTimeMillis(duration))
    )
}

fun showComposeToast(text: String, duration: ToastDuration = ToastDuration.Short) {
    toastMessageChannel.trySend(
        ToastMessage(text = text, durationMillis = getTimeMillis(duration))
    )
}

fun observeComposeToastMessages(): Flow<ToastMessage> = toastMessages

private fun getTimeMillis(duration: ToastDuration): Long {
    return when (duration) {
        ToastDuration.Short -> TOAST_SHORT
        ToastDuration.Long -> TOAST_LONG
    }
}

enum class ToastDuration {

    Short, Long
}

class ToastMessage(
    @StringRes val resId: Int = 0,
    val text: String = "",
    val durationMillis: Long = TOAST_SHORT
) {
    companion object {
        val None = ToastMessage()
    }

    val isEmpty: Boolean = resId == 0 && text.isEmpty()
}

private const val TOAST_SHORT = 2000L
private const val TOAST_LONG = 3500L

private val toastMessageChannel = Channel<ToastMessage>(Channel.UNLIMITED)
private val toastMessages = toastMessageChannel.receiveAsFlow()
