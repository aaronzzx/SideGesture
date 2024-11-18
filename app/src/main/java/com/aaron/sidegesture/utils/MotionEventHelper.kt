package com.aaron.sidegesture.utils

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */
object MotionEventHelper {

    private val _motionEvent: MutableLiveData<MotionEvent?> = MutableLiveData(null)
    val motionEvent: LiveData<MotionEvent?> = _motionEvent

    fun dispatchMotionEvent(event: MotionEvent) {
        _motionEvent.value = event
    }
}

@Composable
fun GestureHandler(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (dragAmount: Offset) -> Unit
) {
    val curOnDragStart by rememberUpdatedState(newValue = onDragStart)
    val curOnDragEnd by rememberUpdatedState(newValue = onDragEnd)
    val curOnDragCancel by rememberUpdatedState(newValue = onDragCancel)
    val curOnDrag by rememberUpdatedState(newValue = onDrag)

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, MotionEventHelper) {
        var dragging = false
        var x = -1f
        var y = -1f
        MotionEventHelper
            .motionEvent
            .observe(lifecycleOwner) { event ->
                event ?: return@observe
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
                        if (offsetX != 0f) {
                            curOnDrag(Offset(offsetX, offsetY))
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        curOnDragEnd()
                        dragging = false
                        x = -1f
                        y = -1f
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        curOnDragCancel()
                        dragging = false
                        x = -1f
                        y = -1f
                    }
                    else -> Unit
                }
            }
    }
}