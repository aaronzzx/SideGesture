package com.aaron.sidegesture.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.LoadingComponent
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.MaxGotoBottomStrength
import com.aaron.sidegesture.constant.GlobalSettings.MaxMiniWindowPositionRatio
import com.aaron.sidegesture.constant.GlobalSettings.MaxMiniWindowSizeRatio
import com.aaron.sidegesture.constant.GlobalSettings.MaxMoveScreenHover
import com.aaron.sidegesture.constant.GlobalSettings.MaxMoveScreenRate
import com.aaron.sidegesture.constant.GlobalSettings.MinGotoBottomStrength
import com.aaron.sidegesture.constant.GlobalSettings.MinMiniWindowPositionRatio
import com.aaron.sidegesture.constant.GlobalSettings.MinMiniWindowSizeRatio
import com.aaron.sidegesture.constant.GlobalSettings.MinMoveScreenHover
import com.aaron.sidegesture.constant.GlobalSettings.MinMoveScreenRate
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindowMode
import com.aaron.sidegesture.ktx.icon
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.theme.TopBarPaddingExtra
import com.aaron.sidegesture.ui.widget.MyTextSlider

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                MyTextSlider(
                    value = uiState.actionSettings.moveScreen.rate,
                    onValueChange = { vm.onMoveScreenRateChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.move_screen_rate),
                    sliderValueHint = stringResource(id = R.string.slow) to stringResource(id = R.string.fast),
                    valueRange = MinMoveScreenRate..MaxMoveScreenRate
                )
                MyTextSlider(
                    value = uiState.actionSettings.moveScreen.hoverDelayMs.toFloat(),
                    onValueChange = { vm.onMoveScreenHoverChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.hover_trigger_delay),
                    sliderValueHint = stringResource(id = R.string.short1) to stringResource(id = R.string.long1),
                    valueRange = MinMoveScreenHover..MaxMoveScreenHover
                )
            }
        }
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
                    valueRange = MinGotoBottomStrength..MaxGotoBottomStrength
                )
            }
        }
    }
}

@Composable
fun MiniWindowSettingsContent(vm: ActionSettingsVM = viewModel()) {
    UDFComponent(
        component = vm.udfComponent,
        onEvent = {}
    ) { uiState ->
        LoadingComponent(
            modifier = Modifier.fillMaxWidth(),
            component = vm.loadingComponent
        ) {
            val miniWindow = uiState.actionSettings.miniWindow
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinInteractiveSize)
                        .onSingleClick {
                            vm.showMiniWindowModeDropdownMenu(true)
                        }
                        .padding(
                            horizontal = ContentPaddingHorizontal,
                            vertical = ContentPaddingVerticalWithSection
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ItemPadding)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.mini_window_mode),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = miniWindowModeText(miniWindow.mode),
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
                            offset = DpOffset(x = 0.dp, y = 0.dp),
                            shape = MaterialTheme.shapes.medium,
                            expanded = uiState.showMiniWindowModeDropdownMenu,
                            onDismissRequest = { vm.showMiniWindowModeDropdownMenu(false) }
                        ) {
                            MiniWindowMode.entries.fastForEach { mode ->
                                key(mode) {
                                    DropdownMenuItem(
                                        onClick = {
                                            vm.onMiniWindowModeChange(mode)
                                            vm.showMiniWindowModeDropdownMenu(false)
                                        },
                                        text = {
                                            Text(text = miniWindowModeText(mode))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                MyTextSlider(
                    value = miniWindow.widthRatio,
                    onValueChange = { vm.onMiniWindowWidthRatioChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.mini_window_width),
                    sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                    valueRange = MinMiniWindowSizeRatio..MaxMiniWindowSizeRatio
                )
                MyTextSlider(
                    value = miniWindow.heightRatio,
                    onValueChange = { vm.onMiniWindowHeightRatioChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.mini_window_height),
                    sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                    valueRange = MinMiniWindowSizeRatio..MaxMiniWindowSizeRatio
                )
                MyTextSlider(
                    value = miniWindow.horizontalPositionRatio,
                    onValueChange = { vm.onMiniWindowHorizontalPositionRatioChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.mini_window_horizontal_position),
                    sliderValueHint = stringResource(id = R.string.mini_window_left) to stringResource(id = R.string.mini_window_right),
                    valueRange = MinMiniWindowPositionRatio..MaxMiniWindowPositionRatio
                )
                MyTextSlider(
                    value = miniWindow.verticalPositionRatio,
                    onValueChange = { vm.onMiniWindowVerticalPositionRatioChange(it) },
                    onValueChangeFinished = { vm.saveSettings() },
                    text = stringResource(id = R.string.mini_window_vertical_position),
                    sliderValueHint = stringResource(id = R.string.top) to stringResource(id = R.string.bottom),
                    valueRange = MinMiniWindowPositionRatio..MaxMiniWindowPositionRatio
                )
            }
        }
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
