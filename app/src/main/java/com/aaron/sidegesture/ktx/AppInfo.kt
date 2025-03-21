package com.aaron.sidegesture.ktx

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aaron.sidegesture.entity.AppInfo

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/3
 */

val AppInfo.componentName: ComponentName get() = ComponentName.createRelative(packageName, className)

val AppInfo.qualifiedName: String get() = "$packageName/$className"

val AppInfo.icon: Drawable? @Composable get() {
    val pkgManager = LocalContext.current.packageManager
    return remember(this) {
        try {
            if (className.isNotEmpty()) {
                pkgManager.getActivityIcon(ComponentName.createRelative(packageName, className))
            } else {
                pkgManager.getApplicationIcon(packageName)
            }
        } catch (ignored: Exception) {
            null
        }
    }
}

fun AppInfo.getIcon(context: Context): Drawable? {
    return try {
        val pkgManager = context.packageManager
        if (className.isNotEmpty()) {
            pkgManager.getActivityIcon(ComponentName.createRelative(packageName, className))
        } else {
            pkgManager.getApplicationIcon(packageName)
        }
    } catch (ignored: Exception) {
        null
    }
}