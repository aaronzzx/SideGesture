@file:OptIn(ExperimentalPermissionsApi::class)

package com.aaron.sidegesture.ktx

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.blankj.utilcode.util.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/1
 */

val PermissionStatus.deniedForever: Boolean
    get() = this is PermissionStatus.Denied && !shouldShowRationale

@Composable
fun rememberGetInstalledAppsPermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): PermissionState {
    PermissionUtils.isGranted()
    val context = LocalContext.current
    val delegate = rememberPermissionState(PERMISSION_GET_INSTALLED_APPS, onPermissionResult)
    return remember(context, delegate) {
        GetInstalledAppsPermissionState(context, delegate)
    }
}

private data class GetInstalledAppsPermissionState(
    val context: Context,
    val delegate: PermissionState
) : PermissionState by delegate {

    override val status: PermissionStatus get() {
        if (!context.supportGetInstalledAppsPermission()) {
            return PermissionStatus.Granted
        }
        return delegate.status
    }

    /**
     * 是否支持com.android.permission.GET_INSTALLED_APPS权限
     */
    private fun Context.supportGetInstalledAppsPermission(): Boolean {
        val permissionInfo = try {
            packageManager.getPermissionInfo(PERMISSION_GET_INSTALLED_APPS, 0)
        } catch (e: Exception) {
            null
        }
        return permissionInfo != null
    }
}

const val PERMISSION_GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS"