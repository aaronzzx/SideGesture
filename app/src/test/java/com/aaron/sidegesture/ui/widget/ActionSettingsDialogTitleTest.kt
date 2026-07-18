package com.aaron.sidegesture.ui.widget

import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionSettingsDialogTitleTest {

    @Test
    fun previousAppUsesExclusionListTitle() {
        assertEquals(
            "上一个应用排除列表",
            resolveActionSettingsDialogTitle(
                action = Action(GlobalActions.PREVIOUS_APP),
                defaultTitle = "上一个应用程序",
                previousAppTitle = "上一个应用排除列表"
            )
        )
    }

    @Test
    fun otherActionsKeepTheirCommonTitle() {
        assertEquals(
            "返回键",
            resolveActionSettingsDialogTitle(
                action = Action(GlobalActions.BACK),
                defaultTitle = "返回键",
                previousAppTitle = "上一个应用排除列表"
            )
        )
    }
}
