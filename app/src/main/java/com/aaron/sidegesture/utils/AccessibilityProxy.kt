package com.aaron.sidegesture.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.aaron.sidegesture.config.Actions
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

    fun onAction(action: Int) {
        host.onAction(action)
    }

    private fun AccessibilityService.onAction(action: Int) {
        when (action) {
            Actions.BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            Actions.HOME -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            Actions.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            Actions.PREVIOUS_APP -> {
                previousApp()
            }
            Actions.WECHAT_SCAN -> {
                gotoWechatScan()
            }
            Actions.WECHAT_PAY -> {
                gotoWechatPayCode()
            }
            Actions.ALIPAY_SCAN -> {
                gotoAlipayScan()
            }
            Actions.ALIPAY_PAY -> {
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