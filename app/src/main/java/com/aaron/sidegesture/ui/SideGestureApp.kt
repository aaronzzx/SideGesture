package com.aaron.sidegesture.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aaron.sidegesture.ui.screen.about.About
import com.aaron.sidegesture.ui.screen.about.AboutScreen
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettings
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsScreen
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettings
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsScreen
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettings
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettingsScreen
import com.aaron.sidegesture.ui.screen.home.Home
import com.aaron.sidegesture.ui.screen.home.HomeScreen
import com.aaron.sidegesture.ui.screen.unlock.Unlock
import com.aaron.sidegesture.ui.screen.unlock.UnlockScreen
import com.aaron.sidegesture.ui.theme.SideGestureTheme

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@Composable
fun SideGestureApp() {
    SideGestureTheme {
        Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
            val navController = rememberNavController()
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = Home,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                composable<Home> {
                    HomeScreen(
                        onNavToUnlock = { navController.navigate(Unlock) },
                        onNavToAbout = { navController.navigate(About) },
                        onNavToAdvancedSettings = { navController.navigate(AdvancedSettings) },
                        onNavToGestureSettings = { navController.navigate(GestureSettings) },
                        onNavToGestureButtonSettings = { navController.navigate(GestureButtonSettings) }
                    )
                }
                composable<Unlock> {
                    UnlockScreen(onBack = { navController.navigateUp() })
                }
                composable<About> {
                    AboutScreen(onBack = { navController.navigateUp() })
                }
                composable<AdvancedSettings> {
                    AdvancedSettingsScreen(onBack = { navController.navigateUp() })
                }
                composable<GestureSettings> {
                    GestureSettingsScreen(onBack = { navController.navigateUp() })
                }
                composable<GestureButtonSettings> {
                    GestureButtonSettingsScreen(onBack = { navController.navigateUp() })
                }
            }
        }
    }
}