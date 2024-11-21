package com.aaron.sidegesture.config

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/16
 */

object Actions {

    const val NONE = 0
    const val BACK = 1
    const val HOME = 2
    const val RECENT = 3
    const val MENU = 4
    const val SEARCH_IN_APP = 5
    const val VOLUME_UP = 6
    const val VOLUME_DOWN = 7
    const val MUTE = 8
    const val LOCK_SCREEN = 9
    const val PREVIOUS_APP = 10
    const val WECHAT_SCAN = 11
    const val WECHAT_PAY = 12
    const val ALIPAY_SCAN = 13
    const val ALIPAY_PAY = 14
}

sealed interface Action {

    data object Back : Action
    data object Home : Action
    data object Recent : Action
    data object Menu : Action
    data object Mute : Action
    data object LockScreen : Action
    data object PreviousApp : Action
    data object WechatScan : Action
    data object WechatPay : Action
    data object AlipayScan : Action
    data object AlipayPay : Action
    data class LaunchApp(val packageName: String) : Action

    sealed interface PendingAction : Action
    data object QuickSettings : PendingAction

    sealed interface QuickStartAction : PendingAction
    data class AppPanel(val packageNames: List<String>) : QuickStartAction
}