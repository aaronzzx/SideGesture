package com.aaron.sidegesture.ui.screen.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ktx.gotoAccessibilitySettings
import com.aaron.sidegesture.ktx.gotoOverlaySettings
import com.aaron.sidegesture.ui.theme.EdgeMenuPadding
import com.aaron.sidegesture.ui.theme.RootPadding
import com.aaron.sidegesture.ui.theme.ScrollBottomPadding
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.theme.SectionPaddingNoTitle
import com.aaron.sidegesture.ui.widget.MyExpandableColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@Serializable
data object Home

@Composable
fun HomeScreen(
    onNavToUnlock: () -> Unit,
    onNavToAbout: () -> Unit,
    onNavToAdvancedSettings: () -> Unit,
    onNavToGestureSettings: () -> Unit,
    onNavToGestureButtonSettings: () -> Unit,
    vm: HomeVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(key1 = lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.updateSystemPermissions()
            }
        }

        Column {
            TopBar(
                onBack = { },
                title = stringResource(id = R.string.home_title),
                titleStyle = MaterialTheme.typography.headlineMedium,
                showBackIcon = false,
                actions = {
                    IconButton(onClick = { vm.showMoreMenu(true) }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.more)
                        )
                    }

                    DropdownMenu(
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        offset = DpOffset(-EdgeMenuPadding, 0.dp),
                        expanded = uiState.showMoreMenu,
                        onDismissRequest = { vm.showMoreMenu(false) }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                vm.showMoreMenu(false)
                            },
                            text = {
                                Text(text = stringResource(id = R.string.reset_all_settings),)
                            }
                        )
                        DropdownMenuItem(
                            onClick = {
                                vm.showMoreMenu(false) {
                                    onNavToUnlock()
                                }
                            },
                            text = {
                                Text(text = stringResource(id = R.string.unlock_advanced_feature))
                            }
                        )
                        DropdownMenuItem(
                            onClick = {
                                vm.showMoreMenu(false) {
                                    onNavToAbout()
                                }
                            },
                            text = {
                                Text(text = stringResource(id = R.string.about))
                            }
                        )
                    }
                }
            )

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(RootPadding)
                    .padding(bottom = ScrollBottomPadding)
            ) {
                MySection(title = stringResource(id = R.string.initial_settings)) {
                    MyTextSwitch(
                        onCheckedChange = {
                            vm.onGestureEnabledChange(it)
                        },
                        checked = uiState.isGestureEnabled,
                        text = stringResource(id = R.string.gesture_switch)
                    )
                    MyTextSwitch(
                        onCheckedChange = {
                            context.gotoOverlaySettings()
                        },
                        checked = uiState.isDrawOverlayEnabled,
                        text = stringResource(id = R.string.system_overlay)
                    )
                    MyTextSwitch(
                        onCheckedChange = {
                            context.gotoAccessibilitySettings()
                        },
                        checked = uiState.isAccessibilityEnabled,
                        text = stringResource(id = R.string.accessibility_service)
                    )
                }

                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.global_settings)
                ) {
                    MyTextButton(
                        onClick = onNavToAdvancedSettings,
                        text = stringResource(id = R.string.advanced_settings),
                        secondaryText = stringResource(id = R.string.advanced_settings_hint)
                    )
                    MyTextButton(
                        onClick = onNavToGestureSettings,
                        text = stringResource(id = R.string.gesture_settings),
                        secondaryText = stringResource(id = R.string.gesture_settings_hint)
                    )
                    MyTextButton(
                        onClick = onNavToGestureButtonSettings,
                        text = stringResource(id = R.string.gesture_button_settings),
                        secondaryText = stringResource(id = R.string.gesture_button_settings_hint)
                    )
                }

                val density = LocalDensity.current
                var gestureButtonListOffset by remember { mutableStateOf(Offset.Unspecified) }
                if (uiState.isGestureButtonListExpanded && gestureButtonListOffset.isSpecified) {
                    LaunchedEffect(scrollState) {
                        scrollState.animateScrollBy(
                            value = gestureButtonListOffset.y,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
                MyExpandableColumn(
                    modifier = Modifier
                        .onGloballyPositioned {
                            if (gestureButtonListOffset.isUnspecified) {
                                density.run {
                                    val position = it.positionInParent()
                                    gestureButtonListOffset = position.copy(
                                        y = position.y + RootPadding.toPx()
                                    )
                                }
                            }
                        }
                        .padding(top = SectionPaddingNoTitle),
                    title = stringResource(id = R.string.gesture_button_list),
                    expanded = uiState.isGestureButtonListExpanded,
                    onExpandedChange = {
                        vm.expandGestureButtonList(it)
                    }
                ) {
                    repeat(9) { index ->
                        MyTextSwitch(
                            onTextClick = { },
                            onCheckedChange = { },
                            checked = true,
                            text = "触钮0${index + 1}",
                            secondaryText = "返回键,最近键,快速工具,主页键,快速启动器,任务切换器,隐藏触钮,启动应用程序,静音开关,打开通知面板,打开快捷面板,锁屏,关闭应用程序",
                            secondaryTextColor = MaterialTheme.colorScheme.primary,
                            markColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}