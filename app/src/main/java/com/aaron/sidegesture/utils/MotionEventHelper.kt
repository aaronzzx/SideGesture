package com.aaron.sidegesture.utils

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/18
 */
object MotionEventHelper {

    private val _motionEvent: MutableStateFlow<MotionEvent?> = MutableStateFlow(null)
    val motionEvent: StateFlow<MotionEvent?> = _motionEvent.asStateFlow()

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
    LaunchedEffect(key1 = MotionEventHelper) {
        var offset = Offset.Unspecified
        MotionEventHelper
            .motionEvent
            .filterNotNull()
            .collect { event ->
                val rawOffset = Offset(event.rawX, event.rawY)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        curOnDragStart(rawOffset)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        offset += rawOffset
                        curOnDrag(offset)
                    }
                    MotionEvent.ACTION_UP -> {
                        curOnDragEnd()
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        curOnDragCancel()
                    }
                    else -> Unit
                }
            }
    }
}