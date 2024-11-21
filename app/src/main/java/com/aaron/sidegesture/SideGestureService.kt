package com.aaron.sidegesture

import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.constant.Actions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.entity.LongPressActions
import com.aaron.sidegesture.entity.PressActions
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ktx.updateGestureButton
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import com.aaron.sidegesture.ui.SideGesturePad
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.utils.AccessibilityProxy
import com.blankj.utilcode.util.ScreenUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private val accessibilityProxy = AccessibilityProxy(this)

    private val buttons = listOf(
        GestureButton(
            position = LEFT,
            start = 0.3f,
            end = 1.0f,
            pressActions = PressActions(
                up = Actions.LOCK_SCREEN,
                center = Actions.BACK
            ),
            longPressActions = LongPressActions(
                up = listOf(
                    Actions.WECHAT_SCAN,
                    Actions.WECHAT_PAY,
                    Actions.HOME,
                    Actions.ALIPAY_SCAN,
                    Actions.ALIPAY_PAY
                ),
                center = listOf(Actions.PREVIOUS_APP)
            )
        ),
        GestureButton(
            position = RIGHT,
            start = 0.3f,
            end = 1.0f,
            pressActions = PressActions(
                up = Actions.LOCK_SCREEN,
                center = Actions.BACK
            ),
            longPressActions = LongPressActions(
                up = listOf(
                    Actions.WECHAT_SCAN,
                    Actions.WECHAT_PAY,
                    Actions.HOME,
                    Actions.ALIPAY_SCAN,
                    Actions.ALIPAY_PAY
                ),
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
        accessibilityProxy.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
    }

    override fun onSetOverlay() {
        val mainView = mainView
        if (mainView != null) {
            removeWindow(mainView)
        }
        this.mainView = attachComposeOverlay {
            SideGestureTheme {
                SideGesturePad(
                    modifier = Modifier.fillMaxSize(),
                    buttons = buttons,
                    onAction = { action ->
                        accessibilityProxy.onAction(action)
                    }
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
}