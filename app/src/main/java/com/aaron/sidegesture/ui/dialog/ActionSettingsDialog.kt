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
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.formatSliderDecimal
import com.aaron.sidegesture.ui.widget.formatSliderInteger

/**
 * @author DS-Z
 * @since 2025/6/30
 */

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimensions.listItem.contentGap
                )
            ) {
                MyTextSwitch(
                    checked = moveScreen.popupEnabled,
                    onCheckedChange = { vm.onMoveScreenPopupEnabledChange(it) },
                    text = stringResource(id = R.string.move_screen_popup_enabled),
                    secondaryText = stringResource(id = R.string.move_screen_popup_enabled_summary)
                )
                MyTextSwitch(
                    checked = moveScreen.fastMoveAccelerationEnabled,
                    onCheckedChange = {
                        vm.onMoveScreenFastMoveAccelerationEnabledChange(it)
                    },
                    text = stringResource(id = R.string.move_screen_fast_move_acceleration),
                    secondaryText = stringResource(
                        id = R.string.move_screen_fast_move_acceleration_summary
                    )
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
                verticalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimensions.listItem.contentGap
                )
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
                        .height(MaterialTheme.dimensions.dialog.previousAppsListHeight)
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
                verticalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimensions.listItem.contentGap
                )
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
            .padding(vertical = MaterialTheme.dimensions.layout.contentVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        AsyncImage(
            modifier = Modifier.size(MaterialTheme.dimensions.listItem.minimumTouchTarget),
            model = appInfo.icon,
            contentDescription = null,
            imageLoader = context.imageLoader,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(start = MaterialTheme.dimensions.listItem.iconTextGap)
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
            modifier = Modifier.padding(end = MaterialTheme.dimensions.topBar.contentInset),
            checked = selected,
            onCheckedChange = onSelect
        )
    }
}
