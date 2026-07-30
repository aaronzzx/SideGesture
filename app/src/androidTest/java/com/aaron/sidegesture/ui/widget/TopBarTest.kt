package com.aaron.sidegesture.ui.widget

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import com.aaron.sidegesture.ui.theme.generator.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTitleAndInvokesBackAction() {
        var backClicked = false

        composeTestRule.setContent {
            AppTheme(darkTheme = false, dynamicColor = false) {
                TopBar(onBack = { backClicked = true }, title = "测试标题")
            }
        }

        composeTestRule.onNodeWithText("测试标题").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("返回")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue(backClicked)
    }
}
