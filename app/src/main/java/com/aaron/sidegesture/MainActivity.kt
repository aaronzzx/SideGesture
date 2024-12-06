package com.aaron.sidegesture

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.aaron.sidegesture.ui.SideGestureApp
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            SideGestureApp()
        }

        lifecycleScope.launch {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            DataStoreHolder.advancedSettings.data.collectLatest {
                am.appTasks.firstOrNull()?.setExcludeFromRecents(it.excludeFromRecents)
            }
        }
    }
}