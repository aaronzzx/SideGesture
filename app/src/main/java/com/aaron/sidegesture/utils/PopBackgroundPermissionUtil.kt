package com.aaron.sidegesture.utils

import android.app.AppOpsManager
import android.content.Context
import android.net.Uri
import android.os.Binder
import android.provider.Settings
import com.blankj.utilcode.util.RomUtils

object PopBackgroundPermissionUtil {

    private const val HW_OP_CODE_POPUP_BACKGROUND_WINDOW = 100000
    private const val XM_OP_CODE_POPUP_BACKGROUND_WINDOW = 10021

    /**
     * 是否有后台弹出页面权限
     */
    fun hasPopupBackgroundPermission(context: Context): Boolean {
        if (RomUtils.isHuawei()) {
            return checkHwPermission(context)
        }
        if (RomUtils.isXiaomi()) {
            return checkXmPermission(context)
        }
        if (RomUtils.isVivo()) {
            checkVivoPermission(context)
        }
        return Settings.canDrawOverlays(context)
    }

    private fun checkHwPermission(context: Context): Boolean {
        try {
            val c = Class.forName("com.huawei.android.app.AppOpsManagerEx")
            val m = c.getDeclaredMethod(
                "checkHwOpNoThrow",
                AppOpsManager::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = m.invoke(
                c.newInstance(),
                *arrayOf(
                    context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager,
                    HW_OP_CODE_POPUP_BACKGROUND_WINDOW,
                    Binder.getCallingUid(),
                    context.packageName
                )
            ) as Int
            return AppOpsManager.MODE_ALLOWED == result
        } catch (e: Exception) {
            //ignore
        }
        return false
    }

    private fun checkXmPermission(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        try {
            val method = ops.javaClass.getMethod(
                "checkOpNoThrow", *arrayOf<Class<*>?>(
                    Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java
                )
            )
            val result = method.invoke(
                ops,
                XM_OP_CODE_POPUP_BACKGROUND_WINDOW,
                android.os.Process.myUid(),
                context.packageName
            ) as Int
            return result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            //ignore
        }
        return false
    }

    private fun checkVivoPermission(context: Context): Boolean {
        val uri =
            Uri.parse("content://com.vivo.permissionmanager.provider.permission/start_bg_activity")
        val selection = "pkgname = ?"
        val selectionArgs = arrayOf(context.packageName)
        var result = 1
        val contentResolver = context.contentResolver
        try {
            contentResolver.query(uri, null, selection, selectionArgs, null).use { cursor ->
                if (cursor!!.moveToFirst()) {
                    result = cursor.getInt(cursor.getColumnIndex("currentstate"))
                }
            }
        } catch (exception: Exception) {
            //ignore
        }
        return result == AppOpsManager.MODE_ALLOWED
    }

}
