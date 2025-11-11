package com.aaron.sidegesture.ui.screen.animationstyle.wave

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
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
import com.aaron.sidegesture.constant.GlobalSettings.MaxBezierLength
import com.aaron.sidegesture.constant.GlobalSettings.MaxBezierStrokeWidth
import com.aaron.sidegesture.constant.GlobalSettings.MaxBezierWidth
import com.aaron.sidegesture.constant.GlobalSettings.MaxIconScale
import com.aaron.sidegesture.constant.GlobalSettings.MinBezierLength
import com.aaron.sidegesture.constant.GlobalSettings.MinBezierStrokeWidth
import com.aaron.sidegesture.constant.GlobalSettings.MinBezierWidth
import com.aaron.sidegesture.constant.GlobalSettings.MinIconScale
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_ANGLE
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_ARROW
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_ARROW_NEW
import com.aaron.sidegesture.entity.WaveStyle.Companion.ICON_TYPE_TRIANGLE
import com.aaron.sidegesture.ktx.getWaveStyleIcon
import com.aaron.sidegesture.ui.screen.animationstyle.wave.WaveStyleVM.UiEvent
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.theme.SubMinInteractiveSize
import com.aaron.sidegesture.ui.widget.ColorPickerDialog
import com.aaron.sidegesture.ui.widget.MyColorDisplay
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MyExpandableColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.MyTextSlider
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar
import kotlinx.coroutines.launch

/**
 * @author DS-Z
 * @since 2025/11/4
 */

@Composable
fun WaveStyleScreen(
    onBack: () -> Unit,
    vm: WaveStyleVM = viewModel()
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    UDFComponent(
        component = vm.udfComponent,
        onEvent = { event ->
            when (event) {
                UiEvent.ScrollToBottom -> {
                    coroutineScope.launch {
                        scrollState.animateScrollBy(
                            value = 1000f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            }
        }
    ) { uiState ->
        if (uiState.colorPickerDialog.first) {
            ColorPickerDialog(
                onDismissRequest = {
                    vm.colorPickerDialog.show(false)
                },
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
                title = stringResource(id = R.string.animation_style)
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
                        onValueChange = { vm.onStrokeWidthChange(it) },
                        onValueChangeFinished = { vm.saveSettings() },
                        text = stringResource(id = R.string.stroke_width),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinBezierStrokeWidth.toFloat()..MaxBezierStrokeWidth.toFloat()
                    )
//                    MyTextSwitch(
//                        onCheckedChange = { vm.onStickySlideChange(it) },
//                        checked = uiState.animationStyle.stickySlideEnabled,
//                        text = stringResource(id = R.string.sticky_slide),
//                        secondaryText = stringResource(id = R.string.sticky_slide_tips)
//                    )
                }

                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.shape_size)
                ) {
                    MyTextSlider(
                        value = uiState.animationStyle.width.toFloat(),
                        onValueChange = { vm.onWidthChange(it) },
                        onValueChangeFinished = { vm.saveSettings() },
                        text = stringResource(id = R.string.width),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinBezierWidth.toFloat()..MaxBezierWidth.toFloat()
                    )
                    MyTextSlider(
                        value = uiState.animationStyle.bezierLengthHalfRatio.toFloat(),
                        onValueChange = { vm.onLengthHalfRatioChange(it) },
                        onValueChangeFinished = { vm.saveSettings() },
                        text = stringResource(id = R.string.length),
                        sliderValueHint = stringResource(id = R.string.short1) to stringResource(id = R.string.long1),
                        valueRange = MinBezierLength.toFloat()..MaxBezierLength.toFloat()
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onSafeBoundsChange(it) },
                        checked = uiState.animationStyle.safeBounds,
                        text = stringResource(id = R.string.reserved_bounds),
                        secondaryText = stringResource(id = R.string.reserved_bounds_tips)
                    )
                    MyTextSwitch(
                        onCheckedChange = { vm.onTransformEnabledChange(it) },
                        checked = uiState.animationStyle.transformEnabled,
                        text = stringResource(id = R.string.bezier_transform),
                        secondaryText = stringResource(id = R.string.bezier_transform_tips)
                    )
                }

                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
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
                        onValueChange = { vm.onIconScaleChange(it) },
                        onValueChangeFinished = { vm.saveSettings() },
                        text = stringResource(id = R.string.scaling),
                        sliderValueHint = stringResource(id = R.string.small) to stringResource(id = R.string.large),
                        valueRange = MinIconScale..MaxIconScale
                    )

                    MyExpandableColumn(
                        onExpandedChange = { vm.onCustomIconExpandedChange(it) },
                        title = stringResource(id = R.string.custom_icon),
                        expanded = uiState.isCustomIconExpanded,
                        shape = RectangleShape
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MinInteractiveSize),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
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
                                        .size(SubMinInteractiveSize)
                                        .clipToBackground(
                                            color = when (selected) {
                                                true -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            },
                                            shape = CircleShape
                                        )
                                        .onSingleClick {
                                            vm.onIconTypeChange(iconType)
                                        },
                                    painter = getWaveStyleIcon(iconType),
                                    contentDescription = null,
                                    contentScale = ContentScale.Inside,
                                    colorFilter = ColorFilter.tint(color = Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}