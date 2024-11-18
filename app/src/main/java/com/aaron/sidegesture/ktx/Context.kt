package com.aaron.sidegesture.ktx

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun Context.gotoAccessibilitySettings() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    try {
        startActivity(intent)
    } catch (ignored: Exception) {
        intent.action = Settings.ACTION_SETTINGS
        startActivity(intent)
    }
}

fun Context.isAccessibilitySettingsOn(clazz: Class<out AccessibilityService?>): Boolean {
    // 判断设备的无障碍功能是否可用
    var accessibilityEnabled = false
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        ) == 1
    } catch (e: Settings.SettingNotFoundException) {
        e.printStackTrace()
    }
    // 创建一个字符串拆分工具实例
    val mStringColonSplitter = TextUtils.SimpleStringSplitter(':')
    if (accessibilityEnabled) {
        // 获取启用的无障碍服务
        val settingValue: String? = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            // 迭代判断是否包含我们的服务
            mStringColonSplitter.setString(settingValue)
            while (mStringColonSplitter.hasNext()) {
                val accessibilityService = mStringColonSplitter.next()
                if (accessibilityService.equals("${packageName}/${clazz.canonicalName}", ignoreCase = true))
                    return true
            }
        }
    }
    return false
}