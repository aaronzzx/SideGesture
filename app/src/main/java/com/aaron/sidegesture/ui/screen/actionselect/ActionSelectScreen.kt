package com.aaron.sidegesture.ui.screen.actionselect

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalSettings
import com.aaron.sidegesture.constant.TriggerDirection
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.ktx.PERMISSION_GET_INSTALLED_APPS
import com.aaron.sidegesture.ktx.actionIcon
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.deniedForever
import com.aaron.sidegesture.ktx.gotoAppDetailSettings
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiState.SelectItem
import com.aaron.sidegesture.ui.theme.AlipayColor
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinIconSize
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.theme.ScrollBottomPadding
import com.aaron.sidegesture.ui.theme.WechatColor
import com.aaron.sidegesture.ui.widget.TopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */

@Serializable
data class ActionSelect(
    val gestureButtonId: String,
    val position: Int,
    val direction: TriggerDirection,
    val isLongSlide: Boolean
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActionSelectScreen(
    onBack: () -> Unit,
    vm: ActionSelectVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopBar(
                    onBack = onBack,
                    title = uiState.title,
                    actions = {
                        if (!uiState.selectSingle) {
                            IconButton(onClick = { vm.done() }) {
                                Icon(imageVector = Icons.Default.Done, contentDescription = null)
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { contentPadding ->
            Column(modifier = Modifier.padding(contentPadding)) {
                val pagerState = rememberPagerState { PAGES.size }
                val coroutineScope = rememberCoroutineScope()
                TabRow(
                    modifier = Modifier.fillMaxWidth(),
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    divider = { }
                ) {
                    PAGES.fastForEach { tabIndex ->
                        Tab(
                            modifier = Modifier.height(48.dp),
                            selected = tabIndex == pagerState.currentPage,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(tabIndex)
                                }
                            }
                        ) {
                            Text(
                                text = when (tabIndex) {
                                    PAGE_ACTION -> stringResource(id = R.string.tab_action)
                                    PAGE_APPS -> stringResource(id = R.string.tab_apps)
                                    else -> error("Unknown tabIndex: $tabIndex")
                                }
                            )
                        }
                    }
                }

                val permissionState = rememberPermissionState(PERMISSION_GET_INSTALLED_APPS) {
                    vm.updateAppInfos()
                }
                LaunchedEffect(pagerState, permissionState) {
                    var init = true
                    snapshotFlow { pagerState.currentPage }
                        .drop(1)
                        .filter { init && it == PAGE_APPS }
                        .collectLatest {
                            if (uiState.needRequestGetAppPermission) {
                                permissionState.launchPermissionRequest()
                            } else {
                                vm.updateAppInfos()
                            }
                            init = false
                        }
                }
                HorizontalPager(
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState
                ) { page ->
                    when (page) {
                        PAGE_ACTION -> {
                            ActionPage(
                                modifier = Modifier.fillMaxSize(),
                                actions = uiState.actions,
                                selectedItem = uiState.selectedItem,
                                selectSingle = uiState.selectSingle,
                                onSelect = { action, selected ->
                                    vm.select(action, selected)
                                }
                            )
                        }
                        PAGE_APPS -> {
                            AppPage(
                                modifier = Modifier.fillMaxSize(),
                                appInfos = uiState.apps,
                                selectedItem = uiState.selectedItem,
                                snackbarHostState = snackbarHostState,
                                permissionState = permissionState,
                                selectSingle = uiState.selectSingle,
                                needRequestGetAppPermission = uiState.needRequestGetAppPermission,
                                onSelect = { appInfo, selected ->
                                    vm.select(appInfo, selected)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPage(
    onSelect: (String, Boolean) -> Unit,
    actions: List<String>,
    selectedItem: SelectItem,
    selectSingle: Boolean,
    modifier: Modifier = Modifier,
    maxSelectCount: Int = MAX_SELECT_COUNT
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ScrollBottomPadding)
    ) {
        items(
            items = actions,
            key = { it }
        ) { item ->
            ActionItem(
                action = item,
                selected = selectedItem.isSelected(item),
                selectSingle = selectSingle,
                enabled = run {
                    !(selectedItem.size >= maxSelectCount && !selectedItem.isSelected(item))
                },
                onSelect = { selected ->
                    onSelect(item, selected)
                }
            )
        }
    }
}

@Composable
private fun ActionItem(
    onSelect: (Boolean) -> Unit,
    selected: Boolean,
    action: String,
    selectSingle: Boolean,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else GlobalSettings.DisabledAlpha
            }
            .fillMaxWidth()
            .height(MinInteractiveSize)
            .onClick(enabled = enabled) {
                onSelect(!selected)
            }
            .padding(vertical = ContentPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val icon = actionIcon(action)
        Box(
            modifier = Modifier
                .padding(start = ContentPaddingHorizontal * 2)
                .size(MinIconSize)
        ) {
            if (icon is ImageVector) {
                Image(
                    imageVector = icon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            } else {
                AsyncImage(
                    model = icon,
                    contentDescription = null,
                    imageLoader = context.imageLoader,
                    contentScale = ContentScale.Crop,
                    colorFilter = when (icon) {
                        R.drawable.wechat_scan,
                        R.drawable.wechat_paycode -> ColorFilter.tint(WechatColor)
                        R.drawable.alipay_scan,
                        R.drawable.alipay_paycode -> ColorFilter.tint(AlipayColor)
                        else -> null
                    }
                )
            }
        }

        Text(
            modifier = Modifier
                .padding(start = ItemPadding, end = ItemPadding)
                .weight(1f),
            text = actionText(action = action, emptyIfNone = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!selectSingle) {
            Checkbox(
                modifier = Modifier.padding(end = 4.dp),
                enabled = enabled,
                checked = selected,
                onCheckedChange = onSelect
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun AppPage(
    onSelect: (AppInfo, Boolean) -> Unit,
    appInfos: List<AppInfo>,
    selectedItem: SelectItem,
    snackbarHostState: SnackbarHostState,
    permissionState: PermissionState,
    selectSingle: Boolean,
    needRequestGetAppPermission: Boolean,
    modifier: Modifier = Modifier,
    maxSelectCount: Int = MAX_SELECT_COUNT
) {
    Box(modifier = modifier) {
        if (!needRequestGetAppPermission || permissionState.status.isGranted) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = ScrollBottomPadding)
            ) {
                items(
                    items = appInfos,
                    key = { "${it.label}-${it.packageName}" }
                ) { item ->
                    AppItem(
                        appInfo = item,
                        selected = selectedItem.isSelected(item),
                        selectSingle = selectSingle,
                        enabled = run {
                            !(selectedItem.size >= maxSelectCount && !selectedItem.isSelected(item))
                        },
                        onSelect = { selected ->
                            onSelect(item, selected)
                        }
                    )
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

@Composable
private fun AppItem(
    onSelect: (Boolean) -> Unit,
    selected: Boolean,
    appInfo: AppInfo,
    selectSingle: Boolean,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else GlobalSettings.DisabledAlpha
            }
            .fillMaxWidth()
            .onClick(enabled = enabled) {
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
        if (!selectSingle) {
            Checkbox(
                modifier = Modifier.padding(end = 4.dp),
                enabled = enabled,
                checked = selected,
                onCheckedChange = onSelect
            )
        }
    }
}

private const val MAX_SELECT_COUNT = 5

private const val PAGE_ACTION = 0
private const val PAGE_APPS = 1

private val PAGES = listOf(PAGE_ACTION, PAGE_APPS)