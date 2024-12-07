package com.aaron.sidegesture.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.aaron.sidegesture.ui.theme.generator.AppTheme
import com.aaron.sidegesture.ui.widget.ComposeToast

@Composable
fun SideGestureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    AppTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    ) {
        content()
        ComposeToast()
    }
}