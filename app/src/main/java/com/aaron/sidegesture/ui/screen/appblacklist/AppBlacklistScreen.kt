package com.aaron.sidegesture.ui.screen.appblacklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.PERMISSION_GET_INSTALLED_APPS
import com.aaron.sidegesture.ktx.deniedForever
import com.aaron.sidegesture.ktx.gotoAppDetailSettings
import com.aaron.sidegesture.ktx.icon
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.theme.ScrollBottomPadding
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.TopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/1
 */

@Serializable
data object AppBlacklist

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
                title = stringResource(id = R.string.reset_default_settings),
                text = stringResource(id = R.string.reset_exclude_apps_warning),
                onConfirmClick = { vm.reset() }
            )
        }

        val permissionState = rememberPermissionState(PERMISSION_GET_INSTALLED_APPS) {
            vm.updateAppInfos()
        }
        LaunchedEffect(permissionState) {
            if (uiState.needRequestGetAppPermission) {
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
                    actions = {
                        if (permissionState.status.isGranted) {
                            IconButton(onClick = { vm.showResetWarningDialog(true) }) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "Reset"
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { contentPadding ->
            Box(modifier = Modifier.padding(contentPadding)) {
                if (!uiState.needRequestGetAppPermission || permissionState.status.isGranted) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = ScrollBottomPadding)
                    ) {
                        arrayOf(uiState.selectedAppInfos, uiState.unselectedAppInfos).forEach { list ->
                            items(
                                items = list,
                                key = { "${it.label}-${it.packageName}" }
                            ) { item ->
                                AppBlacklistItem(
                                    appInfo = item,
                                    selected = item.packageName in uiState.excludeApps,
                                    onSelect = { selected ->
                                        vm.selectApp(item.packageName, selected)
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
                                            message = context.getString(R.string.request_get_apps_permission_rationale),
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
            modifier = Modifier.padding(end = 4.dp),
            checked = selected,
            onCheckedChange = onSelect
        )
    }
}