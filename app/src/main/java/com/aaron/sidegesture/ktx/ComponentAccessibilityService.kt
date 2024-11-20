package com.aaron.sidegesture.ktx

import android.annotation.SuppressLint
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.utils.MotionEventDispatcher

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

fun ComponentAccessibilityService.updateLayout(view: View, lp: WindowManager.LayoutParams) {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    wm.updateViewLayout(view, lp)
}

fun ComponentAccessibilityService.setComposeOverlay(content: @Composable () -> Unit): ComposeView {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        setBasic(false)
        updateMainView()
    }
    val composeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@setComposeOverlay)
        setViewTreeViewModelStoreOwner(this@setComposeOverlay)
        setViewTreeSavedStateRegistryOwner(this@setComposeOverlay)
        setContent {
            content()
        }
    }
    wm.addView(composeView, lp)
    return composeView
}

fun ComponentAccessibilityService.attachGestureButtons(buttons: Collection<GestureButton>): List<View> {
    return buttons.map { button ->
        attachGestureButton(button)
    }
}

fun ComponentAccessibilityService.attachGestureButton(button: GestureButton): View {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    val lp = WindowManager.LayoutParams().apply {
        setBasic(true)
        updateGestureButton(button)
    }
    @SuppressLint("ClickableViewAccessibility")
    val view = View(this).apply {
        tag = button
        setOnTouchListener { _, event ->
            MotionEventDispatcher.dispatch(event)
            false
        }
    }
    wm.addView(view, lp)
    return view
}

fun ComponentAccessibilityService.removeWindows(views: Collection<View>) {
    views.forEach { view ->
        removeWindow(view)
    }
}

fun ComponentAccessibilityService.removeWindow(view: View) {
    val wm = ContextCompat.getSystemService(this, WindowManager::class.java)!!
    try {
        wm.removeView(view)
    } catch (ignored: Exception) {
    }
}