package com.aaron.sidegesture.ui.screen.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings.GestureButtonColorAlpha
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.actionTextCompose
import com.aaron.sidegesture.ktx.bounds
import com.aaron.sidegesture.ktx.buttonTextCompose
import com.aaron.sidegesture.ktx.gotoAccessibilitySettings
import com.aaron.sidegesture.ktx.gotoIgnoreBatteryOptimizations
import com.aaron.sidegesture.ui.screen.home.HomeVM.UiEvent
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
import com.aaron.sidegesture.ui.theme.RootPadding
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.theme.SectionPaddingNoTitle
import com.aaron.sidegesture.ui.theme.TopBarPaddingExtra
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MyExpandableColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.MyTextSwitch
import com.aaron.sidegesture.ui.widget.TopBar
import com.aaron.sidegesture.utils.KeepAliveHelper
import com.blankj.utilcode.util.TimeUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@Composable
fun HomeScreen(
    onNavToUnlock: () -> Unit,
    onNavToAbout: () -> Unit,
    onNavToAdvancedSettings: () -> Unit,
    onNavToGestureSettings: () -> Unit,
    onNavToGestureButtonSettings: (GestureButton) -> Unit,
    vm: HomeVM = viewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    UDFComponent(
        component = vm.udfComponent,
        onEvent = { event ->
            when (event) {
                is UiEvent.ScrollToBottom -> {
                    scrollState.animateScrollTo(
                        value = scrollState.maxValue,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
                is UiEvent.ScrollToEvent -> {
                    scrollState.animateScrollTo(
                        value = event.offsetY,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                }
            }
        }
    ) { uiState ->
        if (uiState.showResetWarningDialog) {
            MyAlertDialog(
                onDismissRequest = { vm.showResetWarningDialog(false) },
                onConfirmClick = { vm.reset() },
                title = stringResource(id = R.string.reset_default_settings_warning),
                text = stringResource(id = R.string.reset_default_settings_warning_desc)
            )
        }

        val createFileLauncher = rememberLauncherForActivityResult(
            contract = CreateDocument("*/*")
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            vm.backup(context, uri)
        }
        val getFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            vm.restore(context, uri)
        }
        if (uiState.showBackupRestoreDialog) {
            BackupRestoreDialog(
                onDismissRequest = {
                    vm.showBackupRestoreDialog(false)
                },
                onBackupRequest = {
                    vm.showBackupRestoreDialog(false)
                    val appName = context.getString(context.applicationInfo.labelRes)
                    val timestamp = System.currentTimeMillis()
                    val date = TimeUtils.millis2String(timestamp, "yyyyMMdd_HHmmss")
                    val fileName = "${appName}_$date"
                    createFileLauncher.launch(fileName)
                },
                onRestoreRequest = {
                    vm.showBackupRestoreDialog(false)
                    getFileLauncher.launch("*/*")
                }
            )
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(key1 = lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.updatePermissionState()
            }
        }

        Box {
            Column {
                TopBar(
                    onBack = { },
                    title = stringResource(id = R.string.home_title),
                    titleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W900),
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
                            offset = DpOffset(-(TopBarPaddingExtra / 2), 0.dp),
                            expanded = uiState.showMoreMenu,
                            onDismissRequest = { vm.showMoreMenu(false) }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    vm.showMoreMenu(false)
                                    vm.showResetWarningDialog(true)
                                },
                                text = {
                                    Text(text = stringResource(id = R.string.reset_default_settings_warning),)
                                }
                            )
//                            DropdownMenuItem(
//                                onClick = {
//                                    vm.showMoreMenu(false) {
//                                        onNavToUnlock()
//                                    }
//                                },
//                                text = {
//                                    Text(text = stringResource(id = R.string.unlock_advanced_feature))
//                                }
//                            )
                            DropdownMenuItem(
                                onClick = {
                                    vm.showMoreMenu(false) {
                                        vm.showBackupRestoreDialog(true)
                                    }
                                },
                                text = {
                                    Text(text = stringResource(id = R.string.backup_restore))
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

                MyColumn(scrollState = scrollState) {
                    MySection(title = stringResource(id = R.string.initial_settings)) {
                        MyTextSwitch(
                            onCheckedChange = {
                                vm.onAppGestureEnabledChange(it)
                            },
                            checked = uiState.isGestureEnabled,
                            text = stringResource(id = R.string.gesture_switch)
                        )
//                        MyTextSwitch(
//                            onCheckedChange = {
//                                context.gotoOverlaySettings()
//                            },
//                            checked = uiState.isDrawOverlayEnabled,
//                            text = stringResource(id = R.string.system_overlay)
//                        )
//                        MyTextSwitch(
//                            onCheckedChange = {
//                                SystemAlertWindow.start(context)
//                            },
//                            checked = uiState.isPopBackgroundEnabled,
//                            text = stringResource(id = R.string.popup_background)
//                        )
                        MyTextSwitch(
                            onCheckedChange = {
                                context.gotoAccessibilitySettings()
                            },
                            checked = uiState.isAccessibilityEnabled,
                            text = stringResource(id = R.string.accessibility_service)
                        )
                        MyTextSwitch(
                            onCheckedChange = {
                                context.gotoIgnoreBatteryOptimizations()
                            },
                            checked = uiState.isIgnoringBatteryOptimizations,
                            text = stringResource(id = R.string.ignoring_battery_optimizations),
                            secondaryText = stringResource(id = R.string.ignoring_battery_optimizations_hint)
                        )
                        MyTextButton(
                            onClick = {
                                KeepAliveHelper.gotoSettings(context)
                            },
                            text = stringResource(id = R.string.launch_self_permission),
                            secondaryText = stringResource(id = R.string.launch_self_permission_hint)
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
                    }

                    val density = LocalDensity.current
                    var gestureButtonListOffset by remember { mutableIntStateOf(Int.MAX_VALUE) }
                    MySection(
                        modifier = Modifier
                            .onGloballyPositioned {
                                density.run {
                                    val position = it.positionInParent()
                                    gestureButtonListOffset =
                                        (position.y + RootPadding.toPx()).toInt()
                                }
                            }
                            .padding(top = SectionPadding),
                        title = stringResource(id = R.string.gesture_button_list)
                    ) {
                        MyExpandableColumn(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.Transparent,
                            shape = RectangleShape,
                            title = stringResource(id = R.string.bottom_gesture_button_list),
                            expanded = uiState.isBottomGestureButtonListExpanded,
                            onExpandedChange = { expanded ->
                                if (expanded) {
                                    vm.expandBottomGestureButtonList(true, gestureButtonListOffset)
                                } else {
                                    vm.expandBottomGestureButtonList(false)
                                }
                            }
                        ) {
                            uiState.bottomGestureButtons.fastForEach { button ->
                                key(button) {
                                    MyTextSwitch(
                                        onTextClick = { onNavToGestureButtonSettings(button) },
                                        onCheckedChange = { vm.onBottomGestureButtonEnabledChange(button, it) },
                                        checked = button.enabled,
                                        text = button.buttonTextCompose(),
                                        secondaryText = run {
                                            val expected = button.actionTextCompose()
                                            if (expected.isNotEmpty()) {
                                                return@run expected
                                            }
                                            stringResource(id = R.string.action_none)
                                        },
                                        secondaryTextColor = MaterialTheme.colorScheme.primary,
                                        markColor = when (button.isDefault) {
                                            true -> MaterialTheme.colorScheme.primary.copy(alpha = GestureButtonColorAlpha)
                                            else -> Color(button.color).copy(alpha = GestureButtonColorAlpha)
                                        }
                                    )
                                }
                            }
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = MinItemHeightNoSecondary)
                                    .onSingleClick {
                                        vm.addBottomGestureButton()
                                    }
                                    .wrapContentSize(),
                                text = stringResource(id = R.string.add_gesture_button),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        MyExpandableColumn(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.Transparent,
                            shape = RectangleShape,
                            title = stringResource(id = R.string.side_gesture_button_list),
                            expanded = uiState.isSideGestureButtonListExpanded,
                            onExpandedChange = { expanded ->
                                if (expanded) {
                                    vm.expandSideGestureButtonList(true, gestureButtonListOffset)
                                } else {
                                    vm.expandSideGestureButtonList(false)
                                }
                            }
                        ) {
                            uiState.sideGestureButtons.fastForEach { button ->
                                key(button) {
                                    MyTextSwitch(
                                        onTextClick = { onNavToGestureButtonSettings(button) },
                                        onCheckedChange = { vm.onSideGestureButtonEnabledChange(button, it) },
                                        checked = button.enabled,
                                        text = button.buttonTextCompose(),
                                        secondaryText = run {
                                            val expected = button.actionTextCompose()
                                            if (expected.isNotEmpty()) {
                                                return@run expected
                                            }
                                            stringResource(id = R.string.action_none)
                                        },
                                        secondaryTextColor = MaterialTheme.colorScheme.primary,
                                        markColor = when (button.isDefault) {
                                            true -> MaterialTheme.colorScheme.primary.copy(alpha = GestureButtonColorAlpha)
                                            else -> Color(button.color).copy(alpha = GestureButtonColorAlpha)
                                        }
                                    )
                                }
                            }
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = MinItemHeightNoSecondary)
                                    .onSingleClick {
                                        vm.addSideGestureButton()
                                    }
                                    .wrapContentSize(),
                                text = stringResource(id = R.string.add_gesture_button),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isSideGestureButtonListExpanded || uiState.isBottomGestureButtonListExpanded,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
            ) {
                val colorScheme = MaterialTheme.colorScheme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val buttons = if (uiState.isSideGestureButtonListExpanded) {
                                uiState.sideGestureButtons
                            } else {
                                uiState.bottomGestureButtons
                            }
                            buttons.fastForEach { button ->
                                if (!button.enabled) {
                                    return@fastForEach
                                }
                                val bounds = button.bounds()
                                drawRect(
                                    color = when (button.isDefault) {
                                        true -> colorScheme.primary.copy(alpha = GestureButtonColorAlpha)
                                        else -> Color(button.color).copy(alpha = GestureButtonColorAlpha)
                                    },
                                    topLeft = bounds.topLeft,
                                    size = bounds.size
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun BackupRestoreDialog(
    onDismissRequest: () -> Unit,
    onBackupRequest: () -> Unit,
    onRestoreRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AlertDialogDefaults.shape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                MySection {
                    MyTextButton(
                        onClick = onBackupRequest,
                        text = stringResource(id = R.string.backup),
                        secondaryText = stringResource(id = R.string.backup_hint)
                    )
                }
                MySection(modifier = Modifier.padding(top = SectionPaddingNoTitle)) {
                    MyTextButton(
                        onClick = onRestoreRequest,
                        text = stringResource(id = R.string.restore),
                        secondaryText = stringResource(id = R.string.restore_hint)
                    )
                }
            }
        }
    }
}