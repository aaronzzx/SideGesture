package com.aaron.sidegesture.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaron.sidegesture.constant.DayNightMode
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.ui.theme.generator.AppTheme
import com.aaron.sidegesture.ui.widget.ComposeToast
import com.aaron.sidegesture.utils.DataStoreHolder

@Composable
fun SideGestureTheme(content: @Composable () -> Unit) {
    val advancedSettings by DataStoreHolder
        .advancedSettings
        .data
        .collectAsStateWithLifecycle(initialValue = AdvancedSettings())
    val darkTheme = when (advancedSettings.dayNightMode) {
        DayNightMode.Auto -> isSystemInDarkTheme()
        DayNightMode.Day -> false
        DayNightMode.Night -> true
    }
    val dynamicColor = advancedSettings.dynamicColor
    AppTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    ) {
        content()
        ComposeToast()
    }
}