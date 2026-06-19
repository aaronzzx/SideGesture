package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.InitialSettingsDefaults.GestureEnabled
import com.aaron.sidegesture.constant.InitialSettingsDefaults.IgnoredUpdateVersion
import com.aaron.sidegesture.constant.InitialSettingsDefaults.MiniWindowVivoShareHintShown
import com.aaron.sidegesture.constant.InitialSettingsDefaults.NotificationPermissionRequested
import com.aaron.sidegesture.constant.InitialSettingsDefaults.Unlocked
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/25
 */
@Serializable
@Keep
data class InitialSettings(
    val gestureEnabled: Boolean = GestureEnabled,
    val unlocked: Boolean = Unlocked,
    // vivo 设备是否已提示过去系统开启小窗分享开关（进入小窗设置页 / 动作选取页勾选应用、快捷方式或「应用小窗」动作，两入口共用，全局只提示一次）
    val miniWindowVivoShareHintShown: Boolean = MiniWindowVivoShareHintShown,
    // 用户在更新弹窗点「忽略此版本」记录的版本号（GitHub release tag），相同版本不再自动弹窗
    val ignoredUpdateVersion: String = IgnoredUpdateVersion,
    // 主进程是否已主动请求过通知权限（首启只问一次，拒绝后不再自动弹，仍可去设置页手动触发）
    val notificationPermissionRequested: Boolean = NotificationPermissionRequested
)
