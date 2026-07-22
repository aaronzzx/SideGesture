package com.aaron.sidegesture.ktx

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.entity.global.GestureSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class GestureSettingsSerializationTest {

    @Test
    fun oldJsonDefaultsDoubleTapToDisabled() = runBlocking {
        val serializer = createJsonDataStoreSerializer(GestureSettings())

        val value = serializer.readFrom("{}".byteInputStream())

        assertFalse(value.doubleTapEnabled)
    }

    @Test
    fun enabledDoubleTapSurvivesRoundTrip() = runBlocking {
        val serializer = createJsonDataStoreSerializer(GestureSettings())
        val output = ByteArrayOutputStream()
        serializer.writeTo(GestureSettings(doubleTapEnabled = true), output)

        val value = serializer.readFrom(output.toByteArray().inputStream())

        assertTrue(value.doubleTapEnabled)
    }
}
