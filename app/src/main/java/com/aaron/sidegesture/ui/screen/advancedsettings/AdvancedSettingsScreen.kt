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
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.DayNightMode
import com.aaron.sidegesture.constant.GlobalSettings.getDayNightModeText
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

@Serializable
data object AdvancedSettings

@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    onNavToAppBlacklist: () -> Unit,
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
//                MySection(modifier = Modifier.padding(top = SectionPaddingNoTitle)) {
//                    MyTextSwitch(
//                        onTextClick = { /*TODO*/ },
//                        onCheckedChange = { /*TODO*/ },
//                        checked = false,
//                        text = stringResource(id = R.string.animation_style),
//                        secondaryText = "TODO",
//                        secondaryTextColor = MaterialTheme.colorScheme.primary
//                    )
//                    MyTextSwitch(
//                        onTextClick = { /*TODO*/ },
//                        onCheckedChange = { /*TODO*/ },
//                        checked = false,
//                        text = stringResource(id = R.string.action_panel_style),
//                        secondaryText = "TODO",
//                        secondaryTextColor = MaterialTheme.colorScheme.primary
//                    )
//                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.hide_gesture_button)
                ) {
                    MyTextSwitch(
                        onCheckedChange = { vm.onHideLandscapeChange(it) },
                        checked = uiState.hideLandscape,
                        text = stringResource(id = R.string.landscape)
                    )
//                    MyTextSwitch(
//                        onCheckedChange = { vm.onHideQuickPanelChange(it) },
//                        checked = uiState.hideQuickPanel,
//                        text = stringResource(id = R.string.quick_settings),
//                        contentPadding = contentPadding
//                    )
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
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.app_settings)
                ) {
//                    MyTextSwitch(
//                        onCheckedChange = { vm.onFitSoftKeyboardChange(it) },
//                        checked = uiState.fitSoftKeyboard,
//                        text = stringResource(id = R.string.fit_soft_keyboard),
//                        secondaryText = stringResource(id = R.string.fit_soft_keyboard_hint)
//                    )
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