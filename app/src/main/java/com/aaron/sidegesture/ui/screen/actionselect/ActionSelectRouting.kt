package com.aaron.sidegesture.ui.screen.actionselect

import com.aaron.sidegesture.constant.GlobalActions

enum class ActionSelectPrimaryTarget {
    Selection,
    QuickLauncherItems,
    Settings
}

enum class ActionSelectSettingsTarget {
    Generic,
    QuickTools,
    QuickLauncher,
    Shell
}

fun resolveActionSelectPrimaryTarget(
    actionValue: String,
    selectSingle: Boolean,
    isSelected: Boolean
): ActionSelectPrimaryTarget {
    return when {
        actionValue == GlobalActions.QUICK_LAUNCHER ->
            ActionSelectPrimaryTarget.QuickLauncherItems
        actionValue == GlobalActions.SHIZUKU_SHELL && (selectSingle || !isSelected) ->
            ActionSelectPrimaryTarget.Settings
        else -> ActionSelectPrimaryTarget.Selection
    }
}

fun resolveActionSelectSettingsTarget(actionValue: String): ActionSelectSettingsTarget {
    return when (actionValue) {
        GlobalActions.QUICK_TOOLS -> ActionSelectSettingsTarget.QuickTools
        GlobalActions.QUICK_LAUNCHER -> ActionSelectSettingsTarget.QuickLauncher
        GlobalActions.SHIZUKU_SHELL -> ActionSelectSettingsTarget.Shell
        else -> ActionSelectSettingsTarget.Generic
    }
}
