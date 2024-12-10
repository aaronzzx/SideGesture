@file:OptIn(DelicateCoroutinesApi::class)

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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */

@Composable
fun ComposeToast(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        var toastData by remember { mutableStateOf(ToastData.None) }
        LaunchedEffect(key1 = Unit) {
            for (data in channel) {
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
                launch {
                    snackbarHostState.showSnackbar(text)
                }
                delay(toastData.duration)
                snackbarHostState.currentSnackbarData?.dismiss()
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
    GlobalScope.launch {
        channel.send(ToastData(resId = resId, duration = getTimeMillis(duration)))
    }
}

fun showComposeToast(text: String, duration: ToastDuration = ToastDuration.Short) {
    GlobalScope.launch {
        channel.send(ToastData(text = text, duration = getTimeMillis(duration)))
    }
}

private fun getTimeMillis(duration: ToastDuration): Long {
    return when (duration) {
        ToastDuration.Short -> TOAST_SHORT
        ToastDuration.Long -> TOAST_LONG
    }
}

enum class ToastDuration {

    Short, Long
}

private class ToastData(
    @StringRes val resId: Int = 0,
    val text: String = "",
    val duration: Long = TOAST_SHORT
) {
    companion object {
        val None = ToastData()
    }

    val isEmpty: Boolean = resId == 0 && text.isEmpty()
}

private const val TOAST_SHORT = 2000L
private const val TOAST_LONG = 3500L

private val channel = Channel<ToastData>()