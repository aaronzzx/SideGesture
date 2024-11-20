package com.aaron.sidegesture.utils

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */
object MotionEventDispatcher {

    private val listeners = mutableListOf<OnMotionEventListener>()

    fun addOnMotionEventListener(listener: OnMotionEventListener) {
        listeners.add(listener)
    }

    fun removeOnMotionEventListener(listener: OnMotionEventListener) {
        listeners.remove(listener)
    }

    fun dispatch(event: MotionEvent) {
        val listeners = listeners
        for (index in listeners.indices) {
            val l = listeners[index]
            l.onDispatch(event)
        }
    }
}

fun interface OnMotionEventListener {

    fun onDispatch(event: MotionEvent)
}

@Composable
fun DragGestureHandler(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (dragAmount: Offset) -> Unit
) {
    val curOnDragStart by rememberUpdatedState(newValue = onDragStart)
    val curOnDragEnd by rememberUpdatedState(newValue = onDragEnd)
    val curOnDragCancel by rememberUpdatedState(newValue = onDragCancel)
    val curOnDrag by rememberUpdatedState(newValue = onDrag)

    DisposableEffect(key1 = Unit) {
        var x = -1f
        var y = -1f
        val l = OnMotionEventListener { event ->
            val rawX = event.rawX
            val rawY = event.rawY
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    x = rawX
                    y = rawY
                    curOnDragStart(Offset(x, y))
                }
                MotionEvent.ACTION_MOVE -> {
                    val offsetX = rawX - x
                    val offsetY = rawY - y
                    x = rawX
                    y = rawY
                    curOnDrag(Offset(offsetX, offsetY))
                }
                MotionEvent.ACTION_UP -> {
                    curOnDragEnd()
                    x = -1f
                    y = -1f
                }
                MotionEvent.ACTION_CANCEL -> {
                    curOnDragCancel()
                    x = -1f
                    y = -1f
                }
                else -> Unit
            }
        }
        MotionEventDispatcher.addOnMotionEventListener(l)
        onDispose {
            MotionEventDispatcher.removeOnMotionEventListener(l)
        }
    }
}