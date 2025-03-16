package com.aaron.sidegesture.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.aaron.sidegesture.entity.AppInfo

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */
object AppInfoUtils {

    fun queryLauncherActivities(context: Context, allowRepeatPackage: Boolean = true): List<AppInfo> {
        val list = mutableListOf<AppInfo>()
        val pkgList = mutableListOf<String>()
        val packageManager = context.packageManager
        val intent = Intent().apply {
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
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