package com.aaron.sidegesture.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aaron.sidegesture.ui.screen.about.About
import com.aaron.sidegesture.ui.screen.about.AboutScreen
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettings
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsScreen
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAngles
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAnglesScreen
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettings
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettingsScreen
import com.aaron.sidegesture.ui.screen.home.Home
import com.aaron.sidegesture.ui.screen.home.HomeScreen
import com.aaron.sidegesture.ui.screen.unlock.Unlock
import com.aaron.sidegesture.ui.screen.unlock.UnlockScreen
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import kotlin.reflect.KType

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@Composable
fun SideGestureApp() {
    SideGestureTheme {
        val navController = rememberNavController()
        val durationMs = ANIMATION_DURATION_MS
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = Home,
            enterTransition = {
                slideInHorizontally(animationSpec = tween(durationMs)) { it }
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(durationMs)) { -it / 3 }
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(durationMs)) { -it / 3 }
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(durationMs)) { it }
            }
        ) {
            myComposable<Home> {
                HomeScreen(
                    onNavToUnlock = { navController.navigate(Unlock) },
                    onNavToAbout = { navController.navigate(About) },
                    onNavToAdvancedSettings = { navController.navigate(AdvancedSettings) },
                    onNavToGestureSettings = { navController.navigate(GestureSettings) }
                )
            }
            myComposable<Unlock> {
                UnlockScreen(onBack = { navController.navigateUp() })
            }
            myComposable<About> {
                AboutScreen(onBack = { navController.navigateUp() })
            }
            myComposable<AdvancedSettings> {
                AdvancedSettingsScreen(onBack = { navController.navigateUp() })
            }
            myComposable<GestureSettings> {
                GestureSettingsScreen(
                    onNavToGestureAngles = { navController.navigate(GestureAngles) },
                    onBack = { navController.navigateUp() }
                )
            }
            myComposable<GestureAngles> {
                GestureAnglesScreen(onBack = { navController.navigateUp() })
            }
        }
    }
}

private inline fun <reified T : Any> NavGraphBuilder.myComposable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    EnterTransition?)? =
        null,
    noinline exitTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    ExitTransition?)? =
        null,
    noinline popEnterTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    EnterTransition?)? =
        enterTransition,
    noinline popExitTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    ExitTransition?)? =
        exitTransition,
    noinline sizeTransform:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    SizeTransform?)? =
        null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        sizeTransform = sizeTransform
    ) { navBackStackEntry ->
        Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
            content(navBackStackEntry)
        }
    }
}

private const val ANIMATION_DURATION_MS = 400