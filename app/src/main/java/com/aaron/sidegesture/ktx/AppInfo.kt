package com.aaron.sidegesture.ktx

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aaron.sidegesture.entity.AppInfo

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/3
 */

val AppInfo.icon: Drawable? @Composable get() {
    val pkgManager = LocalContext.current.packageManager
    return remember(packageName) {
        try {
            val pkgInfo = pkgManager.getPackageInfo(packageName, PackageManager.MATCH_ALL)
            pkgInfo.applicationInfo?.loadIcon(pkgManager)
        } catch (ignored: Exception) {
            null
        }
    }
}