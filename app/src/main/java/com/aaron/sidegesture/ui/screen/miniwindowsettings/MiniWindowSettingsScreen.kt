package com.aaron.sidegesture.ui.screen.miniwindowsettings

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.onClick
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.MinMiniWindowSizeDp
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindow.Bounds
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindowMode
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.EdgeMenuPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ScreenUtils
import kotlin.math.roundToInt

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/6/16
 */

// 缩放补偿系数范围：1.0=不补偿，越小窗口越大
private val MiniWindowScaleRange = 0.3f..1f

@Composable
fun MiniWindowSettingsScreen(
    onBack: () -> Unit,
    vm: MiniWindowSettingsVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        val activity = LocalContext.current as? Activity
        val configuration = LocalConfiguration.current
        // editingPortrait = 设备真实朝向，与 computeBounds 的朝向判定完全一致
        val isPortrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val screenWidthDp = ConvertUtils.px2dp(ScreenUtils.getScreenWidth().toFloat())
        val screenHeightDp = ConvertUtils.px2dp(ScreenUtils.getScreenHeight().toFloat())

        // 把真实朝向同步给 VM，决定读/写哪套 bounds 与 scale
        LaunchedEffect(isPortrait) { vm.onEditingPortraitChange(isPortrait) }
        // 进页面记下原朝向，退出时还原；页内点「横屏」只是临时锁向，离页不影响其他页面
        DisposableEffect(activity) {
            val original = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDispose { activity?.requestedOrientation = original }
        }

        if (uiState.showResetDialog) {
            MyAlertDialog(
                onDismissRequest = { vm.showResetDialog(false) },
                title = stringResource(id = R.string.reset_default_settings_warning),
                text = stringResource(id = R.string.mini_window_reset_warning_desc),
                onConfirmClick = { vm.resetAll() }
            )
        }

        if (uiState.showVivoShareHintDialog) {
            VivoShareHintDialog(
                countdownSec = uiState.vivoShareHintCountdownSec,
                onConfirm = { vm.dismissVivoShareHintDialog() }
            )
        }

        val bounds = uiState.currentBounds

        // 最外层全屏根 Box：主内容 + 1:1 屏幕预览框 overlay
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    onBack = onBack,
                    title = stringResource(id = R.string.mini_window_settings),
                    actions = {
                        IconButton(onClick = { vm.showResetDialog(true) }) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = stringResource(id = R.string.reset_settings)
                            )
                        }
                    }
                )
                // 整页可滚动，短屏(横屏/分屏/小窗/大字体)下不裁切
                MyColumn(verticalArrangement = Arrangement.spacedBy(ItemPadding)) {
                    MySection {
                        ModeRow(
                            mode = uiState.mode,
                            expanded = uiState.showModeDropdownMenu,
                            onExpandedChange = { vm.showModeDropdownMenu(it) },
                            onModeChange = { vm.onModeChange(it) }
                        )
                        MyTextSwitch(
                            onCheckedChange = { vm.onUseMiWindowChange(it) },
                            checked = uiState.useMiWindow,
                            text = stringResource(id = R.string.use_mi_window),
                            secondaryText = stringResource(id = R.string.use_mi_window_hint),
                            secondaryTextMaxLines = 3
                        )
                    }
                    MySection {
                        MyTextSlider(
                            value = uiState.currentScale,
                            onValueChange = { vm.onCurrentScaleChange(it) },
                            onValueChangeFinished = { vm.onScaleChangeFinished() },
                            text = stringResource(
                                id = R.string.mini_window_scale_compensation,
                                "%.2f".format(uiState.currentScale)
                            ),
                            sliderValueHint = stringResource(id = R.string.large) to stringResource(id = R.string.small),
                            valueRange = MiniWindowScaleRange
                        )
                        Text(
                            modifier = Modifier
                                .padding(horizontal = ContentPaddingHorizontal)
                                .padding(bottom = ContentPaddingVerticalWithSection),
                            text = stringResource(id = R.string.mini_window_scale_hint),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OrientationChips(
                        editingPortrait = isPortrait,
                        onChange = { portrait ->
                            activity?.requestedOrientation = if (portrait) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                    )
                    Text(
                        text = stringResource(id = R.string.mini_window_rect_hint),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    MySection {
                        MyTextSlider(
                            value = bounds.widthDp.toFloat(),
                            onValueChange = {
                                vm.onBoundsChange(
                                    bounds.copy(widthDp = it.roundToInt())
                                        .clampTo(screenWidthDp, screenHeightDp)
                                )
                            },
                            onValueChangeFinished = { vm.onBoundsChangeFinished() },
                            text = stringResource(id = R.string.mini_window_width) + "  ${bounds.widthDp}",
                            valueRange = MinMiniWindowSizeDp.toFloat()..screenWidthDp.toFloat()
                        )
                        MyTextSlider(
                            value = bounds.heightDp.toFloat(),
                            onValueChange = {
                                vm.onBoundsChange(
                                    bounds.copy(heightDp = it.roundToInt())
                                        .clampTo(screenWidthDp, screenHeightDp)
                                )
                            },
                            onValueChangeFinished = { vm.onBoundsChangeFinished() },
                            text = stringResource(id = R.string.mini_window_height) + "  ${bounds.heightDp}",
                            valueRange = MinMiniWindowSizeDp.toFloat()..screenHeightDp.toFloat()
                        )
                        MyTextSlider(
                            value = bounds.leftDp.toFloat(),
                            onValueChange = {
                                vm.onBoundsChange(
                                    bounds.copy(leftDp = it.roundToInt())
                                        .clampTo(screenWidthDp, screenHeightDp)
                                )
                            },
                            onValueChangeFinished = { vm.onBoundsChangeFinished() },
                            text = stringResource(id = R.string.mini_window_horizontal_position) + "  ${bounds.leftDp}",
                            valueRange = 0f..(screenWidthDp - bounds.widthDp).coerceAtLeast(0).toFloat()
                        )
                        MyTextSlider(
                            value = bounds.topDp.toFloat(),
                            onValueChange = {
                                vm.onBoundsChange(
                                    bounds.copy(topDp = it.roundToInt())
                                        .clampTo(screenWidthDp, screenHeightDp)
                                )
                            },
                            onValueChangeFinished = { vm.onBoundsChangeFinished() },
                            text = stringResource(id = R.string.mini_window_vertical_position) + "  ${bounds.topDp}",
                            valueRange = 0f..(screenHeightDp - bounds.heightDp).coerceAtLeast(0).toFloat()
                        )
                    }
                }
            }
            // 坐标系=屏幕物理左上(edge-to-edge，不加 inset padding)，与 freeform launchBounds 物理坐标一致
            // 半透明、不拦截触摸，纯展示小窗真实位置与大小；拖动条调值即实时移动/缩放
            Box(
                modifier = Modifier
                    .offset(x = bounds.leftDp.dp, y = bounds.topDp.dp)
                    .size(width = bounds.widthDp.dp, height = bounds.heightDp.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(2.dp, MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * vivo 设备首次进入小窗设置的提示弹窗：引导去系统开启小窗分享开关。
 * 倒计时结束([countdownSec]<=0)前禁用确认键并屏蔽返回/外部点击，不可关闭。
 */
@Composable
private fun VivoShareHintDialog(
    countdownSec: Int,
    onConfirm: () -> Unit
) {
    val dismissable = countdownSec <= 0
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { if (dismissable) onConfirm() },
        properties = DialogProperties(
            dismissOnBackPress = dismissable,
            dismissOnClickOutside = dismissable
        ),
        title = { Text(text = stringResource(id = R.string.mini_window_vivo_share_hint_title)) },
        text = { Text(text = stringResource(id = R.string.mini_window_vivo_share_hint_desc)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = dismissable) {
                Text(
                    text = if (dismissable) {
                        stringResource(id = R.string.mini_window_vivo_share_hint_confirm)
                    } else {
                        stringResource(
                            id = R.string.mini_window_vivo_share_hint_confirm_countdown,
                            countdownSec
                        )
                    }
                )
            }
        }
    )
}

/**
 * 把候选 [Bounds] 收进屏幕范围：尺寸夹到 [MinMiniWindowSizeDp]~屏幕尺寸，
 * 改 width/height 导致 left/top 越界时自动收回，保证窗口完整落在屏幕内。
 */
private fun Bounds.clampTo(screenWidthDp: Int, screenHeightDp: Int): Bounds {
    val w = widthDp.coerceIn(MinMiniWindowSizeDp, screenWidthDp.coerceAtLeast(MinMiniWindowSizeDp))
    val h = heightDp.coerceIn(MinMiniWindowSizeDp, screenHeightDp.coerceAtLeast(MinMiniWindowSizeDp))
    val l = leftDp.coerceIn(0, (screenWidthDp - w).coerceAtLeast(0))
    val t = topDp.coerceIn(0, (screenHeightDp - h).coerceAtLeast(0))
    return Bounds(widthDp = w, heightDp = h, leftDp = l, topDp = t)
}

@Composable
private fun ModeRow(
    mode: MiniWindowMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModeChange: (MiniWindowMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinItemHeightNoSecondary)
            .onSingleClick { onExpandedChange(true) }
            .padding(
                horizontal = ContentPaddingHorizontal,
                vertical = ContentPaddingVerticalWithSection
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ItemPadding)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.mini_window_mode),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = miniWindowModeText(mode),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
            DropdownMenu(
                containerColor = MaterialTheme.colorScheme.surface,
                offset = DpOffset(x = -EdgeMenuPadding, y = 0.dp),
                shape = MaterialTheme.shapes.medium,
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                MiniWindowMode.entries.fastForEach { item ->
                    key(item) {
                        DropdownMenuItem(
                            onClick = {
                                onModeChange(item)
                                onExpandedChange(false)
                            },
                            text = {
                                Text(text = miniWindowModeText(item))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrientationChips(
    editingPortrait: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ItemPadding)
    ) {
        OrientationChip(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.mini_window_portrait),
            selected = editingPortrait,
            onClick = { onChange(true) }
        )
        OrientationChip(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.mini_window_landscape),
            selected = !editingPortrait,
            onClick = { onChange(false) }
        )
    }
}

@Composable
private fun OrientationChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clipToBackground(color = backgroundColor, shape = MaterialTheme.shapes.medium)
            .onClick { onClick() }
            .heightIn(min = MinItemHeightNoSecondary)
            .padding(vertical = ContentPaddingVerticalWithSection),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun miniWindowModeText(mode: MiniWindowMode): String {
    return when (mode) {
        MiniWindowMode.Auto -> stringResource(id = R.string.mini_window_mode_auto)
        MiniWindowMode.Default -> stringResource(id = R.string.mini_window_mode_default)
        MiniWindowMode.Oppo -> stringResource(id = R.string.mini_window_mode_oppo)
        MiniWindowMode.Huawei -> stringResource(id = R.string.mini_window_mode_huawei)
        MiniWindowMode.Vivo -> stringResource(id = R.string.mini_window_mode_vivo)
        MiniWindowMode.Meizu -> stringResource(id = R.string.mini_window_mode_meizu)
    }
}
