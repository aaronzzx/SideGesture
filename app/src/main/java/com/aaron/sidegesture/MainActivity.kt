package com.aaron.sidegesture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aaron.sidegesture.ktx.gotoAccessibilitySettings
import com.aaron.sidegesture.ui.theme.SideGestureTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideGestureTheme {
                SideSlideContainer()
            }
        }
    }
}

@Composable
private fun SideSlideContainer() {
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