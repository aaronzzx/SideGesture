package com.aaron.sidegesture

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.aaron.sidegesture.constant.ActionSettingsDefaults.GotoBottomStrength
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.MoveScreenData
import com.aaron.sidegesture.entity.RecentTask
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.dispatchMediaKeyEvent
import com.aaron.sidegesture.ktx.gotoAlipayPayCode
import com.aaron.sidegesture.ktx.gotoAlipayScan
import com.aaron.sidegesture.ktx.gotoAppDetailSettings
import com.aaron.sidegesture.ktx.gotoWechat
import com.aaron.sidegesture.ktx.gotoWechatScan
import com.aaron.sidegesture.ktx.isMiniWindow
import com.aaron.sidegesture.ktx.launchAppInPopup
import com.aaron.sidegesture.ktx.launchAppInfo
import com.aaron.sidegesture.ktx.launchAssist
import com.aaron.sidegesture.ktx.launchShortcutInfo
import com.aaron.sidegesture.ktx.queryIntentActivitiesCompat
import com.aaron.sidegesture.ktx.shellCommandActionData
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ktx.toggleMute
import com.aaron.sidegesture.ktx.volumeDown
import com.aaron.sidegesture.ktx.volumeUp
import com.aaron.sidegesture.shizuku.ShellResult
import com.aaron.sidegesture.ui.widget.ActionPanelState.TriggerType
import com.aaron.sidegesture.utils.AccessibilityUtils
import com.aaron.sidegesture.utils.FlashlightController
import com.aaron.sidegesture.utils.JsonHelper
import com.aaron.sidegesture.utils.ShellActionExecutor
import com.aaron.sidegesture.utils.showToast
import com.aaron.sidegesture.utils.showVersionTooLowToast
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ScreenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/21
 */
class SideGestureServiceProxy(private val host: SideGestureService) {

    private val recentTaskIdRegex = Regex("""#(\d+)""")
    private val recentTaskPackageRegex = Regex("""A=\d+:(\S+?)[\s}]""")
    private val packageNameRegex = Regex("""^[A-Za-z0-9_.]+$""")

    private var prevPackageName: String? = null
    private var currPackageName: String? = null
    private var currActivityName: String? = null

    private var pendingWechatPay = false
    private var pendingWechatPayAutoCancelJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null

