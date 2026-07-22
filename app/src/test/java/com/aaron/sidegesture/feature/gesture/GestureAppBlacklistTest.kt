package com.aaron.sidegesture.feature.gesture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureAppBlacklistTest {

    @Test
    fun exactPackageNameBlocksGesture() {
        assertTrue(
            isGestureBlockedForApp(
                currentPackageName = "app.target",
                excludeApps = listOf("app.other", "app.target")
            )
        )
    }

    @Test
    fun emptyOrPartialPackageNameDoesNotBlockGesture() {
        assertFalse(
            isGestureBlockedForApp(
                currentPackageName = "",
                excludeApps = listOf("")
            )
        )
        assertFalse(
            isGestureBlockedForApp(
                currentPackageName = "app.target.child",
                excludeApps = listOf("app.target")
            )
        )
    }
}
