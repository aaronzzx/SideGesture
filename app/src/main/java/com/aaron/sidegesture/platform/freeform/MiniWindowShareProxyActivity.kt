package com.aaron.sidegesture.platform.freeform

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.aaron.sidegesture.R
import com.aaron.sidegesture.utils.showToast

/**
 * vivo 小窗分享代理。
 *
 * [com.aaron.sidegesture.platform.freeform.MiniWindowUtils] 用显式 component 的 ACTION_SEND 直投到这里，
 * 由本代理(主进程、前台 Activity)在自身上下文里拉起真正的目标，借 vivo 系统的分享小窗化把目标显示为小窗。
 *
 * 必须在 [onCreate] 里同步拉起目标并立即 [finish]：窗口在首帧绘制前就结束，vivo 不会把代理窗口
 * 画成 freeform 小窗(否则会有小窗框+阴影一闪而过)。发起方在 :service 进程、本代理在主进程，
 * 无法走进程内回传，故成功/失败兜底就地在本代理内完成。
 */
class MiniWindowShareProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openTarget()
        finish()
    }

    private fun openTarget() {
        val targetIntent = getTargetIntent()
        if (targetIntent != null) {
            val started = runCatching {
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(targetIntent)
            }.isSuccess
            if (started) return
        }
        // 兜底：目标 Intent 缺失或拉起失败时，按包名取默认启动 Intent 普通拉起
        val packageName = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!packageName.isNullOrEmpty()) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                val started = runCatching {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }.isSuccess
                if (started) return
            }
        }
        showToast(R.string.launch_mini_window_failed)
    }

    private fun getTargetIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
        }
    }
}
