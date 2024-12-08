package com.aaron.sidegesture.ktx

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/1
 */

@OptIn(ExperimentalPermissionsApi::class)
val PermissionStatus.deniedForever: Boolean
    get() = this is PermissionStatus.Denied && !shouldShowRationale

fun Context.isGetInstalledAppsPermissionGranted(): Boolean {
    if (!supportGetInstalledAppsPermission(this)) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        this,
        PERMISSION_GET_INSTALLED_APPS
    ) == PackageManager.PERMISSION_GRANTED
}

fun Activity.shouldShowRationale(permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
}

const val PERMISSION_GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS"

/**
 * 是否支持com.android.permission.GET_INSTALLED_APPS权限
 */
private fun supportGetInstalledAppsPermission(context: Context): Boolean {
    val permissionInfo = try {
        context.packageManager.getPermissionInfo("com.android.permission.GET_INSTALLED_APPS", 0)
    } catch (e: Exception) {
        null
    }
    return permissionInfo != null
}