package com.aaron.sidegesture.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aaron.sidegesture.ktx.gotoAccessibilitySettings
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@Serializable
data object Home

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        Button(
            onClick = { context.gotoAccessibilitySettings() }
        ) {
            Text(text = "跳转无障碍服务")
        }
    }
}