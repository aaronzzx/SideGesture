package com.aaron.sidegesture

import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.onSingleClick
import com.aaron.composeaccessibility.ComponentAccessibilityService
import com.aaron.composeaccessibility.setComposeOverlay

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/14
 */
class SideGestureService : ComponentAccessibilityService() {

    private var prevPkgName: String? = null
    private var curPkgName: String? = null

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        when(p0?.eventType){
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkgName = p0.packageName?.toString() ?: ""
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
        setComposeOverlay(this, this, this) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .clipToBackground(
                            color = Color.Red,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onSingleClick {
                            test()
                        }
                )
            }
        }
    }

    private fun test() {
        val prevPkgName = prevPkgName ?: return
        val curPkgName = curPkgName
        if (prevPkgName == curPkgName) return
        val intent = packageManager.getLaunchIntentForPackage(prevPkgName) ?: return
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            this.prevPkgName = curPkgName
            this.curPkgName = prevPkgName
        } catch (ex: Exception) {
            Log.d("zzx", "$ex")
        }
    }
}