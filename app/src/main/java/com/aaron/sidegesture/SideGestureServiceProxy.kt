package com.aaron.sidegesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.ktx.gotoAlipayPayCode
import com.aaron.sidegesture.ktx.gotoAlipayScan
import com.aaron.sidegesture.ktx.gotoWechat
import com.aaron.sidegesture.ktx.gotoWechatScan
import com.aaron.sidegesture.utils.AccessibilityUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/21
 */
class SideGestureServiceProxy(private val host: AccessibilityService) {

    private var prevPackageName: String? = null
    private var currPackageName: String? = null

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

    fun onAction(action: String) {
        host.onAction(action)
    }

    private fun AccessibilityService.onAction(action: String) {
        when (action) {
            GlobalActions.BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            GlobalActions.HOME -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            GlobalActions.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            GlobalActions.PREVIOUS_APP -> {
                previousApp()
            }
            GlobalActions.WECHAT_SCAN -> {
                gotoWechatScan()
            }
            GlobalActions.WECHAT_PAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    @OptIn(DelicateCoroutinesApi::class)
                    GlobalScope.launch {
                        gotoWechat()
                        delay(300)
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