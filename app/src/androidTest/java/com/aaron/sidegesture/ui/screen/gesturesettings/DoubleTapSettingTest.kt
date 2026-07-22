package com.aaron.sidegesture.ui.screen.gesturesettings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DoubleTapSettingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingDisplaysExplanationAndUpdatesValue() {
        var checked by mutableStateOf(false)
        var callbackValue = false
        composeTestRule.setContent {
            MaterialTheme {
                DoubleTapSetting(
                    checked = checked,
                    onCheckedChange = {
                        callbackValue = it
                        checked = it
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("双击").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("仅已配置双击动作的触钮会等待第二次点击")
            .assertIsDisplayed()
        composeTestRule.onNode(isToggleable()).assertIsOff().performClick()
        composeTestRule.waitForIdle()

        assertTrue(callbackValue)
        composeTestRule.onNode(isToggleable()).assertIsOn()
    }
}
