package com.aaron.sidegesture.ktx

import androidx.datastore.core.CorruptionException
import com.aaron.sidegesture.defaults.MoveScreenStyleMigration
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.QuickLauncherSettings
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class DataStoreSerializerTest {

    @Test
    fun readFromAppliesMoveScreenDefaultsToOldActionSettingsJson() = runBlocking {
        val serializer = createActionSettingsSerializer()
        val oldJson = """
            {
              "moveScreen": {
                "rate": 1.5,
                "hoverDelayMs": 500,
                "radius": 24
              }
            }
        """.trimIndent()

        val value = serializer.readFrom(oldJson.byteInputStream())

        assertEquals(ActionSettings.MoveScreen.Style.Crosshair, value.moveScreen.style)
        assertTrue(value.moveScreen.popupEnabled)
    }

    @Test
    fun readFromAppliesQuickLauncherDefaultsToOldActionSettingsJson() = runBlocking {
        val serializer = createActionSettingsSerializer()
        val oldJson = """
            {
              "moveScreen": {
                "rate": 1.5,
                "hoverDelayMs": 500,
                "radius": 24
              },
              "gotoBottom": {
                "strength": 8
              }
            }
        """.trimIndent()

        val value = serializer.readFrom(oldJson.byteInputStream())

        assertEquals(QuickLauncherSettings(), value.quickLauncher)
        assertEquals(4, value.quickLauncher.rows)
        assertEquals(4, value.quickLauncher.columns)
        assertEquals(44, value.quickLauncher.iconSizeDp)
        assertEquals(11, value.quickLauncher.textSizeSp)
    }

    @Test
    fun moveScreenStyleMigrationForcesLegacyMagnifierToCrosshair() = runBlocking {
        val legacy = ActionSettings(
            moveScreen = ActionSettings.MoveScreen(
                rate = 1.5f,
                hoverDelayMs = 500L,
                radius = 24,
                style = ActionSettings.MoveScreen.Style.Magnifier,
                popupEnabled = false
            )
        )

        assertTrue(MoveScreenStyleMigration.shouldMigrate(legacy))

        val migrated = MoveScreenStyleMigration.migrate(legacy)

        assertEquals(ActionSettings.MoveScreen.Style.Crosshair, migrated.moveScreen.style)
        assertEquals(1.5f, migrated.moveScreen.rate)
        assertEquals(500L, migrated.moveScreen.hoverDelayMs)
        assertEquals(24, migrated.moveScreen.radius)
        assertEquals(false, migrated.moveScreen.popupEnabled)
        assertEquals(false, MoveScreenStyleMigration.shouldMigrate(migrated))
    }

    @Test
    fun readFromIgnoresUnknownActionSettingsKeys() = runBlocking {
        val serializer = createActionSettingsSerializer()
        val oldJson = """
            {
              "unknownTopLevel": true,
              "moveScreen": {
                "rate": 1.5,
                "hoverDelayMs": 500,
                "radius": 24,
                "unknownMoveScreenField": "ignored"
              }
            }
        """.trimIndent()

        val value = serializer.readFrom(oldJson.byteInputStream())

        assertEquals(1.5f, value.moveScreen.rate)
        assertEquals(500L, value.moveScreen.hoverDelayMs)
        assertEquals(24, value.moveScreen.radius)
    }

    @Test
    fun writeToWritesNonEmptyJson() = runBlocking {
        val serializer = createJsonDataStoreSerializer(TestSettings())
        val value = TestSettings(enabled = false, count = 3)
        val output = ByteArrayOutputStream()

        serializer.writeTo(value, output)

        assertTrue(output.toByteArray().isNotEmpty())
        assertEquals(value, serializer.readFrom(output.toByteArray().inputStream()))
    }

    @Test
    fun readFromThrowsCorruptionExceptionForInvalidJson() = runBlocking {
        val serializer = createJsonDataStoreSerializer(TestSettings())

        try {
            serializer.readFrom("{".byteInputStream())
            fail("Expected CorruptionException")
        } catch (e: CorruptionException) {
            assertTrue(e.cause is SerializationException)
        }
    }

    @Test
    fun writeToPropagatesIoException() = runBlocking {
        val serializer = createJsonDataStoreSerializer(TestSettings())

        try {
            serializer.writeTo(TestSettings(), FailingOutputStream())
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("write failed", e.message)
        }
    }

    @Serializable
    private data class TestSettings(
        val enabled: Boolean = true,
        val count: Int = 1
    )

    private fun createActionSettingsSerializer() = createJsonDataStoreSerializer(
        ActionSettings(moveScreen = ActionSettings.MoveScreen(radius = 12))
    )

    private class FailingOutputStream : OutputStream() {
        override fun write(b: Int) {
            throw IOException("write failed")
        }
    }
}