    fun onRelease() {
        wakeLock?.release()
        wakeLock = null
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        host.apply {
            when(event?.eventType){
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = event.packageName?.toString()
                    val className = event.className?.toString()

                    if (isActivity(event.packageName?.toString(), event.className?.toString())) {
                        currActivityName = className
                    }
                    val prevAppExcludePkgNames = host.actionSettings?.previousApp?.packageNames ?: emptyList()
                    if (packageName !in prevAppExcludePkgNames &&
                        hasLaunchIntent(packageName) &&
                        currPackageName != packageName
                    ) {
                        prevPackageName = currPackageName
                        currPackageName = packageName
                        if (prevPackageName == null) {
                            prevPackageName = currPackageName
                        }
                    }

                    if (pendingWechatPay &&
                        Build.VERSION.SDK_INT >= 24 &&
                        packageName == "com.tencent.mm"
                    ) {
                        pendingWechatPayAutoCancelJob?.cancel()
                        pendingWechatPay = false
                        mockClickWechatPay()
                    }
                }
                else -> Unit
            }
        }
    }

    suspend fun queryRecentTasks(): List<RecentTask> = withContext(Dispatchers.IO) {
        val result = ShellActionExecutor.execute("dumpsys activity recents | grep 'Recent #'")
        if (!result.isSuccess) {
            host.showShellFailureToast(result)
            return@withContext emptyList()
        }
        host.parseRecentTasks(result.stdout)
    }

    suspend fun closeRecentTask(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!packageNameRegex.matches(packageName)) {
            return@withContext false
        }
        val result = ShellActionExecutor.execute("am force-stop $packageName")
        if (!result.isSuccess) {
            host.showShellFailureToast(result)
        }
        result.isSuccess
    }

    fun switchToRecentTask(packageName: String) {
        host.queryLaunchIntentAndStart(packageName)
    }

    fun onAction(action: Action) {
        host.onAction(action)
    }

    private fun SideGestureService.onAction(action: Action) {
        when (action.value) {
            GlobalActions.BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            GlobalActions.HOME -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            GlobalActions.RECENT -> {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
            GlobalActions.VOLUME_UP -> {
                volumeUp()
            }
            GlobalActions.VOLUME_DOWN -> {
                volumeDown()
            }
            GlobalActions.MUTE -> {
                toggleMute()
            }
            GlobalActions.PLAY_PAUSE_SONG -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            GlobalActions.LAST_SONG -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
            GlobalActions.NEXT_SONG -> {
                dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
            }
            GlobalActions.PREVIOUS_APP -> {
                previousApp()
            }
            GlobalActions.KILL_APP -> {
                val curPkgName = currPackageName
                if (curPkgName.isNullOrEmpty()) {
                    return
                }
                if (nowInLauncher()) {
                    return
                }
                if (curPkgName == packageName) {
                    return
                }
                coroutineScope.launch(Dispatchers.IO) {
                    if (!packageNameRegex.matches(curPkgName)) {
                        return@launch
                    }
                    val result = ShellActionExecutor.execute("am force-stop $curPkgName")
                    if (!result.isSuccess) {
                        showShellFailureToast(result)
                    }
                }
            }
            GlobalActions.OPEN_NOTIFICATION_PANEL -> {
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
            GlobalActions.OPEN_QUICK_PANEL -> {
                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            }
            GlobalActions.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else {
                    showVersionTooLowToast(this, R.string.action_lock_screen)
                }
            }
            GlobalActions.FLASHLIGHT -> {
                if (FlashlightController.isAvailable(this)) {
                    val block = {
                        coroutineScope.launch(Dispatchers.Default) {
                            val success = FlashlightController.toggle(this@onAction)
                            if (!success) {
                                showToast(R.string.flashlight_failed)
                            }
                        }
                    }
                    if (PermissionUtils.isGranted(Manifest.permission.CAMERA)) {
                        block()
                    } else {
                        showToast(R.string.grant_camera_permission)
                        PermissionUtils
                            .permission(Manifest.permission.CAMERA)
                            .callback { isAllGranted, granted, deniedForever, denied ->
                                if (isAllGranted) {
                                    block()
                                } else if (deniedForever.isNotEmpty()) {
                                    showToast(R.string.goto_grant_camera_permission)
                                    gotoAppDetailSettings()
                                }
                            }
                            .request()
                    }
                } else {
                    showToast(R.string.flashlight_failed)
                }
            }
            GlobalActions.SPLIT_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                } else {
                    showVersionTooLowToast(this, R.string.action_split_screen)
                }
            }
            GlobalActions.POPUP_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val curPkgName = currPackageName
                    if (nowInLauncher() || curPkgName.isNullOrEmpty()) {
                        return
                    }
                    val intent = Intent().apply {
                        setPackage(curPkgName)
                        setAction(Intent.ACTION_MAIN)
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val resolveInfo = packageManager
                        .queryIntentActivitiesCompat(intent, PackageManager.MATCH_ALL)
                        .firstOrNull()
                    val className = resolveInfo?.activityInfo?.name
                    if (!className.isNullOrEmpty()) {
                        launchAppInPopup(curPkgName, className, actionSettings?.miniWindow ?: ActionSettings.MiniWindow())
                    }
                } else {
                    showVersionTooLowToast(this, R.string.action_popup_screen)
                }
            }
            GlobalActions.ASSIST_APP -> {
                launchAssist()
            }
            GlobalActions.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    coroutineScope.launch {
                        delay(500)
                        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                    }
                } else {
                    showVersionTooLowToast(this, R.string.action_screenshot)
                }
            }
            GlobalActions.POWER_BUTTON -> {
                performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            }
            GlobalActions.WECHAT_SCAN -> {
                gotoWechatScan()
            }
            GlobalActions.WECHAT_PAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val isCurrInWechatHome = currActivityName == "com.tencent.mm.ui.LauncherUI"
                    gotoWechat()
                    if (!isCurrInWechatHome) {
                        pendingWechatPayAutoCancelJob?.cancel()
                        pendingWechatPayAutoCancelJob = coroutineScope.launch {
                            delay(3000)
                            pendingWechatPay = false
                        }
                        pendingWechatPay = true
                    }
                } else {
                    showVersionTooLowToast(this, R.string.action_wechat_pay_simulate_click)
                }
            }
            GlobalActions.ALIPAY_SCAN -> {
                gotoAlipayScan()
            }
            GlobalActions.ALIPAY_PAY -> {
                gotoAlipayPayCode()
            }
            GlobalActions.EXTRA_LAUNCH_APP -> {
                val advancedSettings = advancedSettings ?: return
                val appInfo = action.appInfo
                if (appInfo != null) {
                    val longPressLaunchPopup = advancedSettings.actionPanelAppLongPressLaunchPopup
                    val triggerType = action.extra as? TriggerType
                    val miniWindow = triggerType?.isMiniWindow(!appInfo.miniWindow && longPressLaunchPopup) ?: false
                    launchAppInfo(appInfo, miniWindow, actionSettings?.miniWindow ?: ActionSettings.MiniWindow())
                }
            }
            GlobalActions.EXTRA_LAUNCH_SHORTCUT -> {
                val advancedSettings = advancedSettings ?: return
                val shortcutInfo = action.shortcutInfo
                if (shortcutInfo != null) {
                    val longPressLaunchPopup = advancedSettings.actionPanelAppLongPressLaunchPopup
                    val triggerType = action.extra as? TriggerType
                    val miniWindow = triggerType?.isMiniWindow(!shortcutInfo.miniWindow && longPressLaunchPopup) ?: false
                    launchShortcutInfo(shortcutInfo, miniWindow, actionSettings?.miniWindow ?: ActionSettings.MiniWindow())
                }
            }
            GlobalActions.MOVE_SCREEN -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    showVersionTooLowToast(this, R.string.action_move_screen)
                    return
                }
                if (gestureSettings?.longSlideTriggerImmediately != true) {
                    showToast(R.string.move_screen_disabled_cause_long_slide_trigger_immediately)
                    return
                }
                val data = JsonHelper.decodeFromString<MoveScreenData>(action.data)
                if (data.x in 0..ScreenUtils.getScreenWidth() &&
                    data.y in 0..ScreenUtils.getScreenHeight()
                ) {
                    when (data.action) {
                        ActionSettings.MoveScreen.Action.LongPress -> {
                            AccessibilityUtils.longPress(host, data.x, data.y)
                        }
                        ActionSettings.MoveScreen.Action.DoubleTap -> {
                            AccessibilityUtils.doubleTap(host, data.x, data.y)
                        }
                        ActionSettings.MoveScreen.Action.Tap -> {
                            AccessibilityUtils.click(host, data.x, data.y)
                        }
                        else -> Unit
                    }
                }
            }
            GlobalActions.KEEP_SCREEN_ON -> {
                if (wakeLock != null) {
                    wakeLock?.release()
                    wakeLock = null
                    showToast(R.string.disable_keep_screen_on)
                } else {
                    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "gulugulu:KeepScreenOn")
                    wakeLock?.acquire()
                    showToast(R.string.enable_keep_screen_on)
                }
            }
            GlobalActions.BACK_TO_TOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    AccessibilityUtils.fastVerticalScroll(host, true)
                } else {
                    showVersionTooLowToast(this, R.string.action_back_to_top)
                }
            }
            GlobalActions.GOTO_BOTTOM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val strength = host.actionSettings?.gotoBottom?.strength ?: GotoBottomStrength
                    AccessibilityUtils.fastVerticalScroll(host, false, strength)
                } else {
                    showVersionTooLowToast(this, R.string.action_goto_bottom)
                }
            }
            GlobalActions.SHIZUKU_SHELL -> {
                val command = action.shellCommandActionData?.command.orEmpty()
                if (command.isBlank()) {
                    showToast(R.string.shell_command_empty)
                    return
                }
                coroutineScope.launch(Dispatchers.IO) {
                    val result = ShellActionExecutor.execute(command)
                    if (result.isSuccess) {
                        return@launch
                    }
                    showShellFailureToast(result)
                }
            }
        }
    }

    private fun SideGestureService.parseRecentTasks(output: String): List<RecentTask> {
        return output
            .lineSequence()
            .filter { it.contains("Recent #") }
            .mapNotNull { line ->
                if (line.contains("type=home")) {
                    return@mapNotNull null
                }
                val taskId = recentTaskIdRegex
                    .findAll(line)
                    .lastOrNull()
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?: return@mapNotNull null
                val packageName = recentTaskPackageRegex
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: return@mapNotNull null
                if (packageName == this.packageName) {
                    return@mapNotNull null
                }
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent == null) {
                    return@mapNotNull null
                }
                val label = runCatching {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                }.getOrDefault(packageName)
                RecentTask(
                    taskId = taskId,
                    packageName = packageName,
                    label = label
                )
            }
            .toList()
    }

    private fun SideGestureService.showShellFailureToast(result: ShellResult) {
        val message = when {
            result.timedOut -> getString(R.string.shell_execute_timeout)
            result.errorMessage == "Shizuku permission denied" ||
                    result.errorMessage == "Shizuku binder unavailable" -> {
                getString(R.string.shizuku_permission_required)
            }
            result.errorMessage.isNotBlank() -> {
                getString(R.string.shell_execute_failed, result.errorMessage)
            }
            else -> {
                getString(R.string.shell_execute_failed, result.stderr.ifBlank { "-" })
            }
        }
        showToast(message)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun mockClickWechatPay() {
        val host = host
        host.coroutineScope.launch {
            delay(500)
            val screenWidth = ScreenUtils.getScreenWidth()
            val statusBarHeight = BarUtils.getStatusBarHeight()
            val radius = ConvertUtils.dp2px(12f)
            var x = screenWidth - ConvertUtils.dp2px(14f) - radius
            var y = statusBarHeight + ConvertUtils.dp2px(10f) + radius
            AccessibilityUtils.click(host, x, y)
            delay(500)
            x = screenWidth - ConvertUtils.dp2px(60f) - radius
            y = statusBarHeight + ConvertUtils.dp2px(220f) + radius
            AccessibilityUtils.click(host, x, y)
        }
    }

    private fun AccessibilityService.previousApp() {
        val prevPkgName = prevPackageName
        val curPkgName = currPackageName
        if (prevPkgName.isNullOrEmpty() || curPkgName.isNullOrEmpty()) {
            return
        }
        if (currPackageNameError()) {
            queryLaunchIntentAndStart(curPkgName)
            return
        }
        if (prevPkgName == curPkgName) return
        if (queryLaunchIntentAndStart(prevPkgName)) {
            prevPackageName = curPkgName
            currPackageName = prevPkgName
        }
    }

    private fun AccessibilityService.queryLaunchIntentAndStart(packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) {
            return false
        }
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (ignored: Exception) {
            false
        }
    }

    private fun AccessibilityService.currPackageNameError(): Boolean {
        val pkgName = rootInActiveWindow?.packageName?.toString()
        return pkgName != currPackageName
    }

    private fun AccessibilityService.hasLaunchIntent(packageName: String?): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName ?: "") != null
    }

    private fun isActivity(packageName: String?, className: String?): Boolean {
        packageName ?: return false
        className ?: return false
        return try {
            val component = ComponentName(packageName, className)
            host.packageManager.getActivityInfo(component, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
