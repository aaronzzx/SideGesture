package com.aaron.sidegesture.ui.screen.appblacklist

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.LoadingComponent
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.deniedForever
import com.aaron.sidegesture.ktx.gotoAppDetailSettings
import com.aaron.sidegesture.ktx.icon
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.ktx.rememberGetInstalledAppsPermissionState
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.theme.ScrollBottomPadding
import com.aaron.sidegesture.ui.theme.TopBarPaddingExtra
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.MySnackbarHost
import com.aaron.sidegesture.ui.widget.SearchTopBarField
import com.aaron.sidegesture.ui.widget.TopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/1
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppBlacklistScreen(
    onBack: () -> Unit,
    vm: AppBlacklistVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = { }) { uiState ->
        if (uiState.showResetWarningDialog) {
            MyAlertDialog(
                onDismissRequest = {
                    vm.showResetWarningDialog(false)
                },
                title = stringResource(id = R.string.reset_default_settings_warning),
                text = stringResource(id = R.string.reset_exclude_apps_warning_desc),
                onConfirmClick = { vm.reset() }
            )
        }

        val permissionState = rememberGetInstalledAppsPermissionState { granted ->
            if (granted) {
                vm.updateAppInfos()
            }
        }
        LaunchedEffect(vm, permissionState) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            } else {
                vm.updateAppInfos()
            }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopBar(
                    onBack = onBack,
                    title = stringResource(id = R.string.exclude_app),
                    titleContent = {
                        AnimatedContent(
                            targetState = uiState.isSearching,
                            contentAlignment = Alignment.Center
                        ) { searching ->
                            if (searching) {
                                SearchTopBarField(
                                    query = uiState.appList.searchQuery,
                                    onQueryChange = vm::updateSearchQuery,
                                    onClose = vm::hideSearch,
                                    delayOnClose = false
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.exclude_app),
                                    style = TextStyle(fontSize = 18.sp)
                                )
                            }
                        }
                    },
                    actions = {
                        if (permissionState.status.isGranted) {
                            if (!uiState.isSearching) {
                                IconButton(
                                    onClick = vm::showSearch,
                                    enabled = uiState.appList.savedSelectionLoaded
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(id = R.string.search)
                                    )
                                }
                            }
                            if (!uiState.isSearching) {
                                IconButton(
                                    onClick = { vm.showResetWarningDialog(true) },
                                    enabled = uiState.appList.savedSelectionLoaded
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = stringResource(id = R.string.reset_settings)
                                    )
                                }
                            }
                            IconButton(
                                onClick = vm::done,
                                enabled = uiState.appList.savedSelectionLoaded
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = stringResource(id = R.string.done)
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                MySnackbarHost(hostState = snackbarHostState)
            }
        ) { contentPadding ->
            Box(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
                if (permissionState.status.isGranted) {
                    LoadingComponent(
                        modifier = Modifier.fillMaxSize(),
                        component = vm.loadingComponent
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = run {
                                val direction = LocalLayoutDirection.current
                                PaddingValues(
                                    start = contentPadding.calculateStartPadding(direction),
                                    end = contentPadding.calculateEndPadding(direction),
                                    bottom = contentPadding.calculateBottomPadding() + ScrollBottomPadding
                                )
                            }
                        ) {
                            items(
                                items = uiState.appList.visibleAppInfos,
                                key = { it.qualifiedName }
                            ) { item ->
                                AppBlacklistItem(
                                    appInfo = item,
                                    selected = uiState.appList.isSelected(item.packageName),
                                    onSelect = { selected ->
                                        vm.selectApp(item, selected)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val context = LocalContext.current
                        val coroutineScope = rememberCoroutineScope()
                        TextButton(
                            onClick = {
                                if (permissionState.status.deniedForever) {
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.goto_grant_get_apps_permission),
                                            actionLabel = context.getString(R.string.goto_enable_settings),
                                            withDismissAction = true
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            context.gotoAppDetailSettings()
                                        }
                                    }
                                } else {
                                    permissionState.launchPermissionRequest()
                                }
                            }
                        ) {
                            Text(text = stringResource(id = R.string.request_get_apps_permission))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBlacklistItem(
    onSelect: (Boolean) -> Unit,
    selected: Boolean,
    appInfo: AppInfo
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onClick {
                onSelect(!selected)
            }
            .padding(vertical = ContentPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        AsyncImage(
            modifier = Modifier
                .padding(start = ContentPaddingHorizontal * 2)
                .size(MinInteractiveSize),
            model = appInfo.icon,
            contentDescription = null,
            imageLoader = context.imageLoader,
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(start = IconTextPadding, end = ItemPadding)
                .weight(1f)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = appInfo.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = appInfo.packageName,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Checkbox(
            modifier = Modifier.padding(end = TopBarPaddingExtra),
            checked = selected,
            onCheckedChange = onSelect
        )
    }
}
