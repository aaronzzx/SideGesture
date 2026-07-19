package com.aaron.sidegesture.ui.screen.advancedsettings

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

class AdvancedSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hideGestureOnImeSettingDisplaysExplanationAndUpdatesValue() {
        var checked by mutableStateOf(false)
        var callbackValue = false
        composeTestRule.setContent {
            MaterialTheme {
                HideGestureOnImeSetting(
                    checked = checked,
                    onCheckedChange = {
                        callbackValue = it
                        checked = it
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("输入法时隐藏触钮").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("输入法弹出时隐藏并禁用所有边缘触钮")
            .assertIsDisplayed()
        composeTestRule.onNode(isToggleable()).assertIsOff().performClick()
        composeTestRule.waitForIdle()

        assertTrue(callbackValue)
        composeTestRule.onNode(isToggleable()).assertIsOn()
    }
}
