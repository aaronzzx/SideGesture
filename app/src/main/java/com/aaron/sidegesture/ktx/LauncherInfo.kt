package com.aaron.sidegesture.ktx

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.aaron.sidegesture.entity.LauncherInfo
import java.nio.ByteBuffer

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/3
 */

val LauncherInfo.componentName: ComponentName get() = ComponentName.createRelative(packageName, className)

val LauncherInfo.qualifiedName: String get() = "$packageName/$className"

val LauncherInfo.icon: Drawable? @Composable get() {
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

fun LauncherInfo.getIcon(context: Context): Drawable? {
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

val LauncherInfo.ShortcutInfo.componentName: ComponentName get() = ComponentName.createRelative(packageName, className)

val LauncherInfo.ShortcutInfo.qualifiedName: String get() = "$packageName/$className"

val LauncherInfo.ShortcutInfo.icon: Drawable? @Composable get() {
    val context = LocalContext.current
    val pkgManager = context.packageManager
    return remember(this) {
        try {
            if (iconData != null) {
                val bitmap = createBitmap(iconWidth, iconHeight)
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(iconData))
                bitmap.toDrawable(context.resources)
            } else {
                val resources = pkgManager.getResourcesForApplication(packageName)
                resources.getDrawable(iconRes)
            }
        } catch (ignored: Exception) {
            null
        }
    }
}

fun LauncherInfo.ShortcutInfo.getIcon(context: Context): Drawable? {
    return try {
        if (iconData != null) {
            val bitmap = createBitmap(iconWidth, iconHeight)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(iconData))
            bitmap.toDrawable(context.resources)
        } else {
            val resources = context.packageManager.getResourcesForApplication(packageName)
            resources.getDrawable(iconRes)
        }
    } catch (ignored: Exception) {
        null
    }
}