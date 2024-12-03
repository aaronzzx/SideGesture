package com.aaron.sidegesture.ui.widget

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toDp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.DimAlpha
import com.aaron.sidegesture.constant.Position
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.ktx.actionIcon
import com.aaron.sidegesture.ktx.toIntOffset
import com.aaron.sidegesture.ktx.tryVibrateForActionPanel
import com.aaron.sidegesture.ui.theme.AlipayColor
import com.aaron.sidegesture.ui.theme.WechatColor
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
fun rememberActionPanelState(): ActionPanelState {
    return remember {
        ActionPanelState()
    }
}

class ActionPanelState : QuickStartState()

@Composable
fun ActionPanel(
    actionPanelStyle: ActionPanelStyle,
    actionPanelState: ActionPanelState,
    modifier: Modifier = Modifier,
    vibrations: Vibrations? = null
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = actionPanelState.visible,
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
                .background(color = Color.Black.copy(DimAlpha))
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
                            when (actionPanelState.position) {
                                Position.Left -> value
                                Position.Right -> -value
                            }
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
                                        actionPanelState.select(index, Action.NONE)
                                    }
                                }
                            }
                    }

                    Box(
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
                                color = when (action.value) {
                                    GlobalActions.WECHAT_SCAN,
                                    GlobalActions.WECHAT_PAY -> WechatColor

                                    GlobalActions.ALIPAY_SCAN,
                                    GlobalActions.ALIPAY_PAY -> AlipayColor

                                    else -> MaterialTheme.colorScheme.primary
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val actionIcon = actionIcon(action = action)
                        if (actionIcon is ImageVector) {
                            Image(
                                imageVector = actionIcon,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Color.White)
                            )
                        } else {
                            val isWechatAlipay = remember(actionIcon) {
                                actionIcon == R.drawable.wechat_scan ||
                                        actionIcon == R.drawable.wechat_paycode ||
                                        actionIcon == R.drawable.alipay_scan ||
                                        actionIcon == R.drawable.alipay_paycode
                            }
                            AsyncImage(
                                modifier = Modifier.let {
                                    if (!isWechatAlipay) it else it.padding(12.dp)
                                },
                                model = actionIcon,
                                contentDescription = null,
                                imageLoader = LocalContext.current.imageLoader,
                                colorFilter = if (!isWechatAlipay) null else {
                                    ColorFilter.tint(Color.White)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}