package com.aaron.sidegesture.ui.screen.actionpanelstyle.folder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelColumns
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelCornerRadius
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelItemSize
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelRows
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelScrollHotZoneHeight
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelScrollSpeed
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelColumns
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelCornerRadius
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelItemSize
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelRows
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelScrollHotZoneHeight
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelScrollSpeed
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.TopBar
import com.aaron.sidegesture.ui.widget.formatSliderInteger
import com.blankj.utilcode.util.ConvertUtils

/**
 * @author OpenAI
 * @since 2026/5/22
 */
@Composable
fun FolderActionPanelStyleScreen(
    onBack: () -> Unit,
    vm: FolderActionPanelStyleVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.action_panel_style_folder)
            )
            MyColumn {
                MySection(title = stringResource(id = R.string.icon)) {
                    MyTextSlider(
                        value = uiState.style.itemSize.toFloat(),
                        onValueChange = vm::onItemSizeChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.icon_size),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinActionPanelItemSize.toFloat()..MaxActionPanelItemSize.toFloat(),
                        valueFormatter = { formatSliderInteger(ConvertUtils.px2dp(it).toFloat(), " dp") }
                    )
                    MyTextSlider(
                        value = uiState.style.columns.toFloat(),
                        onValueChange = vm::onColumnsChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.columns),
                        valueRange = MinActionPanelColumns..MaxActionPanelColumns,
                        valueFormatter = { formatSliderInteger(it) }
                    )
                    MyTextSlider(
                        value = uiState.style.rows.toFloat(),
                        onValueChange = vm::onRowsChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.rows),
                        valueRange = MinActionPanelRows..MaxActionPanelRows,
                        valueFormatter = { formatSliderInteger(it) }
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = MaterialTheme.dimensions.layout.sectionSpacing),
                    title = stringResource(id = R.string.scroll)
                ) {
                    MyTextSlider(
                        value = uiState.style.scrollSpeed.toFloat(),
                        onValueChange = vm::onScrollSpeedChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.scroll_speed),
                        sliderValueHint = stringResource(id = R.string.slow) to stringResource(id = R.string.fast),
                        valueRange = MinActionPanelScrollSpeed..MaxActionPanelScrollSpeed,
                        valueFormatter = { formatSliderInteger(it, " px/帧") }
                    )
                    MyTextSlider(
                        value = uiState.style.scrollHotZoneHeight.toFloat(),
                        onValueChange = vm::onScrollHotZoneHeightChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.scroll_hot_zone_height),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinActionPanelScrollHotZoneHeight..MaxActionPanelScrollHotZoneHeight,
                        valueFormatter = { formatSliderInteger(ConvertUtils.px2dp(it).toFloat(), " dp") }
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = MaterialTheme.dimensions.layout.sectionSpacing),
                    title = stringResource(id = R.string.background)
                ) {
                    MyTextSlider(
                        value = uiState.style.cornerRadius.toFloat(),
                        onValueChange = vm::onCornerRadiusChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.corner_radius),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinActionPanelCornerRadius.toFloat()..MaxActionPanelCornerRadius.toFloat(),
                        valueFormatter = { formatSliderInteger(ConvertUtils.px2dp(it).toFloat(), " dp") }
                    )
                }
            }
        }
    }
}
