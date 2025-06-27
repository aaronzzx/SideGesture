package com.aaron.sidegesture.ktx

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Window
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.ui.theme.icons.PlayPause

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/29
 */

fun Context.actionText(action: Action, emptyIfNone: Boolean = true): String = when (action.value) {
    GlobalActions.BACK -> getString(R.string.action_back)
    GlobalActions.HOME -> getString(R.string.action_home)
    GlobalActions.RECENT -> getString(R.string.action_recent)
    GlobalActions.MENU -> getString(R.string.action_menu)
    GlobalActions.SEARCH_IN_APP -> getString(R.string.action_search_in_app)
    GlobalActions.VOLUME_UP -> getString(R.string.action_volume_up)
    GlobalActions.VOLUME_DOWN -> getString(R.string.action_volume_down)
    GlobalActions.MUTE -> getString(R.string.action_mute)
    GlobalActions.PLAY_PAUSE_SONG -> getString(R.string.action_play_pause_song)
    GlobalActions.LAST_SONG -> getString(R.string.action_last_song)
    GlobalActions.NEXT_SONG -> getString(R.string.action_next_song)
    GlobalActions.PREVIOUS_APP -> getString(R.string.action_previous_app)
    GlobalActions.APP_SCREEN -> getString(R.string.action_app_screen)
    GlobalActions.WEB_FORWARD -> getString(R.string.action_web_forward)
    GlobalActions.OPEN_NOTIFICATION_PANEL -> getString(R.string.action_open_notification_panel)
    GlobalActions.OPEN_QUICK_PANEL -> getString(R.string.action_open_quick_panel)
    GlobalActions.LOCK_SCREEN -> getString(R.string.action_lock_screen)
    GlobalActions.KILL_APP -> getString(R.string.action_kill_app)
    GlobalActions.FLASHLIGHT -> getString(R.string.action_flashlight)
    GlobalActions.SPLIT_SCREEN -> getString(R.string.action_split_screen)
    GlobalActions.POPUP_SCREEN -> getString(R.string.action_popup_screen)
    GlobalActions.ASSIST_APP -> getString(R.string.action_assist_app)
    GlobalActions.SEARCH -> getString(R.string.action_search)
    GlobalActions.SCREENSHOT -> getString(R.string.action_screenshot)
    GlobalActions.RECORD_SCREEN -> getString(R.string.action_record_screen)
    GlobalActions.SHOW_HIDE_NAV_BAR -> getString(R.string.action_show_hide_nav_bar)
    GlobalActions.PULL_SCREEN_DOWN -> getString(R.string.action_pull_screen_down)
    GlobalActions.EASY_ONE_HAND -> getString(R.string.action_easy_one_hand)
    GlobalActions.POWER_BUTTON -> getString(R.string.action_power_button)
    GlobalActions.AUTO_ROTATE -> getString(R.string.action_auto_rotate)
    GlobalActions.ROTATE_RIGHT -> getString(R.string.action_rotate_right)
    GlobalActions.ROTATE_LEFT -> getString(R.string.action_rotate_left)
    GlobalActions.INVERSE_COLOR -> getString(R.string.action_inverse_color)
    GlobalActions.QUICK_APP_PANEL -> getString(R.string.action_quick_app_panel)
    GlobalActions.LAUNCH_APP -> getString(R.string.action_launch_app)
    GlobalActions.LAUNCH_APP_IN_POPUP -> getString(R.string.action_launch_app_in_popup)
    GlobalActions.TASK_SWITCHER -> getString(R.string.action_task_switcher)
    GlobalActions.QUICK_TOOLS -> getString(R.string.action_quick_tools)
    GlobalActions.FLOAT_BALL-> getString(R.string.action_float_ball)
    GlobalActions.HIDE_GESTURE_BUTTON -> getString(R.string.action_hide_gesture_button)
    GlobalActions.WECHAT_SCAN -> getString(R.string.action_wechat_scan)
    GlobalActions.WECHAT_PAY -> getString(R.string.action_wechat_pay_simulate_click)
    GlobalActions.ALIPAY_SCAN -> getString(R.string.action_alipay_scan)
    GlobalActions.ALIPAY_PAY -> getString(R.string.action_alipay_pay)
    GlobalActions.EXTRA_LAUNCH_APP -> action.appInfo?.label ?: ""
    GlobalActions.EXTRA_LAUNCH_SHORTCUT -> action.shortcutInfo?.label ?: ""
    GlobalActions.MOVE_SCREEN -> getString(R.string.action_move_screen)
    GlobalActions.KEEP_SCREEN_ON -> getString(R.string.action_keep_screen_on)
    else -> if (emptyIfNone) "" else getString(R.string.action_none)
}

