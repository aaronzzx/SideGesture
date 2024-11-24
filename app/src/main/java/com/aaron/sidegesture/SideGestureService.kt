package com.aaron.sidegesture

import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.attachComposeOverlay
import com.aaron.sidegesture.ktx.attachGestureButtons
import com.aaron.sidegesture.ktx.removeWindow
import com.aaron.sidegesture.ktx.removeWindows
import com.aaron.sidegesture.ktx.updateGestureButton
import com.aaron.sidegesture.ktx.updateLayout
import com.aaron.sidegesture.ktx.updateMainView
import com.aaron.sidegesture.ui.theme.SideGestureTheme
import com.aaron.sidegesture.ui.widget.SideGestureContainer
import com.aaron.sidegesture.utils.DataStoreHolder
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private val proxy = SideGestureServiceProxy(this)

    private var mainView: View? = null
    private var buttonViews: List<View>? = null
    private var orientation = if (ScreenUtils.isLandscape()) 2 else 1

    private val coroutineScope = MainScope()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation
            updateLayout()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        proxy.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        coroutineScope.cancel()
    }

    override fun onSetOverlay() {
        val mainView = mainView
        if (mainView != null) {
            removeWindow(mainView)
        }
        this.mainView = attachComposeOverlay {
            SideGestureTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val buttons by DataStoreHolder
                        .gestureButtons
                        .data
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    SideGestureContainer(
                        modifier = Modifier.matchParentSize(),
                        buttons = buttons,
                        onAction = { action ->
                            proxy.onAction(action)
                        }
                    )
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main.immediate) {
            DataStoreHolder.gestureButtons.data.collectLatest { buttons ->
                val buttonViews = buttonViews
                if (buttonViews != null) {
                    removeWindows(buttonViews)
                }
                this@SideGestureService.buttonViews = attachGestureButtons(buttons)
            }
        }
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