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
    const val HideLandscape = false
    const val HideQuickPanel = false
    const val HideScreenLock = false
    const val HideHomeScreen = false
    const val HideTemporary = false
    const val ExcludeFromRecents = false
    const val DynamicColor = false
    val DayNightMode = com.aaron.sidegesture.entity.DayNightMode.Auto
    val ClipApps = emptyMap<String, Float>()
}

object GestureSettingsDefaults {

    val Angles = GestureAngles()
    val SlideTriggerDistance = ConvertUtils.dp2px(30f)
    val LongSlideTriggerDistance = ConvertUtils.dp2px(100f)
    const val LongSlideTriggerImmediately = true
    const val LongSlideTriggerDelayMs = 100L
    const val IsCustomVibration = false
    val Vibrations = Vibrations()
}

object InitialSettingsDefaults {

    const val GestureEnabled = true
    const val Unlocked = false
}

object ActionPanelStylesDefaults {

    const val TYPE_ARC = 1

    const val Type = TYPE_ARC
    val ArcStyleItemSize = ConvertUtils.dp2px(48f)
}

object AnimationStylesDefaults {

    const val TYPE_WAVE = 1

    const val Type = TYPE_WAVE
    const val IsAnimationEnabled = true
    const val WaveStyleBackgroundColor = android.graphics.Color.BLACK
    const val WaveStyleStrokeColor = android.graphics.Color.TRANSPARENT
    const val WaveStyleStrokeWidth = 0
    val WaveStyleIconColor = android.graphics.Color.argb(200, 255, 255, 255)
}

object AppInfoDefaults {

    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 1.5f
    const val DEFAULT_SCALE = 1f
}

object GestureActionsDefaults {

    val Center = emptyList<Action>()
    val Up = emptyList<Action>()
    val Down = emptyList<Action>()
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
    const val VibrateImmediately = true
    val PredefinedEffect = VibrationEffects.Click
    const val CustomVibrationMs = 50L
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
            slideActions = GestureActions(center = Action.toList(GlobalActions.HOME))
        )
    )
}