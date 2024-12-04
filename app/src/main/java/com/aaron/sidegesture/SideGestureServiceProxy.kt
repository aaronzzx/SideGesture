package com.aaron.sidegesture

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.dispatchMediaKeyEvent
import com.aaron.sidegesture.ktx.gotoAlipayPayCode
import com.aaron.sidegesture.ktx.gotoAlipayScan
import com.aaron.sidegesture.ktx.gotoWechat
import com.aaron.sidegesture.ktx.gotoWechatScan
import com.aaron.sidegesture.ktx.toggleMute
import com.aaron.sidegesture.ktx.volumeDown
import com.aaron.sidegesture.ktx.volumeUp
import com.aaron.sidegesture.utils.AccessibilityUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FlashlightUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/21
 */
class SideGestureServiceProxy(private val host: SideGestureService) {

    private var prevPackageName: String? = null
    private var currPackageName: String? = null

    fun onDestroy() {
        FlashlightUtils.destroy()
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        host.apply {
            when(event?.eventType){
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = event.packageName?.toString()
                    if (hasLaunchIntent(packageName) && currPackageName != packageName) {
                        prevPackageName = currPackageName
                        currPackageName = packageName
                        if (prevPackageName == null) {
                            prevPackageName = currPackageName
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    fun onAction(action: Action) {
        host.onAction(action)
    }

    private fun SideGestureService.onAction(action: Action) {
        when (action.value) {
            GlobalActions.BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            GlobalActions.HOME -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            GlobalActions.RECENT -> {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
            GlobalActions.VOLUME_UP -> {
                volumeUp()
            }
            GlobalActions.VOLUME_DOWN -> {
                volumeDown()
            }
            GlobalActions.MUTE -> {
                toggleMute()
            }
            GlobalActions.PLAY_PAUSE_SONG -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            GlobalActions.LAST_SONG -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
            GlobalActions.NEXT_SONG -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
            }
            GlobalActions.PREVIOUS_APP -> {
                previousApp()
            }
            GlobalActions.OPEN_NOTIFICATION_PANEL -> {
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
            GlobalActions.OPEN_QUICK_PANEL -> {
                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            }
            GlobalActions.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            GlobalActions.FLASHLIGHT -> {
                if (FlashlightUtils.isFlashlightEnable()) {
                    if (PermissionUtils.isGranted(Manifest.permission.CAMERA)) {
                        FlashlightUtils.setFlashlightStatus(!FlashlightUtils.isFlashlightOn())
                    } else {
                        PermissionUtils
                            .permission(Manifest.permission.CAMERA)
                            .callback(object : PermissionUtils.SimpleCallback {
                                override fun onGranted() {
                                    FlashlightUtils.setFlashlightStatus(!FlashlightUtils.isFlashlightOn())
                                }

                                override fun onDenied() {
                                }
                            })
                            .request()
                    }
                }
            }
            GlobalActions.SPLIT_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                }
            }
            GlobalActions.ASSIST_APP -> {
                try {
                    val intent = Intent().apply {
                        Intent.ACTION_ASSIST
                        setAction(Intent.ACTION_ASSIST)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (ignored: Exception) {
                }
            }
            GlobalActions.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            GlobalActions.POWER_BUTTON -> {
                performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            }
            GlobalActions.AUTO_ROTATE -> {
            }
            GlobalActions.INVERSE_COLOR -> {
            }
            GlobalActions.QUICK_APP_PANEL -> {
            }
            GlobalActions.QUICK_TOOLS -> {
            }
            GlobalActions.HIDE_GESTURE_BUTTON -> {
            }
            GlobalActions.WECHAT_SCAN -> {
                gotoWechatScan()
            }
            GlobalActions.WECHAT_PAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    @OptIn(DelicateCoroutinesApi::class)
                    GlobalScope.launch {
                        gotoWechat()
                        delay(500)
                        val screenWidth = ScreenUtils.getScreenWidth()
                        val statusBarHeight = BarUtils.getStatusBarHeight()
                        val radius = ConvertUtils.dp2px(12f)
                        var x = screenWidth - ConvertUtils.dp2px(14f) - radius
                        var y = statusBarHeight + ConvertUtils.dp2px(10f) + radius
                        AccessibilityUtils.click(this@onAction, x, y)
                        delay(500)
                        x = screenWidth - ConvertUtils.dp2px(60f) - radius
                        y = statusBarHeight + ConvertUtils.dp2px(220f) + radius
                        AccessibilityUtils.click(this@onAction, x, y)
                    }
                }
            }
            GlobalActions.ALIPAY_SCAN -> {
                gotoAlipayScan()
            }
            GlobalActions.ALIPAY_PAY -> {
                gotoAlipayPayCode()
            }
            GlobalActions.EXTRA_LAUNCH_APP -> {
                val appInfo = action.appInfo
                if (appInfo != null) {
                    val intent = Intent().apply {
                        setClassName(appInfo.packageName, appInfo.className)
                        setAction(Intent.ACTION_MAIN)
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun AccessibilityService.previousApp() {
        val prevPkgName = prevPackageName
        val curPkgName = currPackageName
        if (prevPkgName.isNullOrEmpty() || curPkgName.isNullOrEmpty()) {
            return
        }
        if (currPackageNameError()) {
            queryLaunchIntentAndStart(curPkgName)
            return
        }
        if (prevPkgName == curPkgName) return
        if (queryLaunchIntentAndStart(prevPkgName)) {
            prevPackageName = curPkgName
            currPackageName = prevPkgName
        }
    }

    private fun AccessibilityService.queryLaunchIntentAndStart(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) {
            return false
        }
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    private fun AccessibilityService.currPackageNameError(): Boolean {
        val pkgName = rootInActiveWindow?.packageName?.toString()
        return pkgName != currPackageName
    }

    private fun AccessibilityService.hasLaunchIntent(packageName: String?): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName ?: "") != null
    }
}