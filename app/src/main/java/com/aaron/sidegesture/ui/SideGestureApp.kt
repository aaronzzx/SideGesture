package com.aaron.sidegesture.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aaron.sidegesture.ui.theme.SideGestureTheme

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@Composable
fun SideGestureApp() {
    SideGestureTheme {
        val navController = rememberNavController()
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = Home
        ) {
            composable<Home> {
                HomeScreen()
            }
        }
    }
}