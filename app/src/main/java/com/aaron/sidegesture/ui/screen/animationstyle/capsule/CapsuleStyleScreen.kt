package com.aaron.sidegesture.ui.screen.animationstyle.capsule

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.MaxBezierStrokeWidth
import com.aaron.sidegesture.constant.GlobalSettings.MaxCapsuleCornerRadius
import com.aaron.sidegesture.constant.GlobalSettings.MaxCapsuleLength
import com.aaron.sidegesture.constant.GlobalSettings.MaxCapsuleThickness
import com.aaron.sidegesture.constant.GlobalSettings.MaxIconScale
import com.aaron.sidegesture.constant.GlobalSettings.MinBezierStrokeWidth
import com.aaron.sidegesture.constant.GlobalSettings.MinCapsuleCornerRadius
import com.aaron.sidegesture.constant.GlobalSettings.MinCapsuleLength
import com.aaron.sidegesture.constant.GlobalSettings.MinCapsuleThickness
import com.aaron.sidegesture.constant.GlobalSettings.MinIconScale
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_ANGLE
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_ARROW
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_ARROW_NEW
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_TRIANGLE
import com.aaron.sidegesture.ktx.getWaveStyleIcon
import com.aaron.sidegesture.ui.screen.animationstyle.capsule.CapsuleStyleVM.UiEvent
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.appColors
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.motion
import com.aaron.sidegesture.ui.widget.ColorPickerDialog
import com.aaron.sidegesture.ui.widget.MyColorDisplay
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MyExpandableColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.TopBar
import com.aaron.sidegesture.ui.widget.formatSliderDecimal
import com.aaron.sidegesture.ui.widget.formatSliderInteger
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.launch

/**
 * @author OpenAI
 * @since 2026/5/20
 */
