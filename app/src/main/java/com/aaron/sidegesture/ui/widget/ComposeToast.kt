@file:OptIn(DelicateCoroutinesApi::class)

package com.aaron.sidegesture.ui.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
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
                snackbarHostState.showSnackbar(text)
            }
        }

        SnackbarHost(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            hostState = snackbarHostState
        ) { snackbarData ->
            Surface(shape = CircleShape) {
                Text(
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 16.dp
                    ),
                    text = snackbarData.visuals.message,
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun showComposeToast(@StringRes resId: Int) {
    GlobalScope.launch {
        channel.send(ToastData(resId = resId))
    }
}

fun showComposeToast(text: String) {
    GlobalScope.launch {
        channel.send(ToastData(text = text))
    }
}

private class ToastData(
    @StringRes val resId: Int = 0,
    val text: String = ""
) {
    companion object {
        val None = ToastData()
    }

    val isEmpty: Boolean = resId == 0 && text.isEmpty()
}

private val channel = Channel<ToastData>()