@Composable
fun actionText(action: Action, emptyIfNone: Boolean = true): String = when (action.value) {
    GlobalActions.BACK -> stringResource(R.string.action_back)
    GlobalActions.HOME -> stringResource(R.string.action_home)
    GlobalActions.RECENT -> stringResource(R.string.action_recent)
    GlobalActions.MENU -> stringResource(R.string.action_menu)
    GlobalActions.SEARCH_IN_APP -> stringResource(R.string.action_search_in_app)
    GlobalActions.VOLUME_UP -> stringResource(R.string.action_volume_up)
    GlobalActions.VOLUME_DOWN -> stringResource(R.string.action_volume_down)
    GlobalActions.MUTE -> stringResource(R.string.action_mute)
    GlobalActions.PLAY_PAUSE_SONG -> stringResource(R.string.action_play_pause_song)
    GlobalActions.LAST_SONG -> stringResource(R.string.action_last_song)
    GlobalActions.NEXT_SONG -> stringResource(R.string.action_next_song)
    GlobalActions.PREVIOUS_APP -> stringResource(R.string.action_previous_app)
    GlobalActions.APP_SCREEN -> stringResource(R.string.action_app_screen)
    GlobalActions.WEB_FORWARD -> stringResource(R.string.action_web_forward)
    GlobalActions.OPEN_NOTIFICATION_PANEL -> stringResource(R.string.action_open_notification_panel)
    GlobalActions.OPEN_QUICK_PANEL -> stringResource(R.string.action_open_quick_panel)
    GlobalActions.LOCK_SCREEN -> stringResource(R.string.action_lock_screen)
    GlobalActions.KILL_APP -> stringResource(R.string.action_kill_app)
    GlobalActions.FLASHLIGHT -> stringResource(R.string.action_flashlight)
    GlobalActions.SPLIT_SCREEN -> stringResource(R.string.action_split_screen)
    GlobalActions.POPUP_SCREEN -> stringResource(R.string.action_popup_screen)
    GlobalActions.ASSIST_APP -> stringResource(R.string.action_assist_app)
    GlobalActions.SEARCH -> stringResource(R.string.action_search)
    GlobalActions.SCREENSHOT -> stringResource(R.string.action_screenshot)
    GlobalActions.RECORD_SCREEN -> stringResource(R.string.action_record_screen)
    GlobalActions.SHOW_HIDE_NAV_BAR -> stringResource(R.string.action_show_hide_nav_bar)
    GlobalActions.PULL_SCREEN_DOWN -> stringResource(R.string.action_pull_screen_down)
    GlobalActions.EASY_ONE_HAND -> stringResource(R.string.action_easy_one_hand)
    GlobalActions.POWER_BUTTON -> stringResource(R.string.action_power_button)
    GlobalActions.AUTO_ROTATE -> stringResource(R.string.action_auto_rotate)
    GlobalActions.ROTATE_RIGHT -> stringResource(R.string.action_rotate_right)
    GlobalActions.ROTATE_LEFT -> stringResource(R.string.action_rotate_left)
    GlobalActions.INVERSE_COLOR -> stringResource(R.string.action_inverse_color)
    GlobalActions.QUICK_APP_PANEL -> stringResource(R.string.action_quick_app_panel)
    GlobalActions.LAUNCH_APP -> stringResource(R.string.action_launch_app)
    GlobalActions.LAUNCH_APP_IN_POPUP -> stringResource(R.string.action_launch_app_in_popup)
    GlobalActions.TASK_SWITCHER -> stringResource(R.string.action_task_switcher)
    GlobalActions.QUICK_TOOLS -> stringResource(R.string.action_quick_tools)
    GlobalActions.FLOAT_BALL-> stringResource(R.string.action_float_ball)
    GlobalActions.HIDE_GESTURE_BUTTON -> stringResource(R.string.action_hide_gesture_button)
    GlobalActions.WECHAT_SCAN -> stringResource(R.string.action_wechat_scan)
    GlobalActions.WECHAT_PAY -> stringResource(R.string.action_wechat_pay_simulate_click)
    GlobalActions.ALIPAY_SCAN -> stringResource(R.string.action_alipay_scan)
    GlobalActions.ALIPAY_PAY -> stringResource(R.string.action_alipay_pay)
    GlobalActions.EXTRA_LAUNCH_APP -> action.appInfo?.label ?: ""
    GlobalActions.EXTRA_LAUNCH_SHORTCUT -> action.shortcutInfo?.label ?: ""
    GlobalActions.MOVE_SCREEN -> stringResource(R.string.action_move_screen)
    GlobalActions.KEEP_SCREEN_ON -> stringResource(R.string.action_keep_screen_on)
    else -> if (emptyIfNone) "" else stringResource(R.string.action_none)
}

