package com.aaron.sidegesture.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState.Visible
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.toDp
import com.aaron.compose.ktx.toPx
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings.DimAlpha
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionPanelStyle
import com.aaron.sidegesture.entity.ArcStyle
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.Vibrations
import com.aaron.sidegesture.ktx.actionIcon
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.alipayColor
import com.aaron.sidegesture.ktx.appInfo
import com.aaron.sidegesture.ktx.isMiniWindow
import com.aaron.sidegesture.ktx.shortcutInfo
import com.aaron.sidegesture.ktx.toIntOffset
import com.aaron.sidegesture.ktx.tryVibrateForActionPanel
import com.aaron.sidegesture.ktx.wechatColor
import com.aaron.sidegesture.ui.theme.RootPadding
import com.blankj.utilcode.util.BarUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        visible = actionPanelState.visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = Color.Black.copy(DimAlpha))
            )

            val selectedAction = actionPanelState.selectedAction
            val selectedLabel = actionText(selectedAction)
            val animationSpec = spring<Float>(stiffness = Spring.StiffnessHigh)
            val enter = fadeIn(animationSpec) + scaleIn(animationSpec, 0.9f)
            val exit = fadeOut(animationSpec) + scaleOut(animationSpec, 0.9f)


            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = BarUtils.getStatusBarHeight().toDp())
                    .padding(RootPadding),
                visible = selectedAction.value == GlobalActions.EXTRA_LAUNCH_APP,
                enter = enter,
                exit = ExitTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .animateContentSize(
                            animationSpec = spring(),
                            alignment = Alignment.Center
                        )
                        .let { thisModifier ->
                            when (actionPanelState.triggerType.isMiniWindow()) {
                                true -> {
                                    thisModifier
                                        .width(200.dp)
                                        .aspectRatio(3 / 4f)
                                }
                                else -> {
                                    thisModifier.fillMaxSize()
                                }
                            }
                        }
                        .background(
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }

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

            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = BarUtils.getStatusBarHeight().toDp())
                    .padding(RootPadding),
                visible = selectedLabel.isNotEmpty(),
                enter = enter,
                exit = exit
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(
                            color = Color.Black, offset = Offset(2.0f, 2.0f), blurRadius = 3f
                        )
                    ),
                    color = Color.White
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
    // 斜边，从origin原点到item中心的距离，值越大item散得越开
    val hypot = itemSize.toPx() * 2f
    var parentSize by remember { mutableStateOf(Size.Zero) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    parentSize = it.size.toSize()
                }
                .matchParentSize()
        )

        val selectedLabel: String = actionText(actionPanelState.selectedAction)
        val animationSpec = spring<Float>(stiffness = Spring.StiffnessHigh)
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = BarUtils.getStatusBarHeight().toDp())
                .padding(RootPadding),
            visible = selectedLabel.isNotEmpty(),
            enter = fadeIn(animationSpec) + scaleIn(animationSpec, 0.9f),
            exit = fadeOut(animationSpec) + scaleOut(animationSpec, 0.9f)
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(
                        color = Color.Black, offset = Offset(2.0f, 2.0f), blurRadius = 3f
                    )
                ),
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .run {
                    val origin = remember(actionPanelState) { actionPanelState.origin }
                    graphicsLayer {
                        if (parentSize.isEmpty()) return@graphicsLayer
                        val itemSizeHalf = itemSize.toPx() / 2f
                        // 限制展开位置，防止显示不全
                        val iconOffset = itemSize.toPx() * 3f
                        val ox = origin.x.let {
                            when (actionPanelState.position) {
                                Position.Left -> it.coerceAtMost(parentSize.width / 2f)
                                Position.Right -> it.coerceAtLeast(parentSize.width / 2f)
                                Position.Bottom -> it.coerceIn(
                                    iconOffset,
                                    parentSize.width - iconOffset
                                )
                            }
                        }
                        val oy = origin.y.let {
                            when (actionPanelState.position) {
                                Position.Left, Position.Right -> it.coerceIn(
                                    minimumValue = iconOffset,
                                    maximumValue = parentSize.height - iconOffset
                                )

                                Position.Bottom -> it.coerceAtLeast(parentSize.height / 2f)
                            }
                        }
                        translationX = ox - itemSizeHalf
                        translationY = oy - itemSizeHalf
                    }
                }
                .size(itemSize)
        ) {
            val transition = transition
            actionPanelState.actions.fastForEachIndexed { index, action ->
                key(index) {
                    val targetAnimOffset = remember {
                        // 平均每个块之间的角度
                        val avgAngleDegree = 35.0
                        val totalAngleDegree = avgAngleDegree * (actionPanelState.actions.size - 1)
                        val angleDegree = -90.0 - totalAngleDegree / 2.0 + avgAngleDegree * index
                        val radians = Math.toRadians(angleDegree)
                        val neighbor = hypot * cos(radians)
                        val opposite = sqrt(hypot.pow(2) - neighbor.pow(2))
                        // 需要移动的x距离
                        val transX = when (actionPanelState.position) {
                            Position.Left -> opposite
                            Position.Right -> -opposite
                            Position.Bottom -> neighbor
                        }
                        // 需要移动的y距离
                        val transY = when (actionPanelState.position) {
                            Position.Left, Position.Right -> neighbor
                            Position.Bottom -> -opposite
                        }
                        Offset(x = transX.toFloat(), y = transY.toFloat())
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
                                val transFinger = finger - targetAnimOffset
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

                    val actionIcon = actionIcon(action = action)
                    val isWechatAlipay = remember(actionIcon) {
                        actionIcon == R.drawable.wechat_scan ||
                                actionIcon == R.drawable.wechat_paycode ||
                                actionIcon == R.drawable.alipay_scan ||
                                actionIcon == R.drawable.alipay_paycode
                    }
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned {
                                originBounds = it.boundsInRoot()
                            }
                            .graphicsLayer {
                                translationX = targetAnimOffset.x
                                translationY = targetAnimOffset.y
                                scaleX = selectAnim.value
                                scaleY = selectAnim.value
                            }
                            .run animateEnterExit@{
                                val stiffness = Spring.StiffnessMedium
                                animateEnterExit(
                                    enter = scaleIn(spring(stiffness = stiffness)) +
                                            slideIn(animationSpec = spring(stiffness = stiffness)) {
                                                -targetAnimOffset.toIntOffset()
                                            },
                                    exit = scaleOut(spring(stiffness = stiffness)) +
                                            slideOut(animationSpec = spring(stiffness = stiffness)) {
                                                -targetAnimOffset.toIntOffset()
                                            }
                                )
                            }
                            .matchParentSize()
                            .clipToBackground(
                                color = when (action.value) {
                                    GlobalActions.WECHAT_SCAN,
                                    GlobalActions.WECHAT_PAY -> MaterialTheme.colorScheme.wechatColor

                                    GlobalActions.ALIPAY_SCAN,
                                    GlobalActions.ALIPAY_PAY -> MaterialTheme.colorScheme.alipayColor

                                    GlobalActions.EXTRA_LAUNCH_APP -> when (actionIcon is ImageVector) {
                                        true -> MaterialTheme.colorScheme.primary
                                        else -> Color(action.appInfo!!.iconBgColor)
                                    }

                                    GlobalActions.EXTRA_LAUNCH_SHORTCUT -> when (actionIcon is ImageVector) {
                                        true -> MaterialTheme.colorScheme.primary
                                        else -> Color(action.shortcutInfo!!.iconBgColor)
                                    }

                                    else -> MaterialTheme.colorScheme.primary
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (actionIcon is ImageVector) {
                            Image(
                                imageVector = actionIcon,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                            )
                        } else {
                            AsyncImage(
                                modifier = Modifier
                                    .graphicsLayer {
                                        if (isWechatAlipay) {
                                            scaleX = 0.5f
                                            scaleY = 0.5f
                                        } else {
                                            val appInfo = action.appInfo
                                            if (appInfo != null) {
                                                scaleX = appInfo.iconScale
                                                scaleY = appInfo.iconScale
                                                return@graphicsLayer
                                            }
                                            val shortcutInfo = action.shortcutInfo
                                            if (shortcutInfo != null) {
                                                scaleX = shortcutInfo.iconScale
                                                scaleY = shortcutInfo.iconScale
                                            }
                                        }
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

@Composable
fun rememberActionPanelState(): ActionPanelState {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        ActionPanelState(coroutineScope)
    }
}

class ActionPanelState(private val coroutineScope: CoroutineScope) : LongSlideState() {

    var visible: Boolean by mutableStateOf(false)
        private set
    var actions: List<Action> by mutableStateOf(emptyList())
        private set
    var position: Position by mutableStateOf(Position.Left)
        private set
    private val pendingActions: MutableMap<Int, Action> = mutableStateMapOf()

    val selectedAction: Action by derivedStateOf {
        pendingActions.values.find { it != Action.NONE } ?: Action.NONE
    }
    var triggerType: TriggerType by mutableStateOf(TriggerType.Press)
        private set
    private var delayTriggerTypeChangedJob: Job? = null

    override fun onDragStart(offset: Offset) {
        super.onDragStart(offset)
        visible = true
    }

    fun ready(position: Position, actions: List<Action>) {
        this.position = position
        this.actions = actions
    }

    fun done(): Action {
        val action = selectedAction
        val triggerType = triggerType
        reset()
        return action.copy(extra = triggerType)
    }

    fun isSelected(action: Action): Boolean {
        return pendingActions.values.find { it == action } != null
    }

    fun select(index: Int, action: Action) {
        pendingActions[index] = action

        delayTriggerTypeChangedJob?.cancel()
        triggerType = TriggerType.Press
        delayTriggerTypeChangedJob = coroutineScope.launch {
            delay(500)
            triggerType = TriggerType.LongPress
        }
    }

    override fun reset() {
        visible = false
        pendingActions.clear()
        origin = Offset.Unspecified
        finger = Offset.Unspecified
        delayTriggerTypeChangedJob?.cancel()
        triggerType = TriggerType.Press
    }

    /**
     * 用于实现短按和长按
     */
    enum class TriggerType {

        Press, LongPress
    }
}