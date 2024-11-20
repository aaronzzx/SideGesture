package com.aaron.sidegesture.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.config.Actions
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.ktx.tryVibrateForActionPanel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/20
 */

@Composable
fun ActionPanel(
    actionPanelStyle: ActionPanelStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    position: Int = LEFT,
    vibrations: Vibrations = Vibrations()
) {
    when (actionPanelStyle) {
        is ActionPanelStyle.Arc -> {
            ArcActionPanel(
                modifier = modifier,
                actionPanelStyle = actionPanelStyle,
                actionPanelState = actionPanelState,
                position = position,
                vibrations = vibrations
            )
        }
    }
}

@Composable
private fun ArcActionPanel(
    actionPanelStyle: ActionPanelStyle.Arc,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    position: Int = LEFT,
    vibrations: Vibrations = Vibrations()
) {
    val origin by rememberUpdatedState(newValue = actionPanelState.origin)
    val finger by rememberUpdatedState(newValue = actionPanelState.finger)
    val actions by rememberUpdatedState(newValue = actionPanelState.actions)
    val itemSize = 48.dp
    val hypot = itemSize.toPx() * 2f
    Box(modifier = modifier) {
        val bgAlpha by animateFloatAsState(
            targetValue = 1f.takeIf { origin.isSpecified } ?: 0f,
            label = ""
        )
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = bgAlpha
                }
                .matchParentSize()
                .background(color = Color.Black.copy(0.5f))
        )

        var transOffset by remember { mutableStateOf(Offset.Zero) }
        LaunchedEffect(key1 = Unit) {
            snapshotFlow { origin }
                .filter { it.isSpecified }
                .collect {
                    transOffset = origin
                }
        }
        Box(
            Modifier
                .graphicsLayer {
                    val offset = itemSize.toPx() / 2
                    translationX = transOffset.x - offset
                    translationY = transOffset.y - offset
                }
                .size(itemSize)
        ) {
            actions.fastForEachIndexed { index, action ->
                key(index) {
                    val animX = remember { Animatable(0f) }
                    val animY = remember { Animatable(0f) }
                    val animScale = remember { Animatable(0f) }

                    LaunchedEffect(animX, animY, animScale) {
                        snapshotFlow { origin }
                            .filter { it.isUnspecified }
                            .collect {
                                coroutineScope {
                                    launch { animX.animateTo(0f) }
                                    launch { animY.animateTo(0f) }
                                    launch { animScale.animateTo(0f) }
                                }
                            }
                    }

                    LaunchedEffect(index, animX, animY, animScale) {
                        snapshotFlow { origin }
                            .filter { origin.isSpecified }
                            .collect {
                                val avgAngDeg = 35.0
                                val totalAngDeg = avgAngDeg * (actions.size - 1)
                                val angDeg = -90.0 - totalAngDeg / 2.0 + avgAngDeg * index
                                val radians = Math.toRadians(angDeg)
                                val dy = hypot * cos(radians)
                                val dx = sqrt(hypot.pow(2) - dy.pow(2)).let { value ->
                                    if (position == LEFT) value else -value
                                }
                                coroutineScope {
                                    launch { animX.animateTo(dx.toFloat()) }
                                    launch { animY.animateTo(dy.toFloat()) }
                                    launch { animScale.animateTo(1f) }
                                }
                            }
                    }

                    var originBounds by remember { mutableStateOf(Rect.Zero) }
                    LaunchedEffect(actionPanelState, index, action, animX, animY, animScale) {
                        snapshotFlow { finger }
                            .filter {
                                it.isSpecified && animScale.value >= 1f
                            }
                            .collect {
                                val offset = Offset(x = animX.value, y = animY.value)
                                val transFinger = it - offset
                                if (originBounds.contains(transFinger)) {
                                    if (!actionPanelState.isSelected(action)) {
                                        launch { animScale.animateTo(1.15f) }
                                        actionPanelState.select(index, action)
                                        vibrations.tryVibrateForActionPanel()
                                    }
                                } else {
                                    if (actionPanelState.isSelected(action)) {
                                        launch { animScale.animateTo(1f) }
                                        actionPanelState.select(index, null)
                                    }
                                }
                            }
                    }
                    Image(
                        modifier = Modifier
                            .onGloballyPositioned {
                                originBounds = it.boundsInRoot()
                            }
                            .graphicsLayer {
                                scaleX = animScale.value
                                scaleY = animScale.value
                                translationX = animX.value
                                translationY = animY.value
                            }
                            .matchParentSize()
                            .clipToBackground(
                                color = when (action) {
                                    Actions.WECHAT_SCAN -> Color.Green
                                    Actions.WECHAT_PAY -> Color.Red
                                    Actions.ALIPAY_SCAN -> Color.Magenta
                                    Actions.ALIPAY_PAY -> Color.Blue
                                    else -> Color.Yellow
                                },
                                shape = CircleShape
                            ),
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        contentScale = ContentScale.Inside
                    )
                }
            }
        }
    }
}