package com.aaron.sidegesture

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
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
                myEnableEdgeToEdge(item.dayNightMode == DayNightMode.Night)
            }
        }
    }
}

private fun ComponentActivity.myEnableEdgeToEdge(isAppNightMode: Boolean = false) {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { resources ->
            val flag = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            flag == Configuration.UI_MODE_NIGHT_YES || isAppNightMode
        },
        navigationBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim) { resources ->
            val flag = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            flag == Configuration.UI_MODE_NIGHT_YES || isAppNightMode
        }
    )
}

private val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)