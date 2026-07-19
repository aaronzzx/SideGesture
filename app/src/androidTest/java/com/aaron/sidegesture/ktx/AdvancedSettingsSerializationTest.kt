package com.aaron.sidegesture.ktx

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.entity.global.AdvancedSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class AdvancedSettingsSerializationTest {

    @Test
    fun oldJsonDefaultsHideGestureOnImeToFalse() = runBlocking {
        val serializer = createJsonDataStoreSerializer(AdvancedSettings())
        val oldJson = """
            {
              "fitSoftKeyboard": false
            }
        """.trimIndent()

        val value = serializer.readFrom(oldJson.byteInputStream())

        assertFalse(value.fitSoftKeyboard)
        assertFalse(value.hideGestureOnIme)
    }

    @Test
    fun enabledHideGestureOnImeSurvivesRoundTrip() = runBlocking {
        val serializer = createJsonDataStoreSerializer(AdvancedSettings())
        val output = ByteArrayOutputStream()
        serializer.writeTo(AdvancedSettings(hideGestureOnIme = true), output)

        val value = serializer.readFrom(output.toByteArray().inputStream())

        assertTrue(value.hideGestureOnIme)
    }
}
