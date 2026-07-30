package com.aaron.sidegesture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

@Immutable
data class AppDimensions(
    val layout: LayoutDimensions = LayoutDimensions(),
    val topBar: TopBarDimensions = TopBarDimensions(),
    val listItem: ListItemDimensions = ListItemDimensions(),
    val slider: SliderDimensions = SliderDimensions(),
    val dialog: DialogDimensions = DialogDimensions(),
    val toast: ToastDimensions = ToastDimensions(),
    val styleCard: StyleCardDimensions = StyleCardDimensions(),
    val actionSelect: ActionSelectDimensions = ActionSelectDimensions(),
    val gestureAngles: GestureAngleDimensions = GestureAngleDimensions(),
    val colorPreview: ColorPreviewDimensions = ColorPreviewDimensions(),
    val iconResize: IconResizeDimensions = IconResizeDimensions(),
    val about: AboutDimensions = AboutDimensions(),
    val quickToolsSettings: QuickToolsSettingsDimensions = QuickToolsSettingsDimensions(),
    val actionPanel: ActionPanelDimensions = ActionPanelDimensions(),
    val quickLauncher: QuickLauncherDimensions = QuickLauncherDimensions(),
    val taskSwitcher: TaskSwitcherDimensions = TaskSwitcherDimensions(),
    val quickTools: QuickToolsDimensions = QuickToolsDimensions(),
    val moveScreen: MoveScreenDimensions = MoveScreenDimensions(),
    val screenshotEditor: ScreenshotEditorDimensions = ScreenshotEditorDimensions(),
    val pinnedScreenshot: PinnedScreenshotDimensions = PinnedScreenshotDimensions(),
    val gestureAnimation: GestureAnimationDimensions = GestureAnimationDimensions(),
    val updateDialog: UpdateDialogDimensions = UpdateDialogDimensions()
)

@Immutable
data class LayoutDimensions(
    val screenPadding: Dp = 12.dp,
    val contentHorizontalPadding: Dp = 12.dp,
    val contentVerticalPadding: Dp = 6.dp,
    val contentVerticalPaddingWithSection: Dp = 12.dp,
    val sectionTitleSpacing: Dp = 8.dp,
    val sectionSpacing: Dp = 24.dp,
    val compactSectionSpacing: Dp = 12.dp,
    val scrollBottomPadding: Dp = 24.dp,
    val nestedItemIndent: Dp = 24.dp,
    val deepNestedItemIndent: Dp = 36.dp,
    val overlayContentPadding: Dp = 12.dp
)

@Immutable
data class TopBarDimensions(
    val contentInset: Dp = 8.dp,
    val leadingInset: Dp = 4.dp,
    val trailingInset: Dp = 4.dp,
    val popupAnchorOffset: Dp = 4.dp,
    val edgeMenuOffset: Dp = 12.dp
)

@Immutable
data class ListItemDimensions(
    val contentGap: Dp = 16.dp,
    val iconTextGap: Dp = 8.dp,
    val titleSupportingTextGap: Dp = 6.dp,
    val dividerSlotHeight: Dp = 24.dp,
    val markerSize: Dp = 16.dp,
    val withSupportingTextMinHeight: Dp = 70.dp,
    val singleLineMinHeight: Dp = 50.dp,
    val minimumTouchTarget: Dp = 48.dp,
    val compactControlVisualSize: Dp = 36.dp,
    val iconSize: Dp = 24.dp,
    val colorDisplaySize: Dp = 30.dp,
    val colorDisplayBorderWidth: Dp = 1.dp
)

@Immutable
data class SliderDimensions(
    val horizontalPadding: Dp = 6.dp,
    val containerHeight: Dp = 30.dp,
    val trackHeight: Dp = 8.dp,
    val thumbSize: Dp = 20.dp,
    val thumbCenterRadius: Dp = 7.dp,
    val bubbleHorizontalPadding: Dp = 10.dp,
    val bubbleVerticalPadding: Dp = 6.dp,
    val bubblePointerWidth: Dp = 12.dp,
    val bubblePointerHeight: Dp = 8.dp,
    val bubblePointerOverlap: Dp = 1.dp,
    val bubbleBodyToThumbClearance: Dp = 40.dp
)

@Immutable
data class DialogDimensions(
    val titlePadding: Dp = 24.dp,
    val contentPadding: Dp = 16.dp,
    val contentGap: Dp = 16.dp,
    val showcaseHeight: Dp = 450.dp,
    val optionSize: Dp = 36.dp,
    val optionBorderWidth: Dp = 1.dp,
    val progressWidth: Dp = 120.dp,
    val actionGap: Dp = 8.dp,
    val previousAppsListHeight: Dp = 280.dp,
    val compactBottomSpacing: Dp = 4.dp
)

@Immutable
data class ToastDimensions(
    val horizontalMargin: Dp = 24.dp,
    val bottomMargin: Dp = 100.dp,
    val maxWidth: Dp = 300.dp,
    val horizontalPadding: Dp = 16.dp,
    val verticalPadding: Dp = 8.dp
)

