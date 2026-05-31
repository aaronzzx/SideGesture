package com.aaron.sidegesture.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.LoadingComponent
import com.aaron.compose.component.UDFComponent
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
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindowMode
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.widget.MyTextSlider

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
        LoadingComponent(
            modifier = Modifier.fillMaxWidth(),
            component = vm.loadingComponent
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                var inputPkgName by remember {
                    mutableStateOf(TextFieldValue())
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = inputPkgName,
                    onValueChange = { inputPkgName = it },
                    singleLine = true,
                    label = {
                        Text(
                            text = stringResource(R.string.exclude_app),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    placeholder = {
                        Text(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            text = stringResource(R.string.typing_package_name_and_click_done),
                            fontSize = 14.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    keyboardActions = KeyboardActions(
                        onDone = {
                            vm.onPreviousAppOperation(inputPkgName.text, true)
                            inputPkgName = TextFieldValue()
                        }
                    )
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(
                        items = uiState.actionSettings.previousApp.packageNames,
                        key = { it }
                    ) { item ->
                        Row(
                            modifier = Modifier.fillParentMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ItemPadding)
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = item,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Image(
                                modifier = Modifier
                                    .size(MinInteractiveSize)
                                    .clip(CircleShape)
                                    .onSingleClick {
                                        vm.onPreviousAppOperation(item, false)
                                    },
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                contentScale = ContentScale.Inside,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                            )
                        }
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
