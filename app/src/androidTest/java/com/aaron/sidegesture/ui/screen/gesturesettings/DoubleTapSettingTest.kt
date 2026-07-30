package com.aaron.sidegesture.ui.screen.gesturesettings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ktx.LocalNavController
import com.aaron.sidegesture.ui.theme.generator.AppTheme
import org.junit.Rule
import org.junit.Test

class DoubleTapSettingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun gestureSettingsDoesNotShowDoubleTapSwitch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.setContent {
            AppTheme(darkTheme = false, dynamicColor = false) {
                val navController = rememberNavController()
                CompositionLocalProvider(LocalNavController provides navController) {
                    GestureSettingsScreen(
                        onNavToGestureAngles = {},
                        onBack = {}
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.gesture_settings))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.double_tap))
            .assertDoesNotExist()
    }
}