@Immutable
data class StyleCardDimensions(
    val listSpacing: Dp = 12.dp,
    val selectedBorderWidth: Dp = 1.5.dp,
    val minHeight: Dp = 78.dp,
    val previewSize: Dp = 80.dp,
    val subtitleTopPadding: Dp = 2.dp,
    val radioSize: Dp = 36.dp,
    val radioBorderWidth: Dp = 1.dp,
    val editIconSize: Dp = 20.dp,
    val actionPanelPreviewWidth: Dp = 58.dp,
    val actionPanelPreviewHeight: Dp = 44.dp,
    val previewHorizontalPadding: Dp = 8.dp,
    val previewVerticalPadding: Dp = 6.dp,
    val previewItemSpacing: Dp = 4.dp,
    val previewItemSize: Dp = 8.dp,
    val previewItemRadius: Dp = 4.dp,
    val previewBorderPadding: Dp = 4.dp,
    val capsuleThickness: Dp = 26.dp,
    val capsuleWidth: Dp = 48.dp,
    val capsuleCornerRadius: Dp = 13.dp,
    val capsuleIconSize: Dp = 15.dp,
    val waveIconStartPadding: Dp = 6.dp,
    val previewIconSize: Dp = 18.dp,
    val bubbleDiameter: Dp = 34.dp,
    val bubbleIconStartPadding: Dp = 4.dp
)

@Immutable
data class ActionSelectDimensions(
    val tabHeight: Dp = 48.dp,
    val compactVerticalPadding: Dp = 2.dp,
    val marqueeVelocity: Dp = 50.dp,
    val appIconSize: Dp = 30.dp,
    val accessoryIconSize: Dp = 20.dp,
    val compactItemGap: Dp = 8.dp,
    val nestedIconSize: Dp = 16.dp,
    val gridContentPadding: Dp = 12.dp,
    val gridEndPadding: Dp = 20.dp,
    val dialogMinHeight: Dp = 200.dp,
    val dialogMaxHeight: Dp = 400.dp
)

@Immutable
data class GestureAngleDimensions(
    val topEdgeAnchorRadius: Dp = 4.5.dp,
    val sideEdgeAnchorRadius: Dp = 6.dp,
    val topEdgeIndicatorRadius: Dp = 15.dp,
    val sideEdgeIndicatorRadius: Dp = 20.dp,
    val guideStrokeWidth: Dp = 2.dp,
    val arcTouchExpansion: Dp = 40.dp
)

@Immutable
data class ColorPreviewDimensions(
    val largeSize: Dp = 30.dp,
    val smallSize: Dp = 20.dp,
    val borderWidth: Dp = 1.dp,
    val miniWindowBorderWidth: Dp = 2.dp
)

@Immutable
data class IconResizeDimensions(
    val badgeOffset: Dp = 4.dp,
    val badgeSize: Dp = 16.dp,
    val previewWidth: Dp = 250.dp
)

@Immutable
data class AboutDimensions(
    val logoSize: Dp = 100.dp,
    val linkIconSize: Dp = 24.dp
)

@Immutable
data class QuickToolsSettingsDimensions(
    val dialogMaxHeight: Dp = 560.dp,
    val dragHandleEndPadding: Dp = 4.dp
)

@Immutable
data class ActionPanelDimensions(
    val miniWindowSize: Dp = 200.dp,
    val edgePadding: Dp = 16.dp,
    val cornerSafePadding: Dp = 56.dp,
    val sectorMinItemSize: Dp = 32.dp,
    val textShadowOffset: Dp = 2.dp,
    val textShadowBlurRadius: Dp = 3.dp
)

@Immutable
data class QuickLauncherDimensions(
    val itemHorizontalPadding: Dp = 6.dp,
    val itemVerticalPadding: Dp = 4.dp,
    val itemLabelTopPadding: Dp = 4.dp,
    val gridHorizontalSpacing: Dp = 4.dp,
    val gridVerticalSpacing: Dp = 8.dp,
    val panelPadding: Dp = 12.dp,
    val edgePadding: Dp = 16.dp,
    val pageIndicatorSize: Dp = 8.dp,
    val inactivePageIndicatorSize: Dp = 6.dp,
    val pageIndicatorSpacing: Dp = 8.dp,
    val miniWindowBadgeSize: Dp = 14.dp
)

@Immutable
data class TaskSwitcherDimensions(
    val itemIconSize: Dp = 36.dp,
    val rowHeight: Dp = 48.dp,
    val actionButtonSize: Dp = 36.dp,
    val panelPadding: Dp = 12.dp,
    val panelWidth: Dp = 240.dp,
    val panelMinHeight: Dp = 120.dp,
    val panelMaxHeight: Dp = 300.dp,
    val closeAllHeight: Dp = 52.dp,
    val edgePadding: Dp = 16.dp,
    val sectionBottomPadding: Dp = 4.dp,
    val itemSpacing: Dp = 4.dp,
    val itemVerticalPadding: Dp = 6.dp,
    val itemStartPadding: Dp = 8.dp,
    val itemEndPadding: Dp = 4.dp,
    val closeAllHorizontalPadding: Dp = 12.dp,
    val closeAllIconSize: Dp = 20.dp
)

