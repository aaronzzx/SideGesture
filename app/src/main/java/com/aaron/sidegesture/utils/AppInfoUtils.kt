package com.aaron.sidegesture.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.aaron.sidegesture.entity.AppInfo

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */
object AppInfoUtils {

    fun getInstalledPackages(context: Context): List<AppInfo> {
        val list = mutableListOf<AppInfo>()
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
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.MATCH_ALL)
            val item = AppInfo(
                packageName = packageName,
                label = activityInfo.loadLabel(packageManager).toString(),
                icon = activityInfo.loadIcon(packageManager),
                isUserApp = packageInfo.applicationInfo?.flags?.let {
                    (it and ApplicationInfo.FLAG_SYSTEM) == 0
                } ?: true
            )
            list.add(item)
        }
        return list
    }
}