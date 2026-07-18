package com.aaron.sidegesture.action.handler

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.ForegroundAppAware
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.feature.environment.ServiceEnvironmentMonitor
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.launchAppInPopup
import com.aaron.sidegesture.ktx.launchAppInfo
import com.aaron.sidegesture.ktx.launchShortcutInfo
import com.aaron.sidegesture.ktx.queryIntentActivitiesCompat
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.platform.shell.ShellActionExecutor
import com.aaron.sidegesture.utils.showVersionTooLowToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppActionHandler(
    private val service: SideGestureService,
    private val settingsStore: ServiceSettingsStore,
    private val environmentMonitor: ServiceEnvironmentMonitor,
    scope: CoroutineScope
) : ActionHandler, ForegroundAppAware {

    private companion object {
        val PACKAGE_NAME_REGEX = Regex("^[A-Za-z0-9_.]+$")
    }

    override val supportedActions = setOf(
        GlobalActions.PREVIOUS_APP,
        GlobalActions.KILL_APP,
        GlobalActions.POPUP_SCREEN,
        GlobalActions.EXTRA_LAUNCH_APP,
        GlobalActions.EXTRA_LAUNCH_SHORTCUT
    )

    private var previousPackageName: String? = null
    private var currentPackageName: String? = null
    private var pendingForegroundSnapshot: ForegroundAppAware.Snapshot? = null

    init {
        scope.launch {
            settingsStore.awaitSnapshot()
            pendingForegroundSnapshot?.let { snapshot ->
                pendingForegroundSnapshot = null
                onChange(snapshot)
            }
        }
    }

    override fun onChange(snapshot: ForegroundAppAware.Snapshot) {
        val settings = settingsStore.currentSnapshotOrNull()
        if (settings == null) {
            pendingForegroundSnapshot = snapshot
            return
        }
        pendingForegroundSnapshot = null
        val packageName = snapshot.packageName ?: return
        val excluded = settings.actionSettings.previousApp.packageNames
        if (packageName in excluded || service.packageManager.getLaunchIntentForPackage(packageName) == null) {
            return
        }
        if (currentPackageName != packageName) {
            previousPackageName = currentPackageName
            currentPackageName = packageName
            if (previousPackageName == null) {
                previousPackageName = currentPackageName
            }
        }
    }

    override suspend fun handle(request: ActionRequest) {
        when (request.action.value) {
            GlobalActions.PREVIOUS_APP -> switchPreviousApp()
            GlobalActions.KILL_APP -> killCurrentApp()
            GlobalActions.POPUP_SCREEN -> launchCurrentAppInPopup()
            GlobalActions.EXTRA_LAUNCH_APP -> request.action.appInfo?.let { appInfo ->
                val settings = settingsStore.currentSnapshotOrNull() ?: return
                service.launchAppInfo(
                    appInfo,
                    appInfo.miniWindow,
                    settings.actionSettings.miniWindow
                )
            }
            GlobalActions.EXTRA_LAUNCH_SHORTCUT -> request.action.shortcutInfo?.let { shortcutInfo ->
                val settings = settingsStore.currentSnapshotOrNull() ?: return
                service.launchShortcutInfo(
                    shortcutInfo,
                    shortcutInfo.miniWindow,
                    settings.actionSettings.miniWindow
                )
            }
        }
    }

    private fun switchPreviousApp() {
        val previous = previousPackageName ?: return
        val current = currentPackageName ?: return
        if (service.rootInActiveWindow?.packageName?.toString() != current) {
            launchPackage(current)
            return
        }
        if (previous == current || !launchPackage(previous)) return
        previousPackageName = current
        currentPackageName = previous
    }

    private fun launchPackage(packageName: String): Boolean {
        val intent = service.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
        }.isSuccess
    }

    private suspend fun killCurrentApp() {
        val packageName = currentPackageName ?: return
        if (environmentMonitor.isLauncherForeground() ||
            packageName == service.packageName ||
            !PACKAGE_NAME_REGEX.matches(packageName)
        ) {
            return
        }
        val result = withContext(Dispatchers.IO) {
            ShellActionExecutor.execute("am force-stop $packageName")
        }
        if (!result.isSuccess) {
            service.showShellFailureToast(result)
        }
    }

    private fun launchCurrentAppInPopup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            showVersionTooLowToast(service, R.string.action_popup_screen)
            return
        }
        val packageName = currentPackageName ?: return
        val settings = settingsStore.currentSnapshotOrNull() ?: return
        if (environmentMonitor.isLauncherForeground()) return
        val intent = Intent(Intent.ACTION_MAIN)
            .setPackage(packageName)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        val className = service.packageManager
            .queryIntentActivitiesCompat(intent, PackageManager.MATCH_ALL)
            .firstOrNull()
            ?.activityInfo
            ?.name
            ?: return
        service.launchAppInPopup(
            packageName,
            className,
            settings.actionSettings.miniWindow
        )
    }
}
