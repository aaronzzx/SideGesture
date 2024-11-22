package com.aaron.sidegesture

import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Actions
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.GestureButton.Companion.RIGHT
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ktx.updateGestureButton
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.ui.widget.SideGestureContainer
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
            pressActions = GestureActions(
                up = Actions.single(GlobalActions.LOCK_SCREEN),
                center = Actions.single(GlobalActions.BACK)
            ),
            longPressActions = GestureActions(
                up = Actions.multiple(
                    GlobalActions.WECHAT_SCAN,
                    GlobalActions.WECHAT_PAY,
                    GlobalActions.HOME,
                    GlobalActions.ALIPAY_SCAN,
                    GlobalActions.ALIPAY_PAY
                ),
                center = Actions.single(GlobalActions.PREVIOUS_APP)
            )
        ),
        GestureButton(
            position = RIGHT,
            start = 0.3f,
            end = 1.0f,
            pressActions = GestureActions(
                up = Actions.single(GlobalActions.LOCK_SCREEN),
                center = Actions.single(GlobalActions.BACK)
            ),
            longPressActions = GestureActions(
                up = Actions.multiple(
                    GlobalActions.WECHAT_SCAN,
                    GlobalActions.WECHAT_PAY,
                    GlobalActions.HOME,
                    GlobalActions.ALIPAY_SCAN,
                    GlobalActions.ALIPAY_PAY
                ),
                center = Actions.single(GlobalActions.PREVIOUS_APP)
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
                Box(modifier = Modifier.fillMaxSize()) {
                    SideGestureContainer(
                        modifier = Modifier.matchParentSize(),
                        buttons = buttons,
                        onAction = { action ->
                            accessibilityProxy.onAction(action)
                        }
                    )
                }
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