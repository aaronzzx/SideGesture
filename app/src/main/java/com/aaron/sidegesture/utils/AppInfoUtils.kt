package com.aaron.sidegesture.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.LauncherInfo
import com.aaron.sidegesture.ktx.queryIntentActivitiesCompat

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */
object AppInfoUtils {

    fun queryCreateShortcutActivities(context: Context, allowRepeatPackage: Boolean = true): List<LauncherInfo> {
        val list = mutableListOf<LauncherInfo>()
        val pkgList = mutableListOf<String>()
        val packageManager = context.packageManager
        val intent = Intent().apply {
            setAction(Intent.ACTION_CREATE_SHORTCUT)
        }
        val activities = packageManager.queryIntentActivitiesCompat(intent, PackageManager.MATCH_ALL)
        for (resolveInfo in activities) {
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo?.packageName
            if (!activityInfo.exported) continue
            if (packageName.isNullOrEmpty()) continue
            if (!allowRepeatPackage && packageName in pkgList) continue
            val item = LauncherInfo(
                packageName = packageName,
                className = activityInfo.name,
                label = activityInfo.loadLabel(packageManager).toString()
            )
            list.add(item)
            pkgList.add(packageName)
        }
        return list
    }

    fun queryLauncherActivities(context: Context, allowRepeatPackage: Boolean = true): List<AppInfo> {
        val list = mutableListOf<AppInfo>()
        val pkgList = mutableListOf<String>()
        val packageManager = context.packageManager
        val intent = Intent().apply {
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = packageManager.queryIntentActivitiesCompat(intent, PackageManager.MATCH_ALL)
        for (resolveInfo in activities) {
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo?.packageName
            if (packageName.isNullOrEmpty()) continue
            if (!allowRepeatPackage && packageName in pkgList) continue
            val item = AppInfo(
                packageName = packageName,
                className = activityInfo.name,
                label = activityInfo.loadLabel(packageManager).toString()
            )
            list.add(item)
            pkgList.add(packageName)
        }
        return list
    }
}