@Composable
fun actionIcon(action: Action): Any? = when (action.value) {
    GlobalActions.BACK -> Icons.Default.ArrowBack
    GlobalActions.HOME -> Icons.Default.Home
    GlobalActions.RECENT -> Icons.Default.ViewCarousel
    GlobalActions.MENU -> Icons.Default.Menu
    GlobalActions.SEARCH_IN_APP -> Icons.Default.Search
    GlobalActions.VOLUME_UP -> Icons.Default.VolumeUp
    GlobalActions.VOLUME_DOWN -> Icons.Default.VolumeDown
    GlobalActions.MUTE -> Icons.Default.VolumeMute
    GlobalActions.PLAY_PAUSE_SONG -> Icons.Default.PlayPause
    GlobalActions.LAST_SONG -> Icons.Default.SkipPrevious
    GlobalActions.NEXT_SONG -> Icons.Default.SkipNext
    GlobalActions.PREVIOUS_APP -> Icons.Default.Android
    GlobalActions.WEB_FORWARD -> Icons.Default.Forward
    GlobalActions.OPEN_NOTIFICATION_PANEL -> Icons.Default.Notifications
    GlobalActions.OPEN_QUICK_PANEL -> Icons.Default.Settings
    GlobalActions.LOCK_SCREEN -> Icons.Default.ScreenLockPortrait
    GlobalActions.KILL_APP -> Icons.Default.Close
    GlobalActions.FLASHLIGHT -> Icons.Default.FlashlightOn
    GlobalActions.SPLIT_SCREEN -> Icons.Default.Splitscreen
    GlobalActions.POPUP_SCREEN -> Icons.Default.BrandingWatermark
    GlobalActions.ASSIST_APP -> Icons.Default.Assistant
    GlobalActions.SEARCH -> Icons.Default.Search
    GlobalActions.SCREENSHOT -> Icons.Default.Screenshot
    GlobalActions.RECORD_SCREEN -> Icons.Default.Screenshot
    GlobalActions.SHOW_HIDE_NAV_BAR -> Icons.Default.BarChart
    GlobalActions.PULL_SCREEN_DOWN -> Icons.Default.ArrowCircleDown
    GlobalActions.EASY_ONE_HAND -> Icons.Default.BackHand
    GlobalActions.POWER_BUTTON -> Icons.Default.PowerSettingsNew
    GlobalActions.AUTO_ROTATE -> Icons.Default.ScreenRotation
    GlobalActions.ROTATE_RIGHT -> Icons.Default.RotateRight
    GlobalActions.ROTATE_LEFT -> Icons.Default.RotateLeft
    GlobalActions.INVERSE_COLOR -> Icons.Default.InvertColors
    GlobalActions.QUICK_APP_PANEL -> Icons.Default.Apps
    GlobalActions.LAUNCH_APP -> Icons.Default.Android
    GlobalActions.LAUNCH_APP_IN_POPUP -> Icons.Default.Window
    GlobalActions.TASK_SWITCHER -> Icons.Default.ViewCarousel
    GlobalActions.QUICK_TOOLS -> Icons.Default.Handyman
    GlobalActions.FLOAT_BALL-> Icons.Default.SportsBaseball
    GlobalActions.HIDE_GESTURE_BUTTON -> Icons.Default.Gesture
    GlobalActions.WECHAT_SCAN -> R.drawable.wechat_scan
    GlobalActions.WECHAT_PAY -> R.drawable.wechat_paycode
    GlobalActions.ALIPAY_SCAN -> R.drawable.alipay_scan
    GlobalActions.ALIPAY_PAY -> R.drawable.alipay_paycode
    GlobalActions.EXTRA_LAUNCH_APP -> {
        // 一般是应用被卸载，返回个默认图标防止ActionPanel显示空白
        action.appInfo?.icon ?: Icons.Default.Android
    }
    GlobalActions.EXTRA_LAUNCH_SHORTCUT -> {
        // 一般是应用被卸载，返回个默认图标防止ActionPanel显示空白
        action.shortcutInfo?.icon ?: Icons.Default.Android
    }
    GlobalActions.MOVE_SCREEN -> Icons.Default.AddToHomeScreen
    GlobalActions.KEEP_SCREEN_ON -> Icons.Default.WbSunny
    else -> null
}