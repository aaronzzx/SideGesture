package com.aaron.sidegesture.ktx

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import com.aaron.sidegesture.utils.JsonHelper
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/24
 */

inline fun <reified T> Context.dataStore(fileName: String, defValue: T): DataStore<T> {
    val serializer = object : Serializer<T> {
        override val defaultValue: T = defValue

        override suspend fun readFrom(input: InputStream): T {
            return try {
                val string = input.readBytes().decodeToString()
                JsonHelper.decodeFromString<T>(string)
            } catch (ingored: Exception) {
                // TODO: Exception
                defaultValue
            }
        }

        override suspend fun writeTo(t: T, output: OutputStream) {
            try {
                val string = JsonHelper.encodeToString(t)
                output.write(string.encodeToByteArray())
            } catch (ignored: Exception) {
                // TODO: Exception
            }
        }
    }
    return MultiProcessDataStoreFactory.create(serializer) {
        File("${filesDir.absolutePath}/ds/$fileName")
    }
}