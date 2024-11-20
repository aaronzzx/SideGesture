package com.aaron.sidegesture

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.config.Actions
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.gotoAliPayPayCode
import com.aaron.sidegesture.ktx.gotoAliPayScan
import com.aaron.sidegesture.ktx.gotoWechatPayCode
import com.aaron.sidegesture.ktx.gotoWechatScan
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ktx.setComposeOverlay
import com.aaron.sidegesture.ktx.updateGestureButton
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import com.aaron.sidegesture.ui.SideGesturePad
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private var prevPkgName: String? = null
    private var curPkgName: String? = null

    private val buttons = listOf(
        GestureButton(
            position = LEFT,
            start = 0.3f,
            end = 1.0f,
            pressAction = GestureActions.Single(
                up = Actions.LOCK_SCREEN,
                center = Actions.BACK
            ),
            longPressAction = GestureActions.Multiple(
//                up = listOf(
//                    Actions.WECHAT_SCAN,
//                    Actions.WECHAT_PAY,
//                    Actions.SEARCH_IN_APP,
//                    Actions.ALIPAY_SCAN,
//                    Actions.ALIPAY_PAY
//                ),
                center = listOf(Actions.PREVIOUS_APP)
            )
        ),
        GestureButton(
            position = RIGHT,
            start = 0.3f,
            end = 1.0f,
            pressAction = GestureActions.Single(
                up = Actions.LOCK_SCREEN,
                center = Actions.BACK
            ),
            longPressAction = GestureActions.Multiple(
//                up = listOf(
//                    Actions.ALIPAY_SCAN,
//                    Actions.ALIPAY_PAY
//                ),
                center = listOf(Actions.PREVIOUS_APP)
            )
        )
    )

    private var mainView: View? = null
    private var buttonViews: List<View>? = null
    private var orientation = if (ScreenUtils.isLandscape()) 2 else 1

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation
            updateLayout()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when(event?.eventType){
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkgName = event.packageName?.toString() ?: ""
                val intent = packageManager.getLaunchIntentForPackage(pkgName)
                if (intent != null && curPkgName != pkgName) {
                    prevPkgName = curPkgName
                    curPkgName = pkgName
                }
            }
            else -> Unit
        }
    }

    override fun onInterrupt() {
    }

    override fun onSetOverlay() {
        val mainView = mainView
        if (mainView != null) {
            removeWindow(mainView)
        }
        this.mainView = setComposeOverlay {
            SideGestureTheme {
                val context = LocalContext.current
                SideGesturePad(
                    modifier = Modifier.fillMaxSize(),
                    onAction = { action ->
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
                                context.gotoWechatScan()
                            }
                            Actions.WECHAT_PAY -> {
                                context.gotoWechatPayCode()
                            }
                            Actions.ALIPAY_SCAN -> {
                                context.gotoAliPayScan()
                            }
                            Actions.ALIPAY_PAY -> {
                                context.gotoAliPayPayCode()
                            }
                        }
                    },
                    buttons = buttons,
                )
            }
        }
        val buttonViews = buttonViews
        if (buttonViews != null) {
            removeWindows(buttonViews)
        }
        this.buttonViews = attachGestureButtons(buttons)
    }

    private fun updateLayout() {
        val mainView = mainView
        if (mainView != null) {
            val lp = (mainView.layoutParams as WindowManager.LayoutParams).apply {
                updateMainView()
            }
            updateLayout(mainView, lp)
        }
        val buttonViews = buttonViews
        buttonViews?.forEach { view ->
            val button = view.tag as? GestureButton ?: return
            val lp = (view.layoutParams as WindowManager.LayoutParams).apply {
                updateGestureButton(button)
            }
            updateLayout(view, lp)
        }
    }

    private fun previousApp() {
        val prevPkgName = prevPkgName
        if (prevPkgName.isNullOrEmpty()) {
            // TODO: hardcode
            ToastUtils.showShort("没有上一个应用")
            return
        }
        val curPkgName = curPkgName
        if (prevPkgName == curPkgName) return
        val intent = packageManager.getLaunchIntentForPackage(prevPkgName) ?: return
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            this.prevPkgName = curPkgName
            this.curPkgName = prevPkgName
        } catch (ignored: Exception) {
        }
    }
}