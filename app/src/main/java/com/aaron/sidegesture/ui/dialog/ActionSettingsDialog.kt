package com.aaron.sidegesture.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.LoadingComponent
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.MaxGotoBottomStrength
import com.aaron.sidegesture.constant.GlobalSettings.MaxMoveScreenHover
import com.aaron.sidegesture.constant.GlobalSettings.MaxMoveScreenRate
import com.aaron.sidegesture.constant.GlobalSettings.MinGotoBottomStrength
import com.aaron.sidegesture.constant.GlobalSettings.MinMoveScreenHover
import com.aaron.sidegesture.constant.GlobalSettings.MinMoveScreenRate
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.icon
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.theme.TopBarPaddingExtra
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.alpha
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.formatSliderDecimal
import com.aaron.sidegesture.ui.widget.formatSliderInteger

/**
 * @author DS-Z
 * @since 2025/6/30
 */

private val PreviousAppListHeight = 280.dp

@Composable
fun MoveScreenSettingsContent(vm: ActionSettingsVM = viewModel()) {
    UDFComponent(
        component = vm.udfComponent,
        onEvent = {}
    ) { uiState ->
        LoadingComponent(
            modifier = Modifier.fillMaxWidth(),
            component = vm.loadingComponent
        ) {
            val moveScreen = uiState.actionSettings.moveScreen
            // 系统低于 Android 11 无法截屏，仅支持准星样式
            val supportsMagnifier = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val displayStyle = if (supportsMagnifier) {
                moveScreen.style
            } else {
                ActionSettings.MoveScreen.Style.Crosshair
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                MoveScreenStyleSelector(
                    style = displayStyle,
                    enabled = supportsMagnifier,
                    onStyleChange = { vm.onMoveScreenStyleChange(it) }
                )
                MyTextSwitch(
                    checked = moveScreen.popupEnabled,
                    onCheckedChange = { vm.onMoveScreenPopupEnabledChange(it) },
                    text = stringResource(id = R.string.move_screen_popup_enabled),
                    secondaryText = stringResource(id = R.string.move_screen_popup_enabled_summary)
                )
                MyTextSlider(
                    value = moveScreen.rate,
                    onValueChange = { vm.onMoveScreenRateChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.move_screen_rate),
                    sliderValueHint = stringResource(id = R.string.slow) to stringResource(id = R.string.fast),
                    valueRange = MinMoveScreenRate..MaxMoveScreenRate,
                    valueFormatter = { formatSliderDecimal(it, 2, "×") }
                )
                MyTextSlider(
                    // 弹窗关闭时悬停延迟无意义，置灰
                    enabled = moveScreen.popupEnabled,
                    value = moveScreen.hoverDelayMs.toFloat(),
                    onValueChange = { vm.onMoveScreenHoverChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.hover_trigger_delay),
                    sliderValueHint = stringResource(id = R.string.short1) to stringResource(id = R.string.long1),
                    valueRange = MinMoveScreenHover..MaxMoveScreenHover,
                    valueFormatter = { formatSliderInteger(it, " ms") }
                )
            }
        }
    }
}

@Composable
private fun MoveScreenStyleSelector(
    style: ActionSettings.MoveScreen.Style,
    enabled: Boolean,
    onStyleChange: (ActionSettings.MoveScreen.Style) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(IconTextPadding)
    ) {
        Text(
            text = stringResource(id = R.string.move_screen_style),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ItemPadding)
        ) {
            MoveScreenStyleChip(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.move_screen_style_magnifier),
                selected = style == ActionSettings.MoveScreen.Style.Magnifier,
                enabled = enabled,
                onClick = { onStyleChange(ActionSettings.MoveScreen.Style.Magnifier) }
            )
            MoveScreenStyleChip(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.move_screen_style_crosshair),
                selected = style == ActionSettings.MoveScreen.Style.Crosshair,
                enabled = enabled,
                onClick = { onStyleChange(ActionSettings.MoveScreen.Style.Crosshair) }
            )
        }
        if (!enabled) {
            Text(
                text = stringResource(id = R.string.move_screen_style_crosshair_only_hint),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun MoveScreenStyleChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
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
            .alpha(if (enabled) 1f else 0.5f)
            .background(color = backgroundColor, shape = MaterialTheme.shapes.medium)
            .then(if (enabled) Modifier.onClick { onClick() } else Modifier)
            .heightIn(min = MinInteractiveSize)
            .padding(vertical = ContentPaddingVertical),
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
fun PreviousAppSettingsContent(vm: ActionSettingsVM = viewModel()) {
    UDFComponent(
        component = vm.udfComponent,
        onEvent = {}
    ) { uiState ->
        LaunchedEffect(uiState.actionSettingsLoaded) {
            if (uiState.actionSettingsLoaded) {
                vm.updatePreviousAppSearchQuery("")
                vm.updatePreviousAppInfos()
            }
        }
        LoadingComponent(
            modifier = Modifier.fillMaxWidth(),
            component = vm.loadingComponent
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.previousAppSearchQuery,
                    onValueChange = vm::updatePreviousAppSearchQuery,
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.search),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_apps_or_package_name),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PreviousAppListHeight)
                ) {
                    items(
                        items = uiState.previousAppVisibleAppInfos,
                        key = { it.qualifiedName }
                    ) { item ->
                        PreviousAppItem(
                            appInfo = item,
                            selected = item.packageName in uiState.actionSettings.previousApp.packageNames,
                            onSelect = { selected ->
                                vm.selectPreviousApp(item, selected)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GotoBottomSettingsContent(vm: ActionSettingsVM = viewModel()) {
    UDFComponent(
        component = vm.udfComponent,
        onEvent = {}
    ) { uiState ->
        LoadingComponent(
            modifier = Modifier.fillMaxWidth(),
            component = vm.loadingComponent
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                MyTextSlider(
                    value = uiState.actionSettings.gotoBottom.strength.toFloat(),
                    onValueChange = { vm.onGotoBottomStrengthChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.strength),
                    sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                    valueRange = MinGotoBottomStrength..MaxGotoBottomStrength,
                    valueFormatter = { formatSliderInteger(it) }
                )
            }
        }
    }
}

@Composable
private fun PreviousAppItem(
    appInfo: AppInfo,
    selected: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onClick {
                onSelect(!selected)
            }
            .padding(vertical = ContentPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        AsyncImage(
            modifier = Modifier.size(MinInteractiveSize),
            model = appInfo.icon,
            contentDescription = null,
            imageLoader = context.imageLoader,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(start = IconTextPadding)
                .weight(1f)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = appInfo.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = appInfo.packageName,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(
            modifier = Modifier.padding(end = TopBarPaddingExtra),
            checked = selected,
            onCheckedChange = onSelect
        )
    }
}
