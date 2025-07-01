package com.aaron.sidegesture.ktx

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.drawable.updateBounds
import androidx.core.graphics.scale
import com.aaron.sidegesture.entity.LauncherInfo
import com.blankj.utilcode.util.ConvertUtils
import kotlin.math.min

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/3
 */

val LauncherInfo.componentName: ComponentName get() = ComponentName.createRelative(packageName, className)

val LauncherInfo.qualifiedName: String get() = "$packageName/$className"

val LauncherInfo.icon: Drawable? @Composable get() {
    val context = LocalContext.current
    return remember(this, context) { getIcon(context) }
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

val LauncherInfo.ShortcutInfo.qualifiedNameWithIntents: String get() = "$packageName/$className(${intents.joinToString()})"

val LauncherInfo.ShortcutInfo.icon: Drawable? @Composable get() {
    val context = LocalContext.current
    return remember(this, context) { getIcon(context) }
}

fun LauncherInfo.ShortcutInfo.getIcon(context: Context): Drawable? {
    return try {
        var resources = context.resources
        val model = iconBitmap
            ?: if (iconPath != null) {
                BitmapFactory.decodeFile(iconPath)
            } else {
                resources = context.packageManager.getResourcesForApplication(packageName)
                BitmapFactory.decodeResource(resources, iconRes) ?: resources.getDrawable(iconRes, context.theme)
            }

        val stdSize = ConvertUtils.dp2px(48f)
        if (model is Bitmap) {
            val minSize = min(model.width, model.height)
            if (minSize < stdSize) {
                val ratio = model.width.toFloat() / model.height.toFloat()
                model.scale(stdSize, (stdSize / ratio).toInt()).toDrawable(resources)
            } else {
                model.toDrawable(resources)
            }
        } else {
            model as Drawable
            model.updateBounds(0, 0, stdSize, stdSize)
            val bitmap = createBitmap(stdSize, stdSize, Bitmap.Config.ARGB_8888)
            model.draw(Canvas(bitmap))
            bitmap.toDrawable(resources)
        }
    } catch (ignored: Exception) {
        null
    }
}