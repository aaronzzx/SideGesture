package com.aaron.sidegesture.feature.servicesettings

import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.RestorePayload
import com.aaron.sidegesture.utils.DataStoreHolder
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.security.MessageDigest

object RestoreDigest {

    suspend fun readPayload(): RestorePayload = coroutineScope {
        val initialSettings = async { DataStoreHolder.initialSettings.data.first() }
        val advancedSettings = async { DataStoreHolder.advancedSettings.data.first() }
        val gestureSettings = async { DataStoreHolder.gestureSettings.data.first() }
        val actionSettings = async { DataStoreHolder.actionSettings.data.first() }
        val sideGestureButtons = async { DataStoreHolder.sideGestureButtons.data.first() }
        val bottomGestureButtons = async { DataStoreHolder.bottomGestureButtons.data.first() }
        val topGestureButtons = async { DataStoreHolder.topGestureButtons.data.first() }
        RestorePayload(
            initialSettings = initialSettings.await(),
            advancedSettings = advancedSettings.await(),
            gestureSettings = gestureSettings.await(),
            actionSettings = actionSettings.await(),
            sideGestureButtons = sideGestureButtons.await(),
            bottomGestureButtons = bottomGestureButtons.await(),
            topGestureButtons = topGestureButtons.await()
        )
    }

    fun fromSnapshot(snapshot: ServiceSettingsSnapshot): RestorePayload {
        return RestorePayload(
            initialSettings = snapshot.initialSettings,
            advancedSettings = snapshot.advancedSettings,
            gestureSettings = snapshot.gestureSettings,
            actionSettings = snapshot.actionSettings,
            sideGestureButtons = snapshot.buttons.filter {
                it.position == Position.Left || it.position == Position.Right
            },
            bottomGestureButtons = snapshot.buttons.filter { it.position == Position.Bottom },
            topGestureButtons = snapshot.buttons.filter { it.position == Position.Top }
        )
    }

    fun payloadDigest(payload: RestorePayload): String {
        val element = JsonHelper.globalJson.encodeToJsonElement(payload)
        val canonical = canonicalize(element).toString()
        return sha256(canonical.encodeToByteArray())
    }

    fun imageDigest(directory: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        if (!directory.exists()) return digest.digest().toHex()
        val rootPath = directory.toPath()
        directory.walkTopDown()
            .filter(File::isFile)
            .sortedBy { rootPath.relativize(it.toPath()).toString() }
            .forEach { file ->
                val relativePath = rootPath.relativize(file.toPath()).toString()
                digest.update(relativePath.encodeToByteArray())
                digest.update(0)
                file.inputStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                digest.update(0)
            }
        return digest.digest().toHex()
    }

    fun combinedDigest(payloadDigest: String, imageDigest: String): String {
        return sha256("$payloadDigest\n$imageDigest".encodeToByteArray())
    }

    fun digest(payload: RestorePayload, imageDirectory: File): String {
        return combinedDigest(payloadDigest(payload), imageDigest(imageDirectory))
    }

    private fun canonicalize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> JsonObject(
                element.entries
                    .sortedBy { it.key }
                    .associate { (key, value) -> key to canonicalize(value) }
            )
            is JsonArray -> JsonArray(element.map(::canonicalize))
            else -> element
        }
    }

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }
}
