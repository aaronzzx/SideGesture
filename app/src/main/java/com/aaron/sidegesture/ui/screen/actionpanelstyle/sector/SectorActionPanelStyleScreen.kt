package com.aaron.sidegesture.ui.screen.actionpanelstyle.sector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelInitialRadiusRatio
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelItemSize
import com.aaron.sidegesture.constant.GlobalSettings.MaxActionPanelItemSpacingRatio
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelInitialRadiusRatio
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelItemSize
import com.aaron.sidegesture.constant.GlobalSettings.MinActionPanelItemSpacingRatio
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.TopBar

/**
 * @author OpenAI
 * @since 2026/5/22
 */
@Composable
fun SectorActionPanelStyleScreen(
    onBack: () -> Unit,
    vm: SectorActionPanelStyleVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.action_panel_style_sector)
            )
            MyColumn {
                MySection(title = stringResource(id = R.string.icon)) {
                    MyTextSlider(
                        value = uiState.style.itemSize.toFloat(),
                        onValueChange = vm::onItemSizeChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.icon_size),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinActionPanelItemSize.toFloat()..MaxActionPanelItemSize.toFloat()
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.layout)
                ) {
                    MyTextSlider(
                        value = uiState.style.initialRadiusRatio,
                        onValueChange = vm::onInitialRadiusRatioChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.expand_radius),
                        valueRange = MinActionPanelInitialRadiusRatio..MaxActionPanelInitialRadiusRatio
                    )
                    MyTextSlider(
                        value = uiState.style.itemSpacingRatio,
                        onValueChange = vm::onItemSpacingRatioChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.expand_spacing),
                        valueRange = MinActionPanelItemSpacingRatio..MaxActionPanelItemSpacingRatio
                    )
                }
            }
        }
    }
}
