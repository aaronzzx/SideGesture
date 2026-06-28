package com.aaron.sidegesture.ktx

import androidx.datastore.core.CorruptionException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DataStoreSerializerTest {

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

    private class FailingOutputStream : OutputStream() {
        override fun write(b: Int) {
            throw IOException("write failed")
        }
    }
}
