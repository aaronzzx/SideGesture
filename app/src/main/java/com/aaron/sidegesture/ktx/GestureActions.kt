package com.aaron.sidegesture.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import com.aaron.sidegesture.App
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.LauncherInfo
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.utils.JsonHelper

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */

val Action.appInfo: AppInfo? get() {
    if (value == GlobalActions.EXTRA_LAUNCH_APP) {
        return JsonHelper.decodeFromString<AppInfo>(data)
    }
    return null
}

val Action.shortcutInfo: LauncherInfo.ShortcutInfo? get() {
    if (value == GlobalActions.EXTRA_LAUNCH_SHORTCUT) {
        return JsonHelper.decodeFromString<LauncherInfo.ShortcutInfo>(data)
    }
    return null
}

val Action.offset: IntOffset? get() {
    if (value == GlobalActions.MOVE_SCREEN) {
        val array = data.split(",")
        return IntOffset(array[0].toInt(), array[1].toInt())
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

fun GestureActions.ohoActionsBy(direction: TriggerDirection): List<Action> {
    return when (direction) {
        TriggerDirection.Up -> up
        TriggerDirection.Center -> center
        TriggerDirection.Down -> down
    }
}

fun GestureActions.parallelActionsBy(direction: TriggerDirection): List<Action> {
    return when (direction) {
        TriggerDirection.Up -> up2
        TriggerDirection.Center -> emptyList()
        TriggerDirection.Down -> down2
    }
}