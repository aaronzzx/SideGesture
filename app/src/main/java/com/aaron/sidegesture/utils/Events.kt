package com.aaron.sidegesture.utils

import android.os.Looper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */
object Events {

    private val map: MutableMap<KClass<out Any>, MutableList<(Any) -> Unit>> = mutableMapOf()

    fun post(event: Any, postOnUiThread: Boolean = true) {
        if (postOnUiThread && Looper.myLooper() != Looper.getMainLooper()) {
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.Main) {
                dispatch(event)
            }
        } else {
            dispatch(event)
        }
    }

    fun <T : Any> subscribe(eventClass: KClass<T>, subscriber: (T) -> Unit) {
        var list = map[eventClass]
        if (list == null) {
            list = mutableListOf<(Any) -> Unit>().apply {
                map[eventClass] = this
            }
        }
        list.add(subscriber as (Any) -> Unit)
    }

    fun <T : Any> unsubscribe(eventClass: KClass<T>, subscriber: (T) -> Unit) {
        val map = map
        val list = map[eventClass]
        if (!list.isNullOrEmpty()) {
            list.remove(subscriber)
            if (list.isEmpty()) {
                map.remove(eventClass)
            }
        }
    }

    private fun dispatch(event: Any) {
        map[event::class]?.forEach {
            it(event)
        }
    }
}