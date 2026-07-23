package com.aaron.sidegesture.platform.userprofile

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.componentName

/**
 * 负责关联用户资料中的桌面应用枚举、图标读取和普通启动。
 *
 * 当前用户仍由 PackageManager 处理；这里只返回关联的其它可访问资料，
 * 避免改变现有主资料应用列表行为。
 */
object ProfileAppManager {

    fun queryAssociatedProfileApps(context: Context): List<AppInfo> {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val userManager = context.getSystemService(UserManager::class.java)
        val currentUser = Process.myUserHandle()
        val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            launcherApps.profiles
        } else {
            userManager.userProfiles
        }
        return profiles
            .asSequence()
            .distinct()
            .filter { it != currentUser }
            .flatMap { profile ->
                val serialNumber = userManager.getSerialNumberForUser(profile)
                if (serialNumber < 0) {
                    emptySequence()
                } else {
                    queryProfileApps(launcherApps, profile, serialNumber).asSequence()
                }
            }
            .toList()
    }

    fun loadBadgedIcon(context: Context, appInfo: AppInfo): Drawable? {
        val profile = resolveProfile(context, appInfo.profileSerialNumber) ?: return null
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        return runCatching {
            launcherApps
                .getActivityList(appInfo.packageName, profile)
                .firstOrNull { it.componentName == appInfo.componentName }
                ?.getBadgedIcon(0)
        }.getOrNull()
    }

    fun launch(context: Context, appInfo: AppInfo): Boolean {
        val profile = resolveProfile(context, appInfo.profileSerialNumber) ?: return false
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        return runCatching {
            launcherApps.startMainActivity(appInfo.componentName, profile, null, null)
        }.isSuccess
    }

    fun profileExists(context: Context, serialNumber: Long): Boolean {
        return resolveProfile(context, serialNumber) != null
    }

    private fun queryProfileApps(
        launcherApps: LauncherApps,
        profile: UserHandle,
        serialNumber: Long
    ): List<AppInfo> {
        return runCatching {
            launcherApps.getActivityList(null, profile).map { activity ->
                AppInfo(
                    packageName = activity.componentName.packageName,
                    className = activity.componentName.className,
                    label = activity.label.toString(),
                    profileSerialNumber = serialNumber
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun resolveProfile(context: Context, serialNumber: Long?): UserHandle? {
        if (serialNumber == null) return null
        val userManager = context.getSystemService(UserManager::class.java)
        return userManager.getUserForSerialNumber(serialNumber)
    }
}