@Immutable
data class QuickToolsDimensions(
    val panelWidth: Dp = 240.dp,
    val panelHeight: Dp = 300.dp,
    val panelOuterPadding: Dp = 0.dp,
    val panelInnerPadding: Dp = 10.dp,
    val itemSpacing: Dp = 8.dp,
    val compactButtonMaxSize: Dp = 40.dp,
    val edgePadding: Dp = 16.dp,
    val sliderIconContainerSize: Dp = 28.dp,
    val sliderIconSize: Dp = 16.dp,
    val sliderHeight: Dp = 20.dp,
    val sliderHorizontalPadding: Dp = 10.dp,
    val sliderVerticalPadding: Dp = 4.dp,
    val sliderContentGap: Dp = 10.dp,
    val mediaHorizontalPadding: Dp = 8.dp,
    val mediaVerticalPadding: Dp = 6.dp,
    val mediaContentGap: Dp = 6.dp,
    val permissionOuterPadding: Dp = 8.dp,
    val permissionHorizontalPadding: Dp = 10.dp,
    val permissionVerticalPadding: Dp = 5.dp,
    val mediaButtonSize: Dp = 28.dp,
    val mediaPrimaryButtonSize: Dp = 32.dp,
    val mediaIconSize: Dp = 16.dp,
    val mediaPrimaryIconSize: Dp = 18.dp,
    val toolIconSize: Dp = 22.dp,
    val statusDotPadding: Dp = 3.dp,
    val statusDotSize: Dp = 7.dp,
    val defaultIconButtonSize: Dp = 34.dp,
    val defaultIconSize: Dp = 18.dp
)

@Immutable
data class MoveScreenDimensions(
    val magnifierSize: Dp = 80.dp,
    val crosshairLineLength: Dp = 16.dp,
    val crosshairCoreStroke: Dp = 2.dp,
    val crosshairHaloExtraStroke: Dp = 3.5.dp,
    val indicatorRadius: Dp = 22.dp,
    val indicatorGap: Dp = 11.dp,
    val indicatorCoreDotRadius: Dp = 2.5.dp,
    val indicatorHaloExtraRadius: Dp = 1.6.dp,
    val popupWidth: Dp = 70.dp,
    val popupHeight: Dp = 150.dp
)

@Immutable
data class ScreenshotEditorDimensions(
    val edgeMargin: Dp = 12.dp,
    val toolbarGap: Dp = 14.dp,
    val dimTapSlop: Dp = 8.dp,
    val frameStrokeWidth: Dp = 3.dp,
    val frameDashLength: Dp = 12.dp,
    val frameDashGap: Dp = 8.dp,
    val handleRadius: Dp = 8.dp,
    val handleStrokeWidth: Dp = 2.dp,
    val crosshairLineLength: Dp = 14.dp,
    val crosshairStrokeWidth: Dp = 2.dp,
    val toolbarHorizontalPadding: Dp = 8.dp,
    val toolbarVerticalPadding: Dp = 6.dp,
    val toolbarItemSpacing: Dp = 8.dp,
    val dividerHeight: Dp = 24.dp,
    val dividerWidth: Dp = 1.dp
)

@Immutable
data class PinnedScreenshotDimensions(
    val panelCornerRadius: Dp = 8.dp,
    val handleOutset: Dp = 8.dp,
    val imageInset: Dp = 4.dp,
    val resizeTouchTarget: Dp = 24.dp,
    val resizeHandleSize: Dp = 16.dp,
    val handlePadding: Dp = 2.dp,
    val handleStrokeWidth: Dp = 5.dp,
    val deleteTargetWindowHeight: Dp = 144.dp,
    val deleteTargetWidth: Dp = 208.dp,
    val deleteTargetCardHeight: Dp = 88.dp,
    val deleteTargetCornerRadius: Dp = 30.dp,
    val deleteTargetVerticalPadding: Dp = 14.dp,
    val deleteTargetIconSize: Dp = 36.dp
)

@Immutable
data class GestureAnimationDimensions(
    val safeEdgeInset: Dp = 70.dp,
    val safeBezierSpacing: Dp = 40.dp,
    val edgeOverlap: Dp = 1.dp
)

@Immutable
data class UpdateDialogDimensions(
    val notesBottomPadding: Dp = 4.dp,
    val progressTrackHeight: Dp = 8.dp
)

val LocalAppDimensions = staticCompositionLocalOf { AppDimensions() }

val MaterialTheme.dimensions: AppDimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimensions.current
