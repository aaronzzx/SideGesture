package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aaron.sidegesture.App
import com.aaron.sidegesture.constant.GestureActionsDefaults.ActionNone
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.LauncherInfo
import com.aaron.sidegesture.entity.QuickLauncherActionData
import com.aaron.sidegesture.entity.ShellCommandActionData
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.utils.JsonHelper

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

val Action.appInfo: AppInfo? get() {
    if (value == GlobalActions.EXTRA_LAUNCH_APP) {
        return runCatching {
            JsonHelper.decodeFromString<AppInfo>(data)
        }.getOrNull()
    }
    return null
}

val Action.shortcutInfo: LauncherInfo.ShortcutInfo? get() {
    if (value == GlobalActions.EXTRA_LAUNCH_SHORTCUT) {
        return runCatching {
            JsonHelper.decodeFromString<LauncherInfo.ShortcutInfo>(data)
        }.getOrNull()
    }
    return null
}

val Action.shellCommandActionData: ShellCommandActionData? get() {
    if (value == GlobalActions.SHIZUKU_SHELL) {
        return runCatching {
            JsonHelper.decodeFromString<ShellCommandActionData>(data)
        }.getOrNull()
    }
    return null
}

val Action.quickLauncherActionData: QuickLauncherActionData? get() {
    if (value == GlobalActions.QUICK_LAUNCHER) {
        return runCatching {
            JsonHelper.decodeFromString<QuickLauncherActionData>(data)
        }.getOrNull()
    }
    return null
}

@Composable
fun GestureActions.actionTextCompose(): String {
    var text = ""
    val centerText = center.actionTextCompose(true)
    if (centerText.isNotEmpty()) {
        text += centerText
    }
    val upText = up.actionTextCompose(true)
    if (upText.isNotEmpty()) {
        text += if (text.isEmpty()) {
            upText
        } else {
            ",$upText"
        }
    }
    val downText = down.actionTextCompose(true)
    if (downText.isNotEmpty()) {
        text += if (text.isEmpty()) {
            downText
        } else {
            ",$downText"
        }
    }
    return text
}

@Composable
fun List<Action>.actionTextCompose(emptyIfNone: Boolean = false): String {
    if (size <= 1) {
        val value = firstOrNull() ?: Action.NONE
        return actionText(value, emptyIfNone)
    }
    return remember(this, emptyIfNone) {
        this
            .filter {
                it.value.isNotEmpty() && it.value != GlobalActions.NONE
            }
            .joinToString(separator = ",") {
                App.getContext().actionText(it, emptyIfNone)
            }
    }
}

fun GestureActions.actionsBy(direction: TriggerDirection): List<Action> {
    return when (direction) {
        TriggerDirection.Up -> up
        TriggerDirection.Center -> center
        TriggerDirection.Down -> down
        TriggerDirection.Up2 -> up2
        TriggerDirection.Center2 -> emptyList()
        TriggerDirection.Down2 -> down2
    }
}

fun List<Action>.isEmptyOrNone(): Boolean {
    return isEmpty() || first() == ActionNone
}
