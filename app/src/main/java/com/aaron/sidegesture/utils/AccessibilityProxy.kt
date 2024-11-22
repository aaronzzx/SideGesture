package com.aaron.sidegesture.utils

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
import com.aaron.sidegesture.ktx.gotoWechatPayCode
import com.aaron.sidegesture.ktx.gotoWechatScan

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/21
 */
class AccessibilityProxy(private val host: AccessibilityService) {

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
                gotoWechatPayCode()
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