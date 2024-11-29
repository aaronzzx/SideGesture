package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Vibrations

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/29
 */

fun requirePosition(position: Int) {
    whenPosition(
        onLeft = { },
        onRight = { },
        position = position
    )
}

inline fun <T> whenPosition(
    onLeft: () -> T,
    onRight: () -> T,
    position: Int
): T = when (position) {
    GestureButton.LEFT -> onLeft()
    GestureButton.RIGHT -> onRight()
    else -> error("Unknown position: $position")
}

inline fun <T> whenVibrationEffect(
    onNone: () -> T,
    onTick: () -> T,
    onClick: () -> T,
    onHeavyClick: () -> T,
    effect: Int
): T = when (effect) {
    Vibrations.EFFECT_NONE -> onNone()
    Vibrations.EFFECT_TICK -> onTick()
    Vibrations.EFFECT_CLICK -> onClick()
    Vibrations.EFFECT_HEAVY_CLICK -> onHeavyClick()
    else -> error("Unknown effect: $effect")
}