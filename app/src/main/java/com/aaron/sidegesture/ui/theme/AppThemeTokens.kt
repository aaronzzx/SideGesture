package com.aaron.sidegesture.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AppShapes = Shapes()

@Immutable
data class AppTextStyles(
    val topBarTitle: TextStyle = TextStyle(fontSize = 18.sp),
    val searchField: TextStyle = TextStyle(fontSize = 16.sp),
    val dialogTitle: TextStyle = TextStyle(fontSize = 24.sp),
    val dialogProgress: TextStyle = TextStyle(fontSize = 20.sp),
    val toast: TextStyle = TextStyle(fontSize = 14.sp),
    val screenSearchField: TextStyle = TextStyle(fontSize = 18.sp),
    val gestureAngleLabel: TextStyle = TextStyle(fontSize = 18.sp),
    val bugTimestamp: TextStyle = TextStyle(fontSize = 12.sp),
    val taskSwitcherLabel: TextStyle = TextStyle(fontSize = 15.sp),
    val quickToolsPermission: TextStyle = TextStyle(fontSize = 12.sp),
    val quickToolsMedia: TextStyle = TextStyle(fontSize = 15.sp, lineHeight = 16.sp)
)

@Immutable
data class AppComponentShapes(
    val toast: Shape = RoundedCornerShape(16.dp),
    val sliderBubble: Shape = RoundedCornerShape(8.dp),
    val stylePreview: Shape = RoundedCornerShape(14.dp),
    val actionSelectGrid: Shape = RoundedCornerShape(12.dp),
    val updateNotes: Shape = RoundedCornerShape(12.dp),
    val actionPanelMiniWindow: Shape = RoundedCornerShape(8.dp),
    val quickLauncherPanel: Shape = RoundedCornerShape(20.dp),
    val taskSwitcherPanel: Shape = RoundedCornerShape(28.dp),
    val taskSwitcherItem: Shape = RoundedCornerShape(10.dp),
    val quickToolsPanel: Shape = RoundedCornerShape(28.dp),
    val quickToolsSlider: Shape = RoundedCornerShape(18.dp),
    val quickToolsMedia: Shape = RoundedCornerShape(20.dp),
    val quickToolsPermission: Shape = RoundedCornerShape(12.dp),
    val screenshotToolbar: Shape = RoundedCornerShape(20.dp),
    val pinnedScreenshotPanel: Shape = RoundedCornerShape(8.dp)
)

@Immutable
data class AppElevations(
    val overlayTonal: Dp = 3.dp,
    val overlayShadowDark: Dp = 8.dp,
    val overlayShadowLight: Dp = 12.dp,
    val quickToolsTonal: Dp = 0.dp,
    val quickToolsShadowDark: Dp = 10.dp,
    val quickToolsShadowLight: Dp = 14.dp,
    val moveScreenPopup: Dp = 4.dp
)

@Immutable
data class AppMotion(
    val navigationDurationMillis: Int = 400,
    val sliderBubbleResizeDurationMillis: Int = 120,
    val pinnedScreenshotFadeDurationMillis: Int = 140,
    val pinnedScreenshotScaleDurationMillis: Int = 140,
    val settingsContentPlacementStiffness: Float = Spring.StiffnessMediumLow,
    val overlayVisibilityStiffness: Float = Spring.StiffnessMedium,
    val actionPanelPlacementStiffness: Float = Spring.StiffnessHigh,
    val actionPanelSelectionStiffness: Float = Spring.StiffnessHigh,
    val moveScreenPopupStiffness: Float = Spring.StiffnessHigh,
    val actionPanelSecondaryEnterScale: Float = 0.90f,
    val actionPanelEnterScale: Float = 0.92f,
    val actionPanelSelectionScale: Float = 1.15f,
    val moveScreenEnterScale: Float = 0.90f,
    val pinnedScreenshotScale: Float = 1.08f,
    val pinnedScreenshotAlpha: Float = 0.62f,
    val pinnedDeleteActiveScale: Float = 1.12f
)

@Immutable
data class AppAlpha(
    val gestureButton: Float = 0.36f,
    val disabledItem: Float = 0.36f,
    val overlayScrimDark: Float = 0.52f,
    val overlayScrimLight: Float = 0.28f,
    val disabledContent: Float = 0.38f,
    val subtleContainer: Float = 0.08f,
    val subtleBorder: Float = 0.35f,
    val selectedIndicator: Float = 0.30f,
    val previewContainer: Float = 0.16f,
    val previewDivider: Float = 0.90f,
    val lowEmphasis: Float = 0.50f,
    val gestureAngleGuide: Float = 0.10f,
    val miniWindowPreview: Float = 0.15f,
    val updateNotes: Float = 0.40f,
    val screenshotScrim: Float = 0.58f,
    val screenshotToolbar: Float = 0.92f,
    val screenshotDivider: Float = 0.90f,
    val pinnedSurface: Float = 0.32f,
    val pinnedDeleteActive: Float = 0.72f,
    val pinnedDeleteInactive: Float = 0.58f,
    val pinnedContentInactive: Float = 0.90f,
    val quickToolsArtworkIconContainer: Float = 0.54f,
    val quickToolsArtworkGradientStart: Float = 0.50f,
    val quickToolsArtworkGradientEnd: Float = 0.42f,
    val quickToolsDarkPanel: Float = 0.96f,
    val quickToolsDarkRow: Float = 0.94f,
    val quickToolsDarkMedia: Float = 0.98f,
    val quickToolsDarkPrimarySoft: Float = 0.18f,
    val quickToolsDarkTrack: Float = 0.14f,
    val quickToolsLightPrimarySoft: Float = 0.12f,
    val quickToolsLightTrack: Float = 0.12f,
    val moveScreenHalo: Float = 0.60f
)

@Immutable
data class AppColors(
    val fixedBlack: Color,
    val fixedWhite: Color,
    val checkerboardLight: Color,
    val resizeHandle: Color,
    val weChat: Color,
    val aliPay: Color
)

fun appColors(colorScheme: ColorScheme): AppColors {
    return AppColors(
        fixedBlack = Color.Black,
        fixedWhite = Color.White,
        checkerboardLight = Color.LightGray,
        resizeHandle = Color(0xFFA7A7A7),
        weChat = Color(0xFF48C87B),
        aliPay = Color(0xFF16A6EE)
    )
}

val LocalAppTextStyles = staticCompositionLocalOf { AppTextStyles() }
val LocalAppComponentShapes = staticCompositionLocalOf { AppComponentShapes() }
val LocalAppElevations = staticCompositionLocalOf { AppElevations() }
val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
val LocalAppAlpha = staticCompositionLocalOf { AppAlpha() }
val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        fixedBlack = Color.Black,
        fixedWhite = Color.White,
        checkerboardLight = Color.LightGray,
        resizeHandle = Color(0xFFA7A7A7),
        weChat = Color(0xFF48C87B),
        aliPay = Color(0xFF16A6EE)
    )
}

val MaterialTheme.textStyles: AppTextStyles
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTextStyles.current

val MaterialTheme.componentShapes: AppComponentShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalAppComponentShapes.current

val MaterialTheme.elevations: AppElevations
    @Composable
    @ReadOnlyComposable
    get() = LocalAppElevations.current

val MaterialTheme.motion: AppMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalAppMotion.current

val MaterialTheme.alpha: AppAlpha
    @Composable
    @ReadOnlyComposable
    get() = LocalAppAlpha.current

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
