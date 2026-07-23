package com.aaron.sidegesture.ui.dialog

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ktx.LocalNavController
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoveScreenSettingsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fastMoveAccelerationSettingFollowsPopupAndPersistsEnabledValue() {
        val originalActionSettings = runBlocking {
            DataStoreHolder.actionSettings.data.first()
        }
        runBlocking {
            DataStoreHolder.actionSettings.updateData {
                originalActionSettings.copy(
                    moveScreen = originalActionSettings.moveScreen.copy(
                        fastMoveAccelerationEnabled = false,
                        popupEnabled = false
                    )
                )
            }
        }
        assertFalse(
            runBlocking {
                DataStoreHolder.actionSettings.data.first()
                    .moveScreen.fastMoveAccelerationEnabled
            }
        )
        val vm = ActionSettingsVM()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            composeTestRule.setContent {
                MaterialTheme {
                    val navController = rememberNavController()
                    CompositionLocalProvider(LocalNavController provides navController) {
                        MoveScreenSettingsContent(vm)
                    }
                }
            }

            composeTestRule
                .onNodeWithText(context.getString(R.string.move_screen_fast_move_acceleration))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(
                    context.getString(R.string.move_screen_fast_move_acceleration_summary)
                )
                .assertIsDisplayed()
            val popupBounds = composeTestRule
                .onNodeWithText(context.getString(R.string.move_screen_popup_enabled))
                .fetchSemanticsNode()
                .boundsInRoot
            val accelerationBounds = composeTestRule
                .onNodeWithText(context.getString(R.string.move_screen_fast_move_acceleration))
                .fetchSemanticsNode()
                .boundsInRoot
            val rateBounds = composeTestRule
                .onNodeWithText(context.getString(R.string.move_screen_rate))
                .fetchSemanticsNode()
                .boundsInRoot
            assertTrue(accelerationBounds.top >= popupBounds.bottom)
            assertTrue(rateBounds.top >= accelerationBounds.bottom)
            composeTestRule
                .onNodeWithText(context.getString(R.string.move_screen_fast_move_acceleration))
                .performClick()

            val saved = runBlocking {
                withTimeout(5_000) {
                    DataStoreHolder.actionSettings.data.first {
                        it.moveScreen.fastMoveAccelerationEnabled
                    }
                }
            }
            assertTrue(saved.moveScreen.fastMoveAccelerationEnabled)
        } finally {
            runBlocking {
                DataStoreHolder.actionSettings.updateData { originalActionSettings }
            }
        }
    }
}
