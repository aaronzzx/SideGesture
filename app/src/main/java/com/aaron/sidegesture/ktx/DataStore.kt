package com.aaron.sidegesture.ktx

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/24
 */

inline fun <reified T> Context.dataStore(
    fileName: String,
    defValue: T,
    corruptionValue: T = defValue,
    migrations: List<DataMigration<T>> = emptyList()
): DataStore<T> {
    val serializer = createJsonDataStoreSerializer(defValue)
    return MultiProcessDataStoreFactory.create(
        serializer = serializer,
        corruptionHandler = ReplaceFileCorruptionHandler { corruptionValue },
        migrations = migrations,
        produceFile = {
            File(filesDir, "ds/$fileName")
        }
    )
}

inline fun <reified T> createJsonDataStoreSerializer(defValue: T): Serializer<T> {
    return object : Serializer<T> {
        override val defaultValue: T = defValue

        override suspend fun readFrom(input: InputStream): T {
            val string = input.readBytes().decodeToString()
            return try {
                JsonHelper.decodeFromString<T>(string)
            } catch (e: SerializationException) {
                throw CorruptionException("Unable to deserialize DataStore value.", e)
            }
        }

        override suspend fun writeTo(t: T, output: OutputStream) {
            val string = JsonHelper.encodeToString(t)
            output.write(string.encodeToByteArray())
        }
    }
}
