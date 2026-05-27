package com.aaron.sidegesture.ui.screen.advancedsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelAppSwitchWindowModeDelayMs
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelAppSwitchWindowModeDelayMs
import com.aaron.sidegesture.constant.GlobalSettings.getDayNightModeText
import com.aaron.sidegesture.entity.ActionPanelStyles
import com.aaron.sidegesture.entity.AnimationStyles
import com.aaron.sidegesture.entity.DayNightMode
import com.aaron.sidegesture.entity.normalizeActionPanelStyleType
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.EdgeMenuPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    onNavToAppBlacklist: () -> Unit,
    onNavToAnimationStyle: () -> Unit,
    onNavToActionPanelStyle: () -> Unit,
    vm: AdvancedSettingsVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.advanced_settings)
            )
            MyColumn {
                MySection {
                    MyTextButton(
                        onClick = onNavToAppBlacklist,
                        text = stringResource(id = R.string.exclude_app),
                        secondaryText = stringResource(id = R.string.exclude_app_hint)
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.gesture_button_extension)
                ) {
                    MyTextSwitch(
                        onTextClick = onNavToAnimationStyle,
                        onCheckedChange = { vm.onShowAnimation(it) },
                        checked = uiState.showAnimation,
                        text = stringResource(id = R.string.animation_style),
                        secondaryText = getAnimationStyleText(uiState.animationStyleType)
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onFitSoftKeyboardChange(it) },
                        checked = uiState.fitSoftKeyboard,
                        text = stringResource(id = R.string.fit_soft_keyboard),
                        secondaryText = stringResource(id = R.string.fit_soft_keyboard_hint)
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.action_panel)
                ) {
                    MyTextButton(
                        onClick = onNavToActionPanelStyle,
                        text = stringResource(id = R.string.action_panel_style),
                        secondaryText = getActionPanelStyleText(uiState.actionPanelStyleType)
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onActionPanelAppLongPressLaunchPopupChanged(it) },
                        checked = uiState.actionPanelAppLongPressLaunchPopup,
                        text = stringResource(id = R.string.action_panel_launch_app),
                        secondaryText = stringResource(id = R.string.action_panel_launch_app_hint)
                    )
                    MyTextSlider(
                        value = uiState.actionPanelAppSwitchWindowModeDelayMs.toFloat(),
                        onValueChange = { vm.onActionPanelAppSwitchWindowModeDelayMsChange(it) },
                        onValueChangeFinished = vm::onActionPanelAppSwitchWindowModeDelayMsChangeFinished,
                        text = stringResource(id = R.string.action_panel_app_switch_window_mode_delay_ms),
                        sliderValueHint = stringResource(id = R.string.short1) to stringResource(id = R.string.long1),
                        valueRange = MinActionPanelAppSwitchWindowModeDelayMs.toFloat()..MaxActionPanelAppSwitchWindowModeDelayMs.toFloat()
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.hide_gesture_button)
                ) {
                    MyTextSwitch(
                        onCheckedChange = { vm.onHideLandscapeChange(it) },
                        checked = uiState.hideLandscape,
                        text = stringResource(id = R.string.landscape)
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onHideScreenLockChange(it) },
                        checked = uiState.hideScreenLock,
                        text = stringResource(id = R.string.lock_screen)
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onHideHomeScreenChange(it) },
                        checked = uiState.hideHomeScreen,
                        text = stringResource(id = R.string.launcher)
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onHideTemporaryChange(it) },
                        checked = uiState.hideTemporary,
                        text = stringResource(id = R.string.click_to_hide_button_temporary)
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.app_settings)
                ) {
                    MyTextSwitch(
                        onCheckedChange = { vm.onVolumeButtonSwitchSong(it) },
                        checked = uiState.volumeButtonSwitchSong,
                        text = stringResource(id = R.string.volume_button_switch_song),
                        secondaryText = stringResource(id = R.string.volume_button_switch_song_hint)
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onExcludeFromRecentsChange(it) },
                        checked = uiState.excludeFromRecents,
                        text = stringResource(id = R.string.exclude_from_recents),
                        secondaryText = stringResource(id = R.string.exclude_from_recents_hint)
                    )
                    if (uiState.showDynamicColorOption) {
                        MyTextSwitch(
                            onCheckedChange = { vm.onDynamicColorChange(it) },
                            checked = uiState.dynamicColor,
                            text = stringResource(id = R.string.dynamic_color),
                            secondaryText = stringResource(id = R.string.dynamic_color_hint)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinItemHeightNoSecondary)
                            .onSingleClick {
                                vm.showDayNightModeDropdownMenu(true)
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
                            text = stringResource(id = R.string.day_night_mode),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = getDayNightModeText(uiState.dayNightMode),
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
                                expanded = uiState.showDayNightModeDropdownMenu,
                                onDismissRequest = { vm.showDayNightModeDropdownMenu(false) }
                            ) {
                                listOf(
                                    DayNightMode.Auto to getDayNightModeText(DayNightMode.Auto),
                                    DayNightMode.Day to getDayNightModeText(DayNightMode.Day),
                                    DayNightMode.Night to getDayNightModeText(DayNightMode.Night),
                                ).fastForEach { (effectValue, text) ->
                                    key(effectValue) {
                                        DropdownMenuItem(
                                            onClick = {
                                                vm.onDayNightModeChange(effectValue)
                                                vm.showDayNightModeDropdownMenu(false)
                                            },
                                            text = {
                                                Text(text = text)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getAnimationStyleText(type: Int): String {
    return when (type) {
        AnimationStyles.TYPE_WAVE -> stringResource(id = R.string.animation_style_wave)
        AnimationStyles.TYPE_CAPSULE -> stringResource(id = R.string.animation_style_capsule)
        AnimationStyles.TYPE_BUBBLE -> stringResource(id = R.string.animation_style_bubble)
        else -> stringResource(id = R.string.animation_style_wave)
    }
}

@Composable
private fun getActionPanelStyleText(type: Int): String {
    return when (normalizeActionPanelStyleType(type)) {
        ActionPanelStyles.TYPE_ARC,
        ActionPanelStyles.TYPE_SECTOR -> stringResource(id = R.string.action_panel_style_sector)
        else -> stringResource(id = R.string.action_panel_style_folder)
    }
}