@Composable
fun CapsuleStyleScreen(
    onBack: () -> Unit,
    vm: CapsuleStyleVM = viewModel()
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val settingsContentPlacementStiffness =
        MaterialTheme.motion.settingsContentPlacementStiffness
    UDFComponent(
        component = vm.udfComponent,
        onEvent = { event ->
            when (event) {
                UiEvent.ScrollToBottom -> {
                    coroutineScope.launch {
                        scrollState.animateScrollBy(
                            value = 1000f,
                            animationSpec = spring(stiffness = settingsContentPlacementStiffness)
                        )
                    }
                }
            }
        }
    ) { uiState ->
        if (uiState.colorPickerDialog.first) {
            ColorPickerDialog(
                onDismissRequest = { vm.colorPickerDialog.show(false) },
                onColorPicked = { color ->
                    vm.colorPickerDialog.onColorChange(color.toArgb())
                    vm.colorPickerDialog.confirm()
                },
                initialColor = Color(uiState.colorPickerDialog.second)
            )
        }

        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.animation_style_capsule)
            )
            MyColumn(scrollState = scrollState) {
                MySection(title = stringResource(id = R.string.color_outline)) {
                    MyTextButton(
                        onClick = {
                            vm.colorPickerDialog.show(
                                show = true,
                                color = uiState.animationStyle.backgroundColor,
                                belongsTo = uiState.animationStyle::backgroundColor
                            )
                        },
                        text = stringResource(id = R.string.background_color),
                        prefix = {
                            MyColorDisplay(color = Color(uiState.animationStyle.backgroundColor))
                        }
                    )
                    MyTextButton(
                        onClick = {
                            vm.colorPickerDialog.show(
                                show = true,
                                color = uiState.animationStyle.strokeColor,
                                belongsTo = uiState.animationStyle::strokeColor
                            )
                        },
                        text = stringResource(id = R.string.stroke_color),
                        prefix = {
                            MyColorDisplay(color = Color(uiState.animationStyle.strokeColor))
                        }
                    )
                    MyTextSlider(
                        value = uiState.animationStyle.strokeWidth.toFloat(),
                        onValueChange = vm::onStrokeWidthChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.stroke_width),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinBezierStrokeWidth.toFloat()..MaxBezierStrokeWidth.toFloat(),
                        valueFormatter = { formatSliderInteger(ConvertUtils.px2dp(it).toFloat(), " dp") }
                    )
                }

                MySection(
                    modifier = Modifier.padding(top = MaterialTheme.dimensions.layout.sectionSpacing),
                    title = stringResource(id = R.string.shape_size)
                ) {
                    MyTextSlider(
                        value = uiState.animationStyle.thickness.toFloat(),
                        onValueChange = vm::onThicknessChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.width),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinCapsuleThickness.toFloat()..MaxCapsuleThickness.toFloat(),
                        valueFormatter = { formatSliderInteger(ConvertUtils.px2dp(it).toFloat(), " dp") }
                    )
                    MyTextSlider(
                        value = uiState.animationStyle.maxLength.toFloat(),
                        onValueChange = vm::onMaxLengthChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.length),
                        sliderValueHint = stringResource(id = R.string.short1) to stringResource(id = R.string.long1),
                        valueRange = MinCapsuleLength.toFloat()..MaxCapsuleLength.toFloat(),
                        valueFormatter = { formatSliderInteger(ConvertUtils.px2dp(it).toFloat(), " dp") }
                    )
                    MyTextSlider(
                        value = uiState.animationStyle.cornerRadius.toFloat(),
                        onValueChange = vm::onCornerRadiusChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.corner_radius),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinCapsuleCornerRadius.toFloat()..MaxCapsuleCornerRadius.toFloat(),
                        valueFormatter = { formatSliderInteger(ConvertUtils.px2dp(it).toFloat(), " dp") }
                    )
                }

                MySection(
                    modifier = Modifier.padding(top = MaterialTheme.dimensions.layout.sectionSpacing),
                    title = stringResource(id = R.string.icon)
                ) {
                    MyTextButton(
                        onClick = {
                            vm.colorPickerDialog.show(
                                show = true,
                                color = uiState.animationStyle.iconColor,
                                belongsTo = uiState.animationStyle::iconColor
                            )
                        },
                        text = stringResource(id = R.string.tint),
                        prefix = {
                            MyColorDisplay(color = Color(uiState.animationStyle.iconColor))
                        }
                    )
                    MyTextSlider(
                        value = uiState.animationStyle.iconScale,
                        onValueChange = vm::onIconScaleChange,
                        onValueChangeFinished = vm::saveSettings,
                        text = stringResource(id = R.string.scaling),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinIconScale..MaxIconScale,
                        valueFormatter = { formatSliderDecimal(it, 2) }
                    )

                    MyExpandableColumn(
                        onExpandedChange = vm::onCustomIconExpandedChange,
                        title = stringResource(id = R.string.custom_icon),
                        expanded = uiState.isCustomIconExpanded,
                        shape = RectangleShape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MaterialTheme.dimensions.listItem.minimumTouchTarget),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            listOf(
                                ICON_TYPE_ARROW,
                                ICON_TYPE_TRIANGLE,
                                ICON_TYPE_ANGLE,
                                ICON_TYPE_ARROW_NEW
                            ).fastForEach { iconType ->
                                val selected = uiState.animationStyle.iconType == iconType
                                Image(
                                    modifier = Modifier
                                        .size(MaterialTheme.dimensions.listItem.compactControlVisualSize)
                                        .clipToBackground(
                                            color = when (selected) {
                                                true -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.primary.copy(
                                                    alpha = MaterialTheme.alpha.lowEmphasis
                                                )
                                            },
                                            shape = CircleShape
                                        )
                                        .onSingleClick {
                                            vm.onIconTypeChange(iconType)
                                        },
                                    painter = getWaveStyleIcon(iconType),
                                    contentDescription = null,
                                    contentScale = ContentScale.Inside,
                                    colorFilter = ColorFilter.tint(
                                        MaterialTheme.appColors.fixedWhite
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
