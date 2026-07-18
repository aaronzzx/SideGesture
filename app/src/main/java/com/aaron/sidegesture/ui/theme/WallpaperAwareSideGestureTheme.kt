package com.aaron.sidegesture.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aaron.sidegesture.event.WallpaperChangedEvent
import com.aaron.sidegesture.ktx.SubscribeEvent

@Composable
fun WallpaperAwareSideGestureTheme(content: @Composable () -> Unit) {
    var themeKey by remember { mutableStateOf(Any()) }
    SubscribeEvent(eventClass = WallpaperChangedEvent::class) {
        themeKey = Any()
    }
    key(themeKey) {
        SideGestureTheme(content)
    }
}
