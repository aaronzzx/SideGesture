package com.aaron.sidegesture.ktx

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils


/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun Context.gotoWechat() {
    val intent = packageManager.getLaunchIntentForPackage("com.tencent.mm")
    if (intent != null &&
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    ) {
        try {
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            // TODO: hardcode
            ToastUtils.showShort("无法跳转到微信，请检查您是否安装了微信！")
        }
    } else {
        // TODO: hardcode
        ToastUtils.showShort("无法跳转到微信，请检查您是否安装了微信！")
    }
}

fun Context.gotoWechatScan() {
    val intent = packageManager.getLaunchIntentForPackage("com.tencent.mm")
    if (intent != null &&
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    ) {
        intent.putExtra("LauncherUI.From.Scaner.Shortcut", true)
        intent.setAction("android.intent.action.VIEW")
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            // TODO: hardcode
            ToastUtils.showShort("无法跳转到微信，请检查您是否安装了微信！")
        }
    } else {
        // TODO: hardcode
        ToastUtils.showShort("无法跳转到微信，请检查您是否安装了微信！")
    }
}

fun Context.gotoWechatPayCode() {
    // 可能需要无障碍识别界面元素来跳转
//    val cmd = "am start -n com.tencent.mm/com.tencent.mm.plugin.offline.ui.WalletOfflineCoinPurseUI"
//    ShellUtils.execCmd(cmd, false)
//    try {
//        //利用Intent打开微信
//        val uri = Uri.parse("weixin://wxapppay/?action=scan")
//        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        }
//        startActivity(intent)
//    } catch (e: Exception) {
//        e.printStackTrace()
//        //若无法正常跳转，在此进行错误处理
//        // TODO: hardcode
//        ToastUtils.showShort("无法跳转到微信，请检查您是否安装了微信！")
//    }
}

fun Context.gotoAlipayScan() {
    try {
        //利用Intent打开支付宝
        //支付宝跳过开启动画打开扫码和付款码的url scheme分别是alipayqr://platformapi/startapp?saId=10000007和
        //alipayqr://platformapi/startapp?saId=20000056
        val uri = Uri.parse("alipayqr://platformapi/startapp?saId=10000007")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    } catch (e: Exception) {
        //若无法正常跳转，在此进行错误处理
        ToastUtils.showShort("无法跳转到支付宝，请检查您是否安装了支付宝！")
    }
}

fun Context.gotoAlipayPayCode() {
    try {
        //利用Intent打开支付宝
        //支付宝跳过开启动画打开扫码和付款码的url scheme分别是alipayqr://platformapi/startapp?saId=10000007和
        //alipayqr://platformapi/startapp?saId=20000056
        val uri = Uri.parse("alipayqr://platformapi/startapp?saId=20000056")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    } catch (e: Exception) {
        //若无法正常跳转，在此进行错误处理
        ToastUtils.showShort("无法跳转到支付宝，请检查您是否安装了支付宝！")
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
    startActivity(intent)
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