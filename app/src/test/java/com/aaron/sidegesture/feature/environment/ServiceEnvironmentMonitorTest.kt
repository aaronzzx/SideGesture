package com.aaron.sidegesture.feature.environment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceEnvironmentMonitorTest {

    @Test
    fun observesImeWhenEitherRelatedSettingIsEnabled() {
        assertFalse(shouldObserveIme(fitSoftKeyboard = false, hideGestureOnIme = false))
        assertTrue(shouldObserveIme(fitSoftKeyboard = true, hideGestureOnIme = false))
        assertTrue(shouldObserveIme(fitSoftKeyboard = false, hideGestureOnIme = true))
        assertTrue(shouldObserveIme(fitSoftKeyboard = true, hideGestureOnIme = true))
    }

    @Test
    fun resolvesVisibleImeAndItsBottomPadding() {
        val state = resolveImeWindowState(
            windowPresent = true,
            boundsTop = 1400,
            boundsHeight = 600,
            screenHeight = 2000
        )

        assertTrue(state.visible)
        assertEquals(600, state.padding)
    }

    @Test
    fun keepsImeVisibleWhenWindowBoundsCannotProvidePadding() {
        val state = resolveImeWindowState(
            windowPresent = true,
            boundsTop = 2000,
            boundsHeight = 0,
            screenHeight = 2000
        )

        assertTrue(state.visible)
        assertEquals(0, state.padding)
    }

    @Test
    fun resolvesMissingImeWindowAsHidden() {
        val state = resolveImeWindowState(
            windowPresent = false,
            boundsTop = 0,
            boundsHeight = 0,
            screenHeight = 2000
        )

        assertFalse(state.visible)
        assertEquals(0, state.padding)
    }
}
