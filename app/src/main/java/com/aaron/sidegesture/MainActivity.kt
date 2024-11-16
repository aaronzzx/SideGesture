package com.aaron.sidegesture

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aaron.sidegesture.config.GestureActions
import com.aaron.sidegesture.ui.GestureButton
import com.aaron.sidegesture.ui.SideGesturePad
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
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFF7F7F7))
    ) {
        SideGesturePad(
            modifier = Modifier.fillMaxSize(),
            onAction = { action ->
                Log.d("zzx", "$action")
            },
            buttons = listOf(
                GestureButton(
                    position = GestureButton.LEFT,
                    start = 0f,
                    end = 0.5f,
                    pressActions = GestureActions(
                        up = GestureActions.BACK,
                        center = GestureActions.HOME,
                        down = GestureActions.MENU
                    ),
                    longPressActions = GestureActions(
                        up = GestureActions.RECENT,
                        center = GestureActions.VOLUME_UP,
                        down = GestureActions.VOLUME_DOWN
                    )
                ),
                GestureButton(
                    position = GestureButton.RIGHT,
                    start = 0f,
                    end = 0.5f,
                    pressActions = GestureActions(
                        up = GestureActions.BACK,
                        center = GestureActions.HOME,
                        down = GestureActions.MENU
                    ),
                    longPressActions = GestureActions(
                        up = GestureActions.RECENT,
                        center = GestureActions.VOLUME_UP,
                        down = GestureActions.VOLUME_DOWN
                    )
                )
            )
        )
    }
}