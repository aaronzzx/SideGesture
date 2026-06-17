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
    val quickTools: QuickToolsSettings = QuickTools,
    val miniWindow: MiniWindow = MiniWindow()
) {
    @Serializable
    @Keep
    data class MoveScreen(
        val rate: Float = MoveScreenRate,
        val hoverDelayMs: Long = MoveScreenHoverDelayMs,
        val radius: Int = ConvertUtils.dp2px(12f),
        // 显示样式：放大镜(需 Android 11+ 截屏)或准星(Android 7+)。默认放大镜，兼容旧配置
        val style: Style = Style.Magnifier,
        // 悬停弹窗(单击/双击/长按三选项)开关，关闭则抬手直接单击。两种样式共用
        val popupEnabled: Boolean = true
    ) {
        enum class Action {
            Tap, DoubleTap, LongPress
        }

        enum class Style {
            Magnifier, Crosshair
        }
    }

    @Serializable
    @Keep
    data class PreviousApp(val packageNames: List<String> = emptyList())

    @Serializable
    @Keep
    data class GotoBottom(val strength: Int = GotoBottomStrength)

    @Serializable
    @Keep
    data class MiniWindow(
        val mode: MiniWindowMode = MiniWindowMode.Auto,
        // 第三方「小窗助手(com.sunshine.freeform)」广播路径开关
        val useMiWindow: Boolean = false,
        // 竖/横屏各存一套窗口尺寸位置(dp)
        val portrait: Bounds = Bounds(widthDp = 230, heightDp = 380, leftDp = 70, topDp = 250),
        val landscape: Bounds = Bounds(widthDp = 200, heightDp = 300, leftDp = 550, topDp = 30),
        // 横竖屏缩放补偿系数(实际 launchBounds 尺寸 = 设定尺寸 / scale)。
        // null = 按 ROM 自动补偿，兼容旧配置且默认行为不变
        val portraitScale: Float? = null,
        val landscapeScale: Float? = null
    ) {
        @Serializable
        @Keep
        data class Bounds(
            val widthDp: Int,
            val heightDp: Int,
            val leftDp: Int,
            val topDp: Int
        )
    }

    @Serializable
    @Keep
    enum class MiniWindowMode {
        Auto,
        Default,
        Oppo,
        Vivo,
        Huawei,
        Meizu
    }
}
