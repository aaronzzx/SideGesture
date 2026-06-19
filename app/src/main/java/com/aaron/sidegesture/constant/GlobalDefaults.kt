@file:Suppress("ConstPropertyName")

package com.aaron.sidegesture.constant

import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.GestureAngles
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.VibrationEffects
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_ARROW
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/12
 */

object AdvancedSettingsDefaults {

    val ExcludeApps = emptyList<String>()
    val AnimationStyles = AnimationStyles()
    val ActionPanelStyles = ActionPanelStyles()
    const val VolumeButtonSwitchSong = false
    const val FitSoftKeyboard = true
    const val ActionPanelAppLongPressLaunchPopup = false
    const val ActionPanelAppSwitchWindowModeDelayMs = 500L
    const val HideLandscape = false
    const val HideQuickPanel = false
    const val HideScreenLock = false
    const val HideHomeScreen = false
    const val ExcludeFromRecents = false
    const val DynamicColor = false
    val DayNightMode = com.aaron.sidegesture.entity.DayNightMode.Auto
    val ClipApps = emptyMap<String, Float>()
    val ClipShortcuts = emptyMap<String, Float>()
    const val AutoCheckUpdate = true
}

object GestureSettingsDefaults {

    val Angles = GestureAngles()
    val SlideTriggerDistance = ConvertUtils.dp2px(30f)
    val LongSlideTriggerDistance = ConvertUtils.dp2px(100f)
    const val LongPressTriggerDelayMs = 250L
    const val LongSlideTriggerImmediately = true
    const val LongSlideTriggerDelayMs = 100L
    const val IsCustomVibration = false
    const val IsPreciseSlideType = true
    val Vibrations = Vibrations()
}

object InitialSettingsDefaults {

    const val GestureEnabled = true
    const val Unlocked = false
    const val MiniWindowVivoShareHintShown = false
    const val IgnoredUpdateVersion = ""
    const val NotificationPermissionRequested = false
}

object ActionPanelStylesDefaults {

    const val TYPE_ARC = 1
    const val TYPE_SECTOR = 2
    const val TYPE_FOLDER = 3

    const val Type = TYPE_SECTOR
    val ArcStyleItemSize = ConvertUtils.dp2px(48f)
    val SectorStyleItemSize = ConvertUtils.dp2px(40f)
    const val SectorStyleInitialRadiusRatio = 1.5f
    const val SectorStyleItemSpacingRatio = 1.12f
    val FolderStyleItemSize = ConvertUtils.dp2px(40f)
    const val FolderStyleColumns = 4
    const val FolderStyleRows = 3
    val FolderStyleItemSpacing = ConvertUtils.dp2px(12f)
    val FolderStyleHorizontalPadding = ConvertUtils.dp2px(16f)
    val FolderStyleVerticalPadding = ConvertUtils.dp2px(16f)
    val FolderStyleCornerRadius = ConvertUtils.dp2px(22f)
    const val FolderStyleScrollSpeed = 12
    val FolderStyleScrollHotZoneHeight = ConvertUtils.dp2px(28f)
}

object AnimationStylesDefaults {

    const val TYPE_WAVE = 1
    const val TYPE_CAPSULE = 2
    const val TYPE_BUBBLE = 3

    const val Type = TYPE_WAVE
    const val IsAnimationEnabled = true
    const val WaveStyleBackgroundColor = android.graphics.Color.BLACK
    const val WaveStyleStrokeColor = android.graphics.Color.TRANSPARENT
    const val WaveStyleStrokeWidth = 0
    val WaveStyleWidth = ConvertUtils.dp2px(40f)
    const val WaveStyleBezierLengthHalfRatio = 2.5f
    const val WaveStyleSafeBounds = true
    const val WaveStyleTransformEnabled = true
    val WaveStyleIconColor = android.graphics.Color.argb(200, 255, 255, 255)
    const val WaveStyleIconScale = 0.6f
    const val WaveStyleIconType = ICON_TYPE_ARROW
    val CapsuleStyleBackgroundColor = android.graphics.Color.argb(220, 18, 18, 18)
    const val CapsuleStyleStrokeColor = android.graphics.Color.TRANSPARENT
    const val CapsuleStyleStrokeWidth = 0
    val CapsuleStyleThickness = ConvertUtils.dp2px(36f)
    val CapsuleStyleMaxLength = ConvertUtils.dp2px(72f)
    val CapsuleStyleCornerRadius = ConvertUtils.dp2px(18f)
    val CapsuleStyleIconColor = android.graphics.Color.argb(220, 255, 255, 255)
    const val CapsuleStyleIconScale = 0.52f
    const val CapsuleStyleIconType = ICON_TYPE_ARROW
    val BubbleStyleBackgroundColor = android.graphics.Color.argb(220, 22, 22, 22)
    val BubbleStyleStrokeColor = android.graphics.Color.argb(36, 255, 255, 255)
    const val BubbleStyleStrokeWidth = 0
    val BubbleStyleDiameter = ConvertUtils.dp2px(44f)
    val BubbleStyleMaxOffset = ConvertUtils.dp2px(72f)
    val BubbleStyleIconColor = android.graphics.Color.argb(232, 255, 255, 255)
    const val BubbleStyleIconScale = 0.52f
    const val BubbleStyleIconType = ICON_TYPE_ARROW
}

