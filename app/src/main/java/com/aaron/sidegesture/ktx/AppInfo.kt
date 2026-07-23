package com.aaron.sidegesture.ktx

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.platform.userprofile.ProfileAppManager

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/3
 */

val AppInfo.componentName: ComponentName get() = ComponentName.createRelative(packageName, className)

val AppInfo.qualifiedName: String get() {
    val currentQualifiedName = "$packageName/$className"
    return profileSerialNumber?.let { "$it@$currentQualifiedName" } ?: currentQualifiedName
}

val AppInfo.icon: Drawable? @Composable get() {
    val context = LocalContext.current
    return remember(this, context) { getIcon(context) }
}

fun AppInfo.getIcon(context: Context): Drawable? {
    if (profileSerialNumber != null) {
        return ProfileAppManager.loadBadgedIcon(context, this)
    }
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
