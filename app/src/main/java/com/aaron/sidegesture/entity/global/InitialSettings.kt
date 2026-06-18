package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.InitialSettingsDefaults.GestureEnabled
import com.aaron.sidegesture.constant.InitialSettingsDefaults.IgnoredUpdateVersion
import com.aaron.sidegesture.constant.InitialSettingsDefaults.MiniWindowVivoShareHintShown
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
    // vivo 设备首次进入小窗设置时，是否已提示过去系统开启小窗分享开关
    val miniWindowVivoShareHintShown: Boolean = MiniWindowVivoShareHintShown,
    // 用户在更新弹窗点「忽略此版本」记录的版本号（GitHub release tag），相同版本不再自动弹窗
    val ignoredUpdateVersion: String = IgnoredUpdateVersion
)
