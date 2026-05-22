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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aaron.sidegesture.entity.About
import com.aaron.sidegesture.entity.ActionPanelStyleSelect
import com.aaron.sidegesture.entity.ActionSelect
import com.aaron.sidegesture.entity.AdjustGestureAngles
import com.aaron.sidegesture.entity.AdvancedSettings
import com.aaron.sidegesture.entity.AnimationStyleSelect
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.AppBlacklist
import com.aaron.sidegesture.entity.BubbleAnimationStyle
import com.aaron.sidegesture.entity.BugCollecting
import com.aaron.sidegesture.entity.CapsuleAnimationStyle
import com.aaron.sidegesture.entity.FolderActionPanelStyle
import com.aaron.sidegesture.entity.GestureButtonSettings
import com.aaron.sidegesture.entity.GestureSettings
import com.aaron.sidegesture.entity.Home
import com.aaron.sidegesture.entity.IconResize
import com.aaron.sidegesture.entity.SectorActionPanelStyle
import com.aaron.sidegesture.entity.Unlock
import com.aaron.sidegesture.entity.WaveAnimationStyle
import com.aaron.sidegesture.ktx.LocalNavController
import com.aaron.sidegesture.ui.screen.about.AboutScreen
import com.aaron.sidegesture.ui.screen.actionpanelstyle.ActionPanelStyleSelectScreen
import com.aaron.sidegesture.ui.screen.actionpanelstyle.folder.FolderActionPanelStyleScreen
import com.aaron.sidegesture.ui.screen.actionpanelstyle.sector.SectorActionPanelStyleScreen
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectScreen
import com.aaron.sidegesture.ui.screen.advancedsettings.AdvancedSettingsScreen
import com.aaron.sidegesture.ui.screen.animationstyle.AnimationStyleSelectScreen
import com.aaron.sidegesture.ui.screen.animationstyle.bubble.BubbleStyleScreen
import com.aaron.sidegesture.ui.screen.animationstyle.capsule.CapsuleStyleScreen
import com.aaron.sidegesture.ui.screen.animationstyle.wave.WaveStyleScreen
import com.aaron.sidegesture.ui.screen.appblacklist.AppBlacklistScreen
import com.aaron.sidegesture.ui.screen.bug.BugScreen
import com.aaron.sidegesture.ui.screen.gestureangles.GestureAnglesScreen
import com.aaron.sidegesture.ui.screen.gesturebuttonsettings.GestureButtonSettingsScreen
import com.aaron.sidegesture.ui.screen.gesturesettings.GestureSettingsScreen
import com.aaron.sidegesture.ui.screen.home.HomeScreen
import com.aaron.sidegesture.ui.screen.iconresize.IconResizeScreen
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
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
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
                        onNavToGestureSettings = { navController.navigate(GestureSettings) },
                        onNavToGestureButtonSettings = { button ->
                            navController.navigate(GestureButtonSettings(button.id, button.position))
                        }
                    )
                }
                myComposable<Unlock> {
                    UnlockScreen(onBack = { navController.navigateUp() })
                }
                myComposable<About> {
                    AboutScreen(
                        onBack = { navController.navigateUp() },
                        onNavToBugCollecting = {
                            navController.navigate(BugCollecting)
                        }
                    )
                }
                myComposable<AdvancedSettings> {
                    AdvancedSettingsScreen(
                        onBack = { navController.navigateUp() },
                        onNavToAppBlacklist = { navController.navigate(AppBlacklist) },
                        onNavToAnimationStyle = {
                            navController.navigate(AnimationStyleSelect)
                        },
                        onNavToActionPanelStyle = {
                            navController.navigate(ActionPanelStyleSelect)
                        }
                    )
                }
                myComposable<ActionPanelStyleSelect> {
                    ActionPanelStyleSelectScreen(
                        onBack = { navController.navigateUp() },
                        onNavToStyleConfig = { type ->
                            when (type) {
                                com.aaron.sidegesture.entity.ActionPanelStyles.TYPE_FOLDER -> {
                                    navController.navigate(FolderActionPanelStyle)
                                }
                                else -> {
                                    navController.navigate(SectorActionPanelStyle)
                                }
                            }
                        }
                    )
                }
                myComposable<AnimationStyleSelect> {
                    AnimationStyleSelectScreen(
                        onBack = { navController.navigateUp() },
                        onNavToStyleConfig = { type ->
                            when (type) {
                                AnimationStyles.TYPE_WAVE -> navController.navigate(WaveAnimationStyle)
                                AnimationStyles.TYPE_CAPSULE -> navController.navigate(CapsuleAnimationStyle)
                                AnimationStyles.TYPE_BUBBLE -> navController.navigate(BubbleAnimationStyle)
                            }
                        }
                    )
                }
                myComposable<GestureSettings> {
                    GestureSettingsScreen(
                        onNavToGestureAngles = { navController.navigate(AdjustGestureAngles) },
                        onBack = { navController.navigateUp() }
                    )
                }
                myComposable<AdjustGestureAngles> {
                    GestureAnglesScreen(onBack = { navController.navigateUp() })
                }
                myComposable<GestureButtonSettings> {
                    GestureButtonSettingsScreen(
                        onBack = { navController.navigateUp() },
                        onNavToActionSelect = { navController.navigate(it) }
                    )
                }
                myComposable<AppBlacklist> {
                    AppBlacklistScreen(onBack = { navController.navigateUp() })
                }
                myComposable<ActionSelect> {
                    ActionSelectScreen(
                        onBack = { navController.navigateUp() },
                        onNavToIconResize = { navController.navigate(it) }
                    )
                }
                myComposable<IconResize> {
                    IconResizeScreen(onBack = { navController.navigateUp() })
                }
                myComposable<BugCollecting> {
                    BugScreen(onBack = { navController.navigateUp() })
                }
                myComposable<WaveAnimationStyle> {
                    WaveStyleScreen(onBack = { navController.navigateUp() })
                }
                myComposable<CapsuleAnimationStyle> {
                    CapsuleStyleScreen(onBack = { navController.navigateUp() })
                }
                myComposable<BubbleAnimationStyle> {
                    BubbleStyleScreen(onBack = { navController.navigateUp() })
                }
                myComposable<FolderActionPanelStyle> {
                    FolderActionPanelStyleScreen(onBack = { navController.navigateUp() })
                }
                myComposable<SectorActionPanelStyle> {
                    SectorActionPanelStyleScreen(onBack = { navController.navigateUp() })
                }
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
        Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)) {
            content(navBackStackEntry)
        }
    }
}

private const val ANIMATION_DURATION_MS = 400
