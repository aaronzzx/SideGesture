package com.aaron.sidegesture.feature.gesture

import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.feature.environment.ImeWindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureButtonImeStateTest {

    @Test
    fun hidesEveryEdgePositionWhileImeIsVisible() {
        enumValues<Position>().forEach { position ->
            val state = resolveGestureButtonImeState(
                position = position,
                fitSoftKeyboard = true,
                hideGestureOnIme = true,
                imeWindowState = ImeWindowState(visible = true, padding = 600)
            )

            assertTrue("$position must be hidden", state.hidden)
            assertEquals("hidden button must not retain IME padding", 0, state.padding)
        }
    }

    @Test
    fun defaultSettingKeepsButtonsVisibleAndOnlyFitsSideButtons() {
        val imeWindowState = ImeWindowState(visible = true, padding = 600)

        enumValues<Position>().forEach { position ->
            val expectedPadding = when (position) {
                Position.Left, Position.Right -> 600
                else -> 0
            }
            assertEquals(
                "$position must keep the existing fitting behavior",
                GestureButtonImeState(hidden = false, padding = expectedPadding),
                resolveGestureButtonImeState(
                    position = position,
                    fitSoftKeyboard = true,
                    hideGestureOnIme = false,
                    imeWindowState = imeWindowState
                )
            )
        }
    }

    @Test
    fun hideSettingDoesNotEnableKeyboardFitting() {
        enumValues<Position>().forEach { position ->
            val state = resolveGestureButtonImeState(
                position = position,
                fitSoftKeyboard = false,
                hideGestureOnIme = true,
                imeWindowState = ImeWindowState(visible = false, padding = 600)
            )

            assertFalse(state.hidden)
            assertEquals(0, state.padding)
        }
    }

    @Test
    fun disablingHideSettingRestoresButtonsWhileImeRemainsVisible() {
        enumValues<Position>().forEach { position ->
            val state = resolveGestureButtonImeState(
                position = position,
                fitSoftKeyboard = false,
                hideGestureOnIme = false,
                imeWindowState = ImeWindowState(visible = true, padding = 600)
            )

            assertFalse("$position must be restored", state.hidden)
            assertEquals(0, state.padding)
        }
    }

    @Test
    fun restoresEveryEdgePositionAfterImeDisappears() {
        enumValues<Position>().forEach { position ->
            val state = resolveGestureButtonImeState(
                position = position,
                fitSoftKeyboard = true,
                hideGestureOnIme = true,
                imeWindowState = ImeWindowState()
            )

            assertFalse("$position must be restored", state.hidden)
            assertEquals(0, state.padding)
        }
    }
}
