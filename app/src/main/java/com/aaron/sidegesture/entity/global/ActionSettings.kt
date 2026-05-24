package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.ActionSettingsDefaults.GotoBottomStrength
import com.aaron.sidegesture.constant.ActionSettingsDefaults.MoveScreenHoverDelayMs
import com.aaron.sidegesture.constant.ActionSettingsDefaults.MoveScreenRate
import com.aaron.sidegesture.constant.ActionSettingsDefaults.QuickTools
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.serialization.Serializable

/**
 * @author DS-Z
 * @since 2025/6/30
 */
@Serializable
@Keep
data class ActionSettings(
    val moveScreen: MoveScreen = MoveScreen(),
    val previousApp: PreviousApp = PreviousApp(),
    val gotoBottom: GotoBottom = GotoBottom(),
    val quickTools: QuickToolsSettings = QuickTools
) {
    @Serializable
    @Keep
    data class MoveScreen(
        val rate: Float = MoveScreenRate,
        val hoverDelayMs: Long = MoveScreenHoverDelayMs,
        val radius: Int = ConvertUtils.dp2px(12f)
    ) {
        enum class Action {
            Tap, DoubleTap, LongPress
        }
    }

    @Serializable
    @Keep
    data class PreviousApp(val packageNames: List<String> = emptyList())

    @Serializable
    @Keep
    data class GotoBottom(val strength: Int = GotoBottomStrength)
}
