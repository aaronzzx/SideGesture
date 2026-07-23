package com.aaron.sidegesture.ktx

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.entity.global.GestureSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class GestureSettingsSerializationTest {

    @Test
    fun emptyJsonUsesGestureSettingsDefaults() = runBlocking {
        val serializer = createJsonDataStoreSerializer(GestureSettings())

        val value = serializer.readFrom("{}".byteInputStream())

        assertEquals(GestureSettings(), value)
    }

    @Test
    fun gestureSettingsSurviveRoundTrip() = runBlocking {
        val serializer = createJsonDataStoreSerializer(GestureSettings())
        val expected = GestureSettings(
            longPressTriggerDelayMs = 500L,
            isPreciseSlideType = false
        )
        val output = ByteArrayOutputStream()
        serializer.writeTo(expected, output)

        val value = serializer.readFrom(output.toByteArray().inputStream())

        assertEquals(expected, value)
    }
}
