package com.aaron.sidegesture.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.util.fastForEachIndexed
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toDp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.constant.Actions
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.GestureButton.Companion.LEFT
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.ktx.toIntOffset
import com.aaron.sidegesture.ktx.tryVibrateForActionPanel
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
    vibrations: Vibrations? = null
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = actionPanelState.isExpanded,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
    ) {
        when (actionPanelStyle) {
            is ArcStyle -> {
                ArcActionPanel(
                    modifier = Modifier.fillMaxSize(),
                    actionPanelStyle = actionPanelStyle,
                    actionPanelState = actionPanelState,
                    vibrations = vibrations
                )
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.ArcActionPanel(
    actionPanelStyle: ArcStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    vibrations: Vibrations? = null
) {
    val itemSize = actionPanelStyle.itemSize.toDp()
    val hypot = itemSize.toPx() * 2f
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color = Color.Black.copy(0.5f))
        )

        Box(
            Modifier
                .run graphicsLayer@{
                    val origin = remember(actionPanelState) { actionPanelState.origin }
                    graphicsLayer {
                        val offset = itemSize.toPx() / 2f
                        translationX = origin.x - offset
                        translationY = origin.y - offset
                    }
                }
                .size(itemSize)
        ) {
            val transition = transition
            actionPanelState.actions.fastForEachIndexed { index, action ->
                key(index) {
                    val animOffset = remember {
                        val avgAngDeg = 35.0
                        val totalAngDeg = avgAngDeg * (actionPanelState.actions.size - 1)
                        val angDeg = -90.0 - totalAngDeg / 2.0 + avgAngDeg * index
                        val radians = Math.toRadians(angDeg)
                        val dy = hypot * cos(radians)
                        val dx = sqrt(hypot.pow(2) - dy.pow(2)).let { value ->
                            if (actionPanelState.position == LEFT) value else -value
                        }
                        Offset(x = dx.toFloat(), y = dy.toFloat())
                    }
                    val selectAnim = remember { Animatable(1f) }

                    var originBounds by remember { mutableStateOf(Rect.Zero) }
                    LaunchedEffect(transition, actionPanelState, index, action, selectAnim) {
                        snapshotFlow { actionPanelState.finger }
                            .filter {
                                it.isSpecified &&
                                        !transition.isRunning &&
                                        transition.currentState == Visible
                            }
                            .collect { finger ->
                                val transFinger = finger - animOffset
                                if (originBounds.contains(transFinger)) {
                                    if (!actionPanelState.isSelected(action)) {
                                        launch { selectAnim.animateTo(1.15f) }
                                        actionPanelState.select(index, action)
                                        vibrations?.tryVibrateForActionPanel()
                                    }
                                } else {
                                    if (actionPanelState.isSelected(action)) {
                                        launch { selectAnim.animateTo(1f) }
                                        actionPanelState.select(index, null)
                                    }
                                }
                            }
                    }
                    Text(
                        modifier = Modifier
                            .onGloballyPositioned {
                                originBounds = it.boundsInRoot()
                            }
                            .graphicsLayer {
                                translationX = animOffset.x
                                translationY = animOffset.y
                                scaleX = selectAnim.value
                                scaleY = selectAnim.value
                            }
                            .run animateEnterExit@{
                                val stiffness = Spring.StiffnessMedium
                                animateEnterExit(
                                    enter = scaleIn(spring(stiffness = stiffness)) +
                                            slideIn(animationSpec = spring(stiffness = stiffness)) {
                                                -animOffset.toIntOffset()
                                            },
                                    exit = scaleOut(spring(stiffness = stiffness)) +
                                            slideOut(animationSpec = spring(stiffness = stiffness)) {
                                                -animOffset.toIntOffset()
                                            }
                                )
                            }
                            .matchParentSize()
                            .clipToBackground(
                                // TODO: hardcode
                                color = when (action) {
                                    Actions.WECHAT_SCAN -> Color(0xFF1FCA37)
                                    Actions.WECHAT_PAY -> Color(0xFF1FCA37)
                                    Actions.ALIPAY_SCAN -> Color(0xFF008EFF)
                                    Actions.ALIPAY_PAY -> Color(0xFF008EFF)
                                    else -> Color(0xFFFF7E55)
                                },
                                shape = CircleShape
                            )
                            .wrapContentSize(),
                        // TODO: hardcode
                        text = when (action) {
                            Actions.WECHAT_SCAN -> "WS"
                            Actions.WECHAT_PAY -> "WP"
                            Actions.ALIPAY_SCAN -> "AS"
                            Actions.ALIPAY_PAY -> "AP"
                            else -> "?"
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}