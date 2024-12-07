package com.aaron.sidegesture.ktx

import android.os.SystemClock
import kotlinx.coroutines.delay

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/7
 */

suspend inline fun <T> coerceTimeMillis(timeMillis: Long, block: () -> T): T {
    val start = SystemClock.uptimeMillis()
    val returnValue = block()
    val cost = SystemClock.uptimeMillis() - start
    if (cost < timeMillis) {
        delay(timeMillis - cost)
    }
    return returnValue
}