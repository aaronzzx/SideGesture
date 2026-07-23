package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.utils.JsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppInfoProfileTest {

    @Test
    fun legacyAppInfoDefaultsToCurrentProfileAndKeepsQualifiedName() {
        val appInfo = JsonHelper.decodeFromString<AppInfo>(
            """
                {
                    "packageName": "com.example.app",
                    "className": "com.example.app.MainActivity",
                    "label": "Example"
                }
            """.trimIndent()
        )

        assertNull(appInfo.profileSerialNumber)
        assertEquals(
            "com.example.app/com.example.app.MainActivity",
            appInfo.qualifiedName
        )
    }

    @Test
    fun profileSerialNumberRoundTripsAndSeparatesSameComponent() {
        val currentApp = AppInfo(
            packageName = "com.example.app",
            className = "com.example.app.MainActivity",
            label = "Example"
        )
        val profileApp = currentApp.copy(profileSerialNumber = 12L)

        val restored = JsonHelper.decodeFromString<AppInfo>(
            JsonHelper.encodeToString(profileApp)
        )

        assertEquals(profileApp, restored)
        assertEquals(
            "12@com.example.app/com.example.app.MainActivity",
            restored.qualifiedName
        )
        assertNotEquals(currentApp.qualifiedName, restored.qualifiedName)
    }
}
