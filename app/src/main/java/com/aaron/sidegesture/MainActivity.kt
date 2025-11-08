package com.aaron.sidegesture

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.aaron.sidegesture.entity.DayNightMode
import com.aaron.sidegesture.ui.SideGestureApp
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        myEnableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SideGestureApp()
        }

        lifecycleScope.launch {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            DataStoreHolder.advancedSettings.data.collectLatest { item ->
                am.appTasks.firstOrNull()?.setExcludeFromRecents(item.excludeFromRecents)
                myEnableEdgeToEdge(item.dayNightMode)
            }
        }
    }
}

private fun ComponentActivity.myEnableEdgeToEdge(dayNightMode: DayNightMode = DayNightMode.Auto) {
    val block: (Resources) -> Boolean = block@{ resources ->
        if (dayNightMode != DayNightMode.Auto) {
            return@block when (dayNightMode) {
                DayNightMode.Night -> true
                else -> false
            }
        }
        val flags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        flags == Configuration.UI_MODE_NIGHT_YES
    }
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT, block),
        navigationBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim, block)
    )
}

private val DefaultLightScrim = Color.argb(0xBF, 0xFF, 0xFF, 0xFF)
private val DefaultDarkScrim = Color.argb(0xBF, 0x00, 0x00, 0x00)