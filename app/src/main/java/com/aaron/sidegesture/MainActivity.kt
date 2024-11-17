package com.aaron.sidegesture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aaron.compose.ktx.roundToPx
import com.aaron.sidegesture.config.GestureAction
import com.aaron.sidegesture.config.GestureActions
import com.aaron.sidegesture.ui.GestureButton
import com.aaron.sidegesture.ui.SideGesturePad
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.blankj.utilcode.util.ToastUtils

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
                ToastUtils.showShort("$action")
            },
            buttons = listOf(
                GestureButton(
                    position = GestureButton.LEFT,
                    width = 80.dp.roundToPx(),
                    pressAction = GestureAction.Single(
                        center = GestureActions.BACK
                    ),
                    longPressAction = GestureAction.Multiple(
                        up = listOf(
                            GestureActions.HOME,
                        ),
                        center = listOf(
                            GestureActions.HOME,
                            GestureActions.RECENT,
                        ),
                        down = listOf(
                            GestureActions.HOME,
                            GestureActions.RECENT,
                            GestureActions.MENU,
                        )
                    )
                ),
                GestureButton(
                    position = GestureButton.RIGHT,
                    width = 80.dp.roundToPx(),
                    pressAction = GestureAction.Single(
                        center = GestureActions.BACK
                    ),
                    longPressAction = GestureAction.Multiple(
                        up = listOf(
                            GestureActions.HOME,
                            GestureActions.RECENT,
                            GestureActions.MENU,
                            GestureActions.MUTE,
                        ),
                        center = listOf(
                            GestureActions.HOME,
                            GestureActions.RECENT,
                            GestureActions.MENU,
                            GestureActions.MUTE,
                            GestureActions.VOLUME_DOWN
                        )
                    )
                )
            )
        )
    }
}