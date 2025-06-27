package com.aaron.sidegesture.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.aaron.sidegesture.ktx.queryIntentActivitiesCompat
import java.util.Locale

object SystemAlertWindow {

    private val MARK = Build.MANUFACTURER.lowercase(Locale.getDefault())
    private const val REQUEST_OVERLY = 7562
    
    fun start(context: Context) {
        var intent: Intent?
        intent = if (MARK.contains("huawei")) {
            huaweiApi(context)
        } else if (MARK.contains("xiaomi")) {
            xiaomiApi(context)
        } else if (MARK.contains("oppo")) {
            oppoApi(context)
        } else if (MARK.contains("vivo")) {
            vivoApi(context)
        } else if (MARK.contains("meizu")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                meiZuApi(context)
            } else {
                meizuApi(context)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MdefaultApi(context)
        } else {
            LdefaultApi(context)
        }
        try {
            intent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            intent = appDetailsApi(context)
            context.startActivity(intent)
        }
    }

    private fun huaweiApi(context: Context): Intent? {
        val intent = Intent()
        intent.setClassName(
            "com.huawei.systemmanager",
            "com.huawei.permissionmanager.ui.MainActivity"
        )
        if (hasActivity(context, intent)) {
            return intent
        }
        intent.setClassName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity"
        )
        if (hasActivity(context, intent)) {
            return intent
        }
        intent.setClassName(
            "com.huawei.systemmanager",
            "com.huawei.notificationmanager.ui.NotificationManagmentActivity"
        )
        return if (hasActivity(context, intent)) {
            intent
        } else MdefaultApi(context)
    }

    private fun xiaomiApi(context: Context): Intent? {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.putExtra("extra_pkgname", context.packageName)
        if (hasActivity(context, intent)) {
            return intent
        }
        intent.setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.permissions.AppPermissionsEditorActivity"
        )
        return if (hasActivity(context, intent)) {
            intent
        } else MdefaultApi(context)
    }

    private fun oppoApi(context: Context): Intent? {
        val intent = Intent()
        intent.putExtra("packageName", context.packageName)
        intent.setClassName(
            "com.color.safecenter",
            "com.color.safecenter.permission.floatwindow.FloatWindowListActivity"
        )
        if (hasActivity(context, intent)) {
            return intent
        }
        intent.setClassName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.sysfloatwindow.FloatWindowListActivity"
        )
        if (hasActivity(context, intent)) {
            return intent
        }
        intent.setClassName("com.oppo.safe", "com.oppo.safe.permission.PermissionAppListActivity")
        return if (hasActivity(context, intent)) {
            intent
        } else MdefaultApi(context)
    }

    private fun vivoApi(context: Context): Intent? {
        val intent = Intent()
        intent.setClassName(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.FloatWindowManager"
        )
        intent.putExtra("packagename", context.packageName)
        if (hasActivity(context, intent)) {
            return intent
        }
        intent.setClassName(
            "com.iqoo.secure",
            "com.iqoo.secure.safeguard.SoftPermissionDetailActivity"
        )
        return if (hasActivity(context, intent)) {
            intent
        } else MdefaultApi(context)
    }

    private fun meizuApi(context: Context): Intent? {
        val intent = Intent("com.meizu.safe.security.SHOW_APPSEC")
        intent.putExtra("packageName", context.packageName)
        intent.component = ComponentName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity")
        return if (hasActivity(context, intent)) {
            intent
        } else MdefaultApi(context)
    }

    private fun LdefaultApi(context: Context): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        return intent
    }

    private fun appDetailsApi(context: Context): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        return intent
    }

    private fun MdefaultApi(context: Context): Intent? {
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        }
        intent!!.data = Uri.fromParts("package", context.packageName, null)
        return if (hasActivity(context, intent)) {
            intent
        } else appDetailsApi(context)
    }

    private fun meiZuApi(context: Context): Intent? {
        val intent = Intent("com.meizu.safe.security.SHOW_APPSEC")
        intent.putExtra("packageName", context.packageName)
        intent.setClassName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity")
        return if (hasActivity(context, intent)) {
            intent
        } else MdefaultApi(context)
    }

    private fun hasActivity(context: Context, intent: Intent?): Boolean {
        val packageManager = context.packageManager
        return packageManager.queryIntentActivitiesCompat(
            intent!!,
            PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()
    }
}
