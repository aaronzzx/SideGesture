package com.aaron.sidegesture.ui.screen.quicklauncher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.global.QuickLauncherSettings
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.TopBar
import com.aaron.sidegesture.ui.widget.formatSliderInteger

@Composable
fun QuickLauncherSettingsScreen(
    onBack: () -> Unit,
    vm: QuickLauncherSettingsVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(R.string.quick_launcher_settings)
            )
            MyColumn {
                MySection(title = stringResource(R.string.layout)) {
                    MyTextSlider(
                        value = uiState.settings.rows.toFloat(),
                        onValueChange = vm::onRowsChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(R.string.rows),
                        enabled = uiState.loaded,
                        valueRange = QuickLauncherSettings.MinRows.toFloat()..
                            QuickLauncherSettings.MaxRows.toFloat(),
                        valueFormatter = { formatSliderInteger(it) }
                    )
                    MyTextSlider(
                        value = uiState.settings.columns.toFloat(),
                        onValueChange = vm::onColumnsChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(R.string.columns),
                        enabled = uiState.loaded,
                        valueRange = QuickLauncherSettings.MinColumns.toFloat()..
                            QuickLauncherSettings.MaxColumns.toFloat(),
                        valueFormatter = { formatSliderInteger(it) }
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(R.string.quick_launcher_appearance)
                ) {
                    MyTextSlider(
                        value = uiState.settings.iconSizeDp.toFloat(),
                        onValueChange = vm::onIconSizeChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(R.string.icon_size),
                        enabled = uiState.loaded,
                        sliderValueHint = stringResource(R.string.small) to
                            stringResource(R.string.large),
                        valueRange = QuickLauncherSettings.MinIconSizeDp.toFloat()..
                            QuickLauncherSettings.MaxIconSizeDp.toFloat(),
                        valueFormatter = { formatSliderInteger(it, " dp") }
                    )
                    MyTextSlider(
                        value = uiState.settings.textSizeSp.toFloat(),
                        onValueChange = vm::onTextSizeChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(R.string.text_size),
                        enabled = uiState.loaded,
                        sliderValueHint = stringResource(R.string.small) to
                            stringResource(R.string.large),
                        valueRange = QuickLauncherSettings.MinTextSizeSp.toFloat()..
                            QuickLauncherSettings.MaxTextSizeSp.toFloat(),
                        valueFormatter = { formatSliderInteger(it, " sp") }
                    )
                }
            }
        }
    }
}
