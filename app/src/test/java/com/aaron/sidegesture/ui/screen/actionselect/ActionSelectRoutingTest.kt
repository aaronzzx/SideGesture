package com.aaron.sidegesture.ui.screen.actionselect

import com.aaron.sidegesture.constant.GlobalActions
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionSelectRoutingTest {

    @Test
    fun quickLauncherPrimaryClickEditsItemsWhileSettingsButtonOpensAppearance() {
        assertEquals(
            ActionSelectPrimaryTarget.QuickLauncherItems,
            resolveActionSelectPrimaryTarget(
                actionValue = GlobalActions.QUICK_LAUNCHER,
                selectSingle = true,
                isSelected = false
            )
        )
        assertEquals(
            ActionSelectSettingsTarget.QuickLauncher,
            resolveActionSelectSettingsTarget(GlobalActions.QUICK_LAUNCHER)
        )
    }

    @Test
    fun existingSettingsRoutesRemainUnchanged() {
        assertEquals(
            ActionSelectSettingsTarget.QuickTools,
            resolveActionSelectSettingsTarget(GlobalActions.QUICK_TOOLS)
        )
        assertEquals(
            ActionSelectSettingsTarget.Shell,
            resolveActionSelectSettingsTarget(GlobalActions.SHIZUKU_SHELL)
        )
        assertEquals(
            ActionSelectSettingsTarget.Generic,
            resolveActionSelectSettingsTarget(GlobalActions.PREVIOUS_APP)
        )
        assertEquals(
            ActionSelectPrimaryTarget.Settings,
            resolveActionSelectPrimaryTarget(
                actionValue = GlobalActions.SHIZUKU_SHELL,
                selectSingle = true,
                isSelected = false
            )
        )
    }
}
