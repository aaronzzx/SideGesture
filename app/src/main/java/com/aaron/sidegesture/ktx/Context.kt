package com.aaron.sidegesture.ktx

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.LauncherInfo
import com.aaron.sidegesture.utils.MiniWindowUtils
import com.aaron.sidegesture.utils.showToast
import com.blankj.utilcode.util.AppUtils


/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun Context.gotoIgnoreBatteryOptimizations(): Boolean {
    return try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        true
    } catch (ignored: Exception) {
        showToast(R.string.please_enable_ignoring_battery_optimizations_by_yourself)
        false
    }
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}

fun Context.launchAssist(): Boolean {
    return try {
        val intent = Intent().apply {
            setAction(Intent.ACTION_ASSIST)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        true
    } catch (ignored: Exception) {
        showToast(R.string.launch_assist_failed)
        false
    }
}

fun Context.launchShortcutInfo(shortcutInfo: LauncherInfo.ShortcutInfo): Boolean {
    return try {
        val intents = shortcutInfo
            .intents
            .map {
                Intent.parseUri(it, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            .toTypedArray()
        startActivities(intents)
        true
    } catch (ignored: Exception) {
        showToast(getString(R.string.launch_shortcut_info_failed, shortcutInfo.label))
        false
    }
}

fun Context.launchAppInfo(appInfo: AppInfo, miniWindow: Boolean = appInfo.miniWindow): Boolean {
    return try {
        val intent = Intent().apply {
            setClassName(appInfo.packageName, appInfo.className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        var launchSucceed = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && miniWindow) {
            launchSucceed = MiniWindowUtils.startActivity(this, appInfo.componentName, false)
        }
        if (!launchSucceed) {
            startActivity(intent)
        }
        true
    } catch (ignored: ActivityNotFoundException) {
        showToast(getString(R.string.launch_app_info_failed, appInfo.label))
        false
    }
}

fun Context.launchApp(packageName: String, className: String): Boolean {
    return try {
        val intent = Intent().apply {
            setClassName(packageName, className)
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        true
    } catch (ignored: ActivityNotFoundException) {
        showToast(R.string.launch_app_failed)
        false
    }
}

fun Context.volumeUp() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
}

fun Context.volumeDown() {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
}

fun Context.toggleMute() {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (!notificationManager.isNotificationPolicyAccessGranted) {
        showToast(R.string.goto_grant_notification_policy_access_permission)
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        return
    }
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val ringerMode = audioManager.ringerMode
    val newRingerMode = when (ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_NORMAL
        else -> AudioManager.RINGER_MODE_SILENT
    }
    audioManager.ringerMode = newRingerMode
}

fun Context.dispatchMediaKeyEvent(keycode: Int) {
    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val down = KeyEvent(KeyEvent.ACTION_DOWN, keycode)
    audioManager.dispatchMediaKeyEvent(down)
    val up = KeyEvent(KeyEvent.ACTION_UP, keycode)
    audioManager.dispatchMediaKeyEvent(up)
}

fun Context.gotoWechat(): Boolean {
    return try {
        val intent = Intent().apply {
            setAction("com.tencent.mm.action.BIZSHORTCUT")
            addCategory(Intent.CATEGORY_DEFAULT)
            setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        true
    } catch (exception: ActivityNotFoundException) {
        showToast(R.string.goto_wechat_failed)
        false
    }
}

fun Context.gotoWechatScan(): Boolean {
    val intent = packageManager.getLaunchIntentForPackage("com.tencent.mm")
    return if (intent != null &&
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    ) {
        intent.putExtra("LauncherUI.From.Scaner.Shortcut", true)
        intent.setAction("android.intent.action.VIEW")
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
            true
        } catch (exception: ActivityNotFoundException) {
            showToast(R.string.goto_wechat_failed)
            false
        }
    } else {
        showToast(R.string.goto_wechat_failed)
        false
    }
}

fun Context.gotoAlipayScan(): Boolean {
    return try {
        //利用Intent打开支付宝
        //支付宝跳过开启动画打开扫码和付款码的url scheme分别是alipayqr://platformapi/startapp?saId=10000007和
        //alipayqr://platformapi/startapp?saId=20000056
        val uri = Uri.parse("alipayqr://platformapi/startapp?saId=10000007")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        true
    } catch (e: Exception) {
        showToast(R.string.goto_alipay_failed)
        false
    }
}

fun Context.gotoAlipayPayCode(): Boolean {
    return try {
        //利用Intent打开支付宝
        //支付宝跳过开启动画打开扫码和付款码的url scheme分别是alipayqr://platformapi/startapp?saId=10000007和
        //alipayqr://platformapi/startapp?saId=20000056
        val uri = Uri.parse("alipayqr://platformapi/startapp?saId=20000056")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        true
    } catch (e: Exception) {
        showToast(R.string.goto_alipay_failed)
        false
    }
}

fun Context.gotoAccessibilitySettings() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    try {
        startActivity(intent)
    } catch (ignored: Exception) {
        intent.action = Settings.ACTION_SETTINGS
        startActivity(intent)
    }
}

fun Context.gotoOverlaySettings() {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
        data = Uri.parse("package:${packageName}")
    }
    try {
        startActivity(intent)
    } catch (ignored: Exception) {
        intent.action = Settings.ACTION_SETTINGS
        startActivity(intent)
    }
}

fun Context.gotoAppDetailSettings() {
    AppUtils.launchAppDetailsSettings(packageName)
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