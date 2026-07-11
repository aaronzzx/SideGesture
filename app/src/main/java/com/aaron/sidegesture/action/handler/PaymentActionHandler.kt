package com.aaron.sidegesture.action.handler

import android.content.ComponentName
import android.os.Build
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ForegroundAppAware
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.ktx.gotoAlipayPayCode
import com.aaron.sidegesture.ktx.gotoAlipayScan
import com.aaron.sidegesture.ktx.gotoWechat
import com.aaron.sidegesture.ktx.gotoWechatScan
import com.aaron.sidegesture.utils.AccessibilityUtils
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PaymentActionHandler(
    private val service: SideGestureService,
    private val scope: CoroutineScope
) : ActionHandler, ForegroundAppAware {

    override val supportedActions = setOf(
        GlobalActions.WECHAT_SCAN,
        GlobalActions.WECHAT_PAY,
        GlobalActions.ALIPAY_SCAN,
        GlobalActions.ALIPAY_PAY
    )

    private var currentActivityName: String? = null
    private var pendingWechatPay = false
    private var pendingWechatPayAutoCancelJob: Job? = null

    override fun onChange(snapshot: ForegroundAppAware.Snapshot) {
        if (isActivity(snapshot.packageName, snapshot.className)) {
            currentActivityName = snapshot.className
        }
        if (pendingWechatPay &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            snapshot.packageName == WECHAT_PACKAGE
        ) {
            pendingWechatPayAutoCancelJob?.cancel()
            pendingWechatPay = false
            mockClickWechatPay()
        }
    }

    override suspend fun handle(request: ActionRequest) {
        when (request.action.value) {
            GlobalActions.WECHAT_SCAN -> service.gotoWechatScan()
            GlobalActions.WECHAT_PAY -> openWechatPay()
            GlobalActions.ALIPAY_SCAN -> service.gotoAlipayScan()
            GlobalActions.ALIPAY_PAY -> service.gotoAlipayPayCode()
        }
    }

    private fun openWechatPay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            showVersionTooLowToast(service, R.string.action_wechat_pay_simulate_click)
            return
        }
        val isWechatHome = currentActivityName == WECHAT_HOME_ACTIVITY
        service.gotoWechat()
        if (!isWechatHome) {
            pendingWechatPayAutoCancelJob?.cancel()
            pendingWechatPayAutoCancelJob = scope.launch {
                delay(3000)
                pendingWechatPay = false
            }
            pendingWechatPay = true
        }
    }

    private fun mockClickWechatPay() {
        scope.launch {
            delay(500)
            val radius = ConvertUtils.dp2px(12f)
            AccessibilityUtils.click(
                service,
                ScreenUtils.getScreenWidth() - ConvertUtils.dp2px(14f) - radius,
                BarUtils.getStatusBarHeight() + ConvertUtils.dp2px(10f) + radius
            )
            delay(500)
            AccessibilityUtils.click(
                service,
                ScreenUtils.getScreenWidth() - ConvertUtils.dp2px(60f) - radius,
                BarUtils.getStatusBarHeight() + ConvertUtils.dp2px(220f) + radius
            )
        }
    }

    private fun isActivity(packageName: String?, className: String?): Boolean {
        packageName ?: return false
        className ?: return false
        return runCatching {
            service.packageManager.getActivityInfo(ComponentName(packageName, className), 0)
        }.isSuccess
    }

    private companion object {
        const val WECHAT_PACKAGE = "com.tencent.mm"
        const val WECHAT_HOME_ACTIVITY = "com.tencent.mm.ui.LauncherUI"
    }
}
