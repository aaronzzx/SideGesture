package com.aaron.sidegesture

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.aaron.sidegesture.entity.DayNightMode
import com.aaron.sidegesture.ui.SideGestureApp
import com.aaron.sidegesture.ui.update.UpdateViewModel
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 与 SideGestureApp 内 viewModel() 同为 Activity 作用域，解析到同一实例
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        myEnableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SideGestureApp()
        }

        // 冷启动入口（含通知拉起）：读状态评估是否调起更新弹窗
        updateViewModel.onEntry()

        lifecycleScope.launch {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            DataStoreHolder.advancedSettings.data.collectLatest { item ->
                am.appTasks.firstOrNull()?.setExcludeFromRecents(item.excludeFromRecents)
                myEnableEdgeToEdge(item.dayNightMode)
            }
        }
    }

    // singleTask 复用实例时不走 onCreate，这里重新评估状态（通知再次点击等）
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateViewModel.onEntry()
    }

    // 从最近任务返回（无新 Intent）时浮出下载完成/失败入口，但不重复弹「有新版」
    override fun onResume() {
        super.onResume()
        updateViewModel.onForeground()
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