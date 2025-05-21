package com.aaron.sidegesture.constant

import com.aaron.sidegesture.entity.Action

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/21
 */
object GlobalActions {

    /** 无 */
    const val NONE = "0"
    /** 返回键 */
    const val BACK = "1"
    /** 主页键 */
    const val HOME = "2"
    /** 最近键 */
    const val RECENT = "3"
    /** 菜单键 */
    const val MENU = "4"
    /** 在应用程序中搜索 */
    const val SEARCH_IN_APP = "5"
    /** 音量增加键 */
    const val VOLUME_UP = "6"
    /** 音量减小键 */
    const val VOLUME_DOWN = "7"
    /** 静音开关 */
    const val MUTE = "8"
    /** 播放/暂停歌曲 */
    const val PLAY_PAUSE_SONG = "9"
    /** 上一首 */
    const val LAST_SONG = "10"
    /** 下一首 */
    const val NEXT_SONG = "11"
    /** 上一个应用程序 */
    const val PREVIOUS_APP = "12"
    /** 应用程序屏幕 */
    const val APP_SCREEN = "13"
    /** 前进（网络浏览器） */
    const val WEB_FORWARD = "14"
    /** 打开通知面板 */
    const val OPEN_NOTIFICATION_PANEL = "15"
    /** 打开快捷面板 */
    const val OPEN_QUICK_PANEL = "16"
    /** 锁屏 */
    const val LOCK_SCREEN = "17"
    /** 关闭应用程序 */
    const val KILL_APP = "18"
    /** 手电筒 */
    const val FLASHLIGHT = "19"
    /** 分屏视图 */
    const val SPLIT_SCREEN = "20"
    /** 应用小窗 */
    const val POPUP_SCREEN = "21"
    /** 语音助手 */
    const val ASSIST_APP = "22"
    /** 搜索查找 */
    const val SEARCH = "23"
    /** 截屏 */
    const val SCREENSHOT = "24"
    /** 录屏 */
    const val RECORD_SCREEN = "25"
    /** 显示/隐藏导航栏 */
    const val SHOW_HIDE_NAV_BAR = "26"
    /** 向下拉屏幕 */
    const val PULL_SCREEN_DOWN = "27"
    /** 单手模式 */
    const val EASY_ONE_HAND = "28"
    /** 电源键菜单 */
    const val POWER_BUTTON = "29"
    /** 自动旋转 */
    const val AUTO_ROTATE = "30"
    /** 向右旋转 */
    const val ROTATE_RIGHT = "31"
    /** 向左旋转 */
    const val ROTATE_LEFT = "32"
    /** 颜色反转 */
    const val INVERSE_COLOR = "33"
    /** 应用程序面板 */
    const val QUICK_APP_PANEL = "34"
    /** 启动应用程序 */
    const val LAUNCH_APP = "35"
    /** 小窗启动应用程序 */
    const val LAUNCH_APP_IN_POPUP = "36"
    /** 任务切换器 */
    const val TASK_SWITCHER = "37"
    /** 快速工具 */
    const val QUICK_TOOLS = "38"
    /** 悬浮球 */
    const val FLOAT_BALL = "39"
    /** 隐藏触钮 */
    const val HIDE_GESTURE_BUTTON = "40"
    /** 微信扫一扫 */
    const val WECHAT_SCAN = "41"
    /** 微信付款码 */
    const val WECHAT_PAY = "42"
    /** 支付宝扫一扫 */
    const val ALIPAY_SCAN = "43"
    /** 支付宝付款码 */
    const val ALIPAY_PAY = "44"
    /** 移动屏幕 */
    const val MOVE_SCREEN = "45"

    /** 启动应用 */
    const val EXTRA_LAUNCH_APP = "101"

    val all = Action.toList(
        NONE, BACK, HOME, RECENT, /*MENU,*/ /*SEARCH_IN_APP,*/ VOLUME_UP, VOLUME_DOWN, MUTE, PLAY_PAUSE_SONG,
        LAST_SONG, NEXT_SONG, PREVIOUS_APP, /*APP_SCREEN,*/ /*WEB_FORWARD,*/ OPEN_NOTIFICATION_PANEL,
        OPEN_QUICK_PANEL, LOCK_SCREEN, /*KILL_APP,*/ FLASHLIGHT, /*SPLIT_SCREEN,*/ /*POPUP_SCREEN,*/ ASSIST_APP,
        /*SEARCH,*/ SCREENSHOT, /*RECORD_SCREEN,*/ /*SHOW_HIDE_NAV_BAR,*/ /*PULL_SCREEN_DOWN,*/ /*EASY_ONE_HAND,*/ POWER_BUTTON,
        /*AUTO_ROTATE,*/ /*ROTATE_RIGHT,*/ /*ROTATE_LEFT,*/ /*INVERSE_COLOR,*/ /*QUICK_APP_PANEL,*/ /*LAUNCH_APP,*/ /*LAUNCH_APP_IN_POPUP,*/
        /*TASK_SWITCHER,*/ /*QUICK_TOOLS,*/ /*FLOAT_BALL,*/ /*HIDE_GESTURE_BUTTON,*/MOVE_SCREEN, WECHAT_SCAN, WECHAT_PAY, ALIPAY_SCAN, ALIPAY_PAY
    )
}