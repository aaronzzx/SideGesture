package com.aaron.sidegesture.ktx

import android.os.Bundle
import androidx.navigation.NavType
import com.aaron.sidegesture.utils.JsonHelper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/4
 */

inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = JsonHelper.globalJson,
) = object : CustomNavType<T>(
    type = T::class,
    isNullableAllowed = isNullableAllowed,
) {
    override fun get(bundle: Bundle, key: String): T? {
        return bundle.getString(key)?.let<String, T>(json::decodeFromString)
    }

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, json.encodeToString(value))
    }

    override fun parseValue(value: String): T {
        return json.decodeFromString(value)
    }

    override fun serializeAsValue(value: T): String {
        return json.encodeToString(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as CustomNavType<*>
        return type == that.type
    }
}

abstract class CustomNavType<T : Any>(
    val type: KClass<T>,
    isNullableAllowed: Boolean = false,
) : NavType<T>(isNullableAllowed)