object ScaleableDefaults {

    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 2.0f
    const val DEFAULT_SCALE = 1f
}

object GestureActionsDefaults {

    val Center = emptyList<Action>()
    val Up = emptyList<Action>()
    val Down = emptyList<Action>()
    val Center2 = emptyList<Action>()
    val Up2 = emptyList<Action>()
    val Down2 = emptyList<Action>()
    val Click = emptyList<Action>()
    const val ActionValue = GlobalActions.NONE
    val ActionNone = Action(value = ActionValue, data = "")
}

object GestureAnglesDefaults {

    val Left = GestureAngle()
    val Right = GestureAngle()
    val Bottom = GestureAngle(0.12f, 0.40f, 0.60f, 0.88f)
    const val P1 = 0.12f
    const val P2 = 0.40f
    const val P3 = 0.70f
    const val P4 = 0.88f
}

object VibrationDefaults {

    const val SlideEnabled = true
    const val LongSlideEnabled = true
    const val ActionPanelEnabled = true
    const val MoveScreenEnabled = true
    val PredefinedEffect = VibrationEffects.Click
    const val CustomVibrationMs = 50L
    const val VibrateImmediately = false
}

object GestureButtonDefaults {

    const val ID_DEFAULT = "1"
    const val Enabled = true
    const val Start = 0.0f
    const val End = 0.1f
    val Width = ConvertUtils.dp2px(16f)
    val Angle = GestureAngle()
    val SlideActions = GestureActions()
    val LongSlideActions = GestureActions()
    val SlideTriggerDistance = GestureSettingsDefaults.SlideTriggerDistance
    val LongSlideTriggerDistance = GestureSettingsDefaults.LongSlideTriggerDistance
    const val LongSlideTriggerImmediately = GestureSettingsDefaults.LongSlideTriggerImmediately
    const val LongSlideTriggerDelayMs = GestureSettingsDefaults.LongSlideTriggerDelayMs
    val Vibrations = GestureSettingsDefaults.Vibrations
    const val Color = android.graphics.Color.TRANSPARENT
    const val AlignRegion = true
    const val ExcludeSystemGestureRects = false
    const val LimitMaxExcludeSystemGestureLength = true
    val SideDefaults = listOf(
        GestureButton(
            id = ID_DEFAULT,
            position = Position.Left,
            start = 0.0f,
            end = 1.0f,
            slideActions = GestureActions(center = Action.toList(GlobalActions.BACK))
        ),
        GestureButton(
            id = ID_DEFAULT,
            position = Position.Right,
            start = 0.0f,
            end = 1.0f,
            slideActions = GestureActions(center = Action.toList(GlobalActions.BACK))
        )
    )
    val BottomDefaults = listOf(
        GestureButton(
            id = ID_DEFAULT,
            position = Position.Bottom,
            enabled = false,
            start = 0.0f,
            end = 1.0f,
            slideActions = GestureActions(center = Action.toList(GlobalActions.HOME)),
            longSlideActions = GestureActions(center = Action.toList(GlobalActions.RECENT))
        )
    )
}

object ActionSettingsDefaults {

    const val MoveScreenRate = 2f
    const val MoveScreenHoverDelayMs = 600L
    const val GotoBottomStrength = 10
    const val ShellCommandTimeoutMs = 10_000L
    const val ShellCommandMaxOutputLength = 4096
    val QuickToolItems = listOf(
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.MediaControl),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.Volume),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.Brightness),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.Wifi),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.Bluetooth),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.Flashlight),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.Mute),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.NotificationPanel),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.QuickSettingsPanel),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.LockScreen),
        com.aaron.sidegesture.entity.global.QuickToolItem(com.aaron.sidegesture.entity.global.QuickToolType.Screenshot)
    )
    val QuickTools = com.aaron.sidegesture.entity.global.QuickToolsSettings()
}
