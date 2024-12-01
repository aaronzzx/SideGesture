package com.aaron.sidegesture.ui.screen.advancedsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.theme.SectionPaddingNoTitle
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
                MySection(modifier = Modifier.padding(top = SectionPaddingNoTitle)) {
                    MyTextSwitch(
                        onCheckedChange = { vm.onFitSoftKeyboardChange(it) },
                        checked = uiState.fitSoftKeyboard,
                        text = stringResource(id = R.string.fit_soft_keyboard),
                        secondaryText = stringResource(id = R.string.fit_soft_keyboard_hint)
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
                        onCheckedChange = { vm.onHideQuickPanelChange(it) },
                        checked = uiState.hideQuickPanel,
                        text = stringResource(id = R.string.quick_settings)
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
                }
            }
        }
    }
}