package com.aaron.sidegesture.ui.screen.actionselect

import android.app.Activity
import android.content.Intent
import android.content.Intent.ShortcutIconResource
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.LoadingComponent
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.onClick
import com.aaron.compose.ktx.onSingleClick
import com.aaron.compose.ktx.toDp
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.ActionSelect
import com.aaron.sidegesture.entity.AppInfo
import com.aaron.sidegesture.entity.IconResize
import com.aaron.sidegesture.entity.LauncherInfo
import com.aaron.sidegesture.ktx.actionIcon
import com.aaron.sidegesture.ktx.actionText
import com.aaron.sidegesture.ktx.alipayColor
import com.aaron.sidegesture.ktx.deniedForever
import com.aaron.sidegesture.ktx.gotoAppDetailSettings
import com.aaron.sidegesture.ktx.icon
import com.aaron.sidegesture.ktx.qualifiedName
import com.aaron.sidegesture.ktx.rememberGetInstalledAppsPermissionState
import com.aaron.sidegesture.ktx.wechatColor
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiEvent
import com.aaron.sidegesture.ui.screen.actionselect.ActionSelectVM.UiState.SelectedRecord
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVertical
import com.aaron.sidegesture.ui.theme.IconTextPadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinIconSize
import com.aaron.sidegesture.ui.theme.MinInteractiveSize
import com.aaron.sidegesture.ui.theme.RootPadding
import com.aaron.sidegesture.ui.theme.ScrollBottomPadding
import com.aaron.sidegesture.ui.theme.SubMinInteractiveSize
import com.aaron.sidegesture.ui.theme.TopBarPaddingExtra
import com.aaron.sidegesture.ui.widget.ActionSettingsDialog
import com.aaron.sidegesture.ui.widget.MySnackbarHost
import com.aaron.sidegesture.ui.widget.SearchTopBarField
import com.aaron.sidegesture.ui.widget.ShellActionSettingsDialog
import com.aaron.sidegesture.ui.widget.TopBar
import com.aaron.sidegesture.utils.VibrateUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState


/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActionSelectScreen(
    onBack: () -> Unit,
    onNavToIconResize: (IconResize) -> Unit,
    onNavToQuickTools: () -> Unit,
    onNavToQuickLauncher: (ActionSelect) -> Unit = {},
    vm: ActionSelectVM = viewModel()
) {
    UDFComponent(
        component = vm.udfComponent,
        onEvent = { event ->
            when (event) {
                is UiEvent.GotoIconResize -> onNavToIconResize(event.iconResize)
                is UiEvent.GotoQuickLauncherConfig -> onNavToQuickLauncher(event.route)
            }
        }
    ) { uiState ->
        if (uiState.actionSettingsDialog.show) {
            ActionSettingsDialog(
                onDismissRequest = { vm.actionSettingsDialog.show(false) },
                action = uiState.actionSettingsDialog.action
            )
        }
        if (uiState.shellActionDialog.show) {
            ShellActionSettingsDialog(
                onDismissRequest = { vm.shellActionDialog.show(false) },
                value = uiState.shellActionDialog,
                onCommandChange = vm::updateShellCommand,
                onRequestShizukuPermission = vm::requestShizukuPermission,
                onTest = vm::testShellCommand,
                onSave = vm::saveShellAction
            )
        }

        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            topBar = {
                TopBar(
                    onBack = onBack,
                    title = uiState.title,
                    titleContent = {
                        AnimatedContent(
                            targetState = uiState.isSearching,
                            contentAlignment = Alignment.Center
                        ) { searching ->
                            if (searching) {
                                SearchTopBarField(
                                    query = uiState.searchQuery,
                                    onQueryChange = vm::updateSearchQuery,
                                    onClose = vm::hideSearch
                                )
                            } else {
                                Text(
                                    text = uiState.title,
                                    style = TextStyle(fontSize = 18.sp)
                                )
                            }
                        }
                    },
                    actions = {
                        if (!uiState.isSearching) {
                            IconButton(
                                onClick = vm::showSearch
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(id = R.string.search)
                                )
                            }
                        }
                        if (!uiState.selectSingle) {
                            IconButton(onClick = { vm.done() }) {
                                Icon(imageVector = Icons.Default.Done, contentDescription = null)
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                MySnackbarHost(hostState = snackbarHostState)
            }
        ) { contentPadding ->
            Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
                val pages = if (uiState.isQuickLauncher) PAGES_QUICK_LAUNCHER else PAGES
                val pagerState = rememberPagerState { pages.size }
                val coroutineScope = rememberCoroutineScope()
                TabRow(
                    modifier = Modifier.fillMaxWidth(),
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    divider = { }
                ) {
                    pages.fastForEach { tabIndex ->
                        Tab(
                            modifier = Modifier.height(48.dp),
                            selected = tabIndex == pages[pagerState.currentPage],
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pages.indexOf(tabIndex))
                                }
                            }
                        ) {
                            Text(
                                text = when (tabIndex) {
                                    PAGE_ACTION -> stringResource(id = R.string.tab_action)
                                    PAGE_APPS -> stringResource(id = R.string.tab_apps)
                                    PAGE_SHORTCUTS -> stringResource(id = R.string.tab_shortcuts)
                                    else -> error("Unknown tabIndex: $tabIndex")
                                }
                            )
                        }
                    }
                }

                val permissionState = rememberGetInstalledAppsPermissionState { granted ->
                    if (granted) {
                        vm.updateAppInfos()
                        vm.updateShortcutInfos()
                    }
                }
                LaunchedEffect(vm, pagerState, permissionState, pages) {
                    if (uiState.isQuickLauncher) {
                        if (!permissionState.status.isGranted) {
                            permissionState.launchPermissionRequest()
                        } else {
                            vm.updateAppInfos()
                        }
                    }
                    launch {
                        var init = true
                        snapshotFlow { pagerState.currentPage }
                            .drop(1)
                            .filter { init && pages[it] == PAGE_APPS }
                            .collectLatest {
                                if (!permissionState.status.isGranted) {
                                    permissionState.launchPermissionRequest()
                                } else {
                                    vm.updateAppInfos()
                                }
                                init = false
                            }
                    }
                    launch {
                        var init = true
                        snapshotFlow { pagerState.currentPage }
                            .drop(1)
                            .filter { init && pages[it] == PAGE_SHORTCUTS }
                            .collectLatest {
                                if (!permissionState.status.isGranted) {
                                    permissionState.launchPermissionRequest()
                                } else {
                                    vm.updateShortcutInfos()
                                }
                                init = false
                            }
                    }
                }
                HorizontalPager(
                    modifier = Modifier
                        .imePadding()
                        .fillMaxWidth()
                        .weight(1f),
                    state = pagerState
                ) { page ->
                    when (pages[page]) {
                        PAGE_ACTION -> {
                            ActionPage(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = run {
                                    val direction = LocalLayoutDirection.current
                                    PaddingValues(
                                        start = contentPadding.calculateStartPadding(direction),
                                        end = contentPadding.calculateEndPadding(direction),
                                        bottom = contentPadding.calculateBottomPadding() + ScrollBottomPadding
                                    )
                                },
                                actions = uiState.actions,
                                selectedRecord = uiState.selectedRecord,
                                selectSingle = uiState.selectSingle,
                                onSelect = { action, selected ->
                                    vm.select(action, selected)
                                },
                                onSettingsClick = { action ->
                                    when (action.value) {
                                        GlobalActions.QUICK_TOOLS -> onNavToQuickTools()
                                        GlobalActions.QUICK_LAUNCHER -> {
                                            onNavToQuickLauncher(vm.getQuickLauncherRoute())
                                        }
                                        GlobalActions.SHIZUKU_SHELL -> {
                                            vm.shellActionDialog.show(true, action)
                                        }
                                        else -> {
                                            vm.actionSettingsDialog.show(true, action)
                                        }
                                    }
                                }
                            )
                        }
                        PAGE_APPS -> {
                            LoadingComponent(
                                modifier = Modifier.fillMaxSize(),
                                component = vm.loadingComponent
                            ) {
                                AppPage(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = run {
                                        val direction = LocalLayoutDirection.current
                                        PaddingValues(
                                            start = contentPadding.calculateStartPadding(direction),
                                            end = contentPadding.calculateEndPadding(direction),
                                            bottom = contentPadding.calculateBottomPadding() + ScrollBottomPadding
                                        )
                                    },
                                    onSelect = { appInfo, selected ->
                                        vm.select(appInfo, selected)
                                    },
                                    onLongClick = { appInfo ->
                                        vm.toggleMiniWindow(appInfo)
                                    },
                                    appInfos = uiState.apps,
                                    selectedRecord = uiState.selectedRecord,
                                    snackbarHostState = snackbarHostState,
                                    permissionState = permissionState,
                                    selectSingle = uiState.selectSingle
                                )
                            }
                        }
                        PAGE_SHORTCUTS -> {
                            LoadingComponent(
                                modifier = Modifier.fillMaxSize(),
                                component = vm.loadingComponent
                            ) {
                                val context = LocalContext.current
                                var currentLauncherInfo: LauncherInfo? by remember {
                                    mutableStateOf(null)
                                }
                                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                                    val launcherInfo = currentLauncherInfo
                                    if (result.resultCode == Activity.RESULT_OK && launcherInfo != null) {
                                        val bitmap = result.data?.getParcelableExtra<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)
                                        val shortcutIconRes = result.data?.getParcelableExtra<ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)
                                        val intent = result.data?.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)?.toUri(Intent.URI_INTENT_SCHEME)
                                        val label = result.data?.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: ""
                                        var iconRes = 0
                                        if (shortcutIconRes != null) {
                                            val res = context.packageManager.getResourcesForApplication(shortcutIconRes.packageName)
                                            iconRes = res.getIdentifier(shortcutIconRes.resourceName, null, null)
                                        }
                                        val shortcutInfo = LauncherInfo.ShortcutInfo(
                                            packageName = launcherInfo.packageName,
                                            className = launcherInfo.className,
                                            intents = intent?.let { listOf(it) } ?: emptyList(),
                                            label = label,
                                            iconRes = iconRes,
                                            iconPath = null,
                                            iconBitmap = bitmap
                                        )
                                        vm.addNewShortcut(launcherInfo, shortcutInfo)
                                        if (uiState.selectedRecord.size < MAX_SELECT_COUNT) {
                                            vm.select(shortcutInfo, true)
                                        }
                                    }
                                    currentLauncherInfo = null
                                }
                                ShortcutPage(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = run {
                                        val direction = LocalLayoutDirection.current
                                        PaddingValues(
                                            start = contentPadding.calculateStartPadding(direction),
                                            end = contentPadding.calculateEndPadding(direction),
                                            bottom = contentPadding.calculateBottomPadding() + ScrollBottomPadding
                                        )
                                    },
                                    onSelect = { shortcutInfo, selected ->
                                        vm.select(shortcutInfo, selected)
                                    },
                                    onLongClick = { shortcutInfo ->
                                        vm.toggleMiniWindow(shortcutInfo)
                                    },
                                    onClick = { launcherInfo ->
                                        try {
                                            currentLauncherInfo = launcherInfo
                                            val intent = Intent().apply {
                                                setClassName(
                                                    launcherInfo.packageName,
                                                    launcherInfo.className
                                                )
                                            }
                                            launcher.launch(intent)
                                        } catch (ignored: Exception) {
                                            currentLauncherInfo = null
                                        }
                                    },
                                    createShortcuts = uiState.createShortcuts,
                                    launchShortcuts = uiState.launchShortcuts,
                                    selectedRecord = uiState.selectedRecord,
                                    snackbarHostState = snackbarHostState,
                                    permissionState = permissionState,
                                    selectSingle = uiState.selectSingle
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = !uiState.selectSingle &&
                            uiState.selectedRecord.size > 0 &&
                            !uiState.isSearching
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    SelectedList(
                        onClear = {
                            vm.clearSelected()
                        },
                        onUnselected = {
                            vm.select(it, false)
                        },
                        onReordered = { newList ->
                            vm.reorder(newList)
                        },
                        onExpandedChange = { expanded = it },
                        list = uiState.selectedRecord.list,
                        expanded = expanded
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPage(
    onSettingsClick: (Action) -> Unit,
    onSelect: (Action, Boolean) -> Unit,
    actions: List<Action>,
    selectedRecord: SelectedRecord,
    selectSingle: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    maxSelectCount: Int = MAX_SELECT_COUNT
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(
            items = actions,
            key = { it.value }
        ) { item ->
            ActionItem(
                action = item,
                selected = selectedRecord.isSelected(item),
                selectSingle = selectSingle,
                enabled = canActionEnabled(selectedRecord, item, maxSelectCount),
                onSelect = { selected ->
                    val isShellAction = item.value == GlobalActions.SHIZUKU_SHELL
                    val isSelected = selectedRecord.isSelected(item)
                    when {
                        item.value == GlobalActions.QUICK_LAUNCHER -> onSettingsClick(item)
                        isShellAction && (selectSingle || !isSelected) -> onSettingsClick(item)
                        else -> onSelect(item, selected)
                    }
                },
                showSettings = item.value == GlobalActions.MOVE_SCREEN ||
                    item.value == GlobalActions.PREVIOUS_APP ||
                    item.value == GlobalActions.GOTO_BOTTOM ||
                    item.value == GlobalActions.POPUP_SCREEN ||
                    item.value == GlobalActions.SHIZUKU_SHELL,
                onSettingsClick = {
                    onSettingsClick(item)
                }
            )
        }
    }
}

@Composable
private fun ActionItem(
    onSelect: (Boolean) -> Unit,
    selected: Boolean,
    action: Action,
    selectSingle: Boolean,
    enabled: Boolean = true,
    showSettings: Boolean = false,
    onSettingsClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else GlobalSettings.DisabledAlpha
            }
            .fillMaxWidth()
            .heightIn(min = MinInteractiveSize)
            .onClick(enabled = enabled) {
                onSelect(!selected)
            }
            .padding(vertical = 2.dp),
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
                        R.drawable.wechat_paycode -> ColorFilter.tint(MaterialTheme.colorScheme.wechatColor)
                        R.drawable.alipay_scan,
                        R.drawable.alipay_paycode -> ColorFilter.tint(MaterialTheme.colorScheme.alipayColor)
                        else -> null
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = ItemPadding)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ItemPadding)
        ) {
            Text(
                modifier = Modifier
                    .weight(1f, false)
                    .basicMarquee(velocity = 50.dp),
                text = actionText(action = action, emptyIfNone = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showSettings) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clipToBackground(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .onSingleClick(enabled = enabled) {
                            onSettingsClick?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (!selectSingle) {
            Checkbox(
                modifier = Modifier.padding(end = TopBarPaddingExtra),
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
    onLongClick: (AppInfo) -> Unit,
    onSelect: (AppInfo, Boolean) -> Unit,
    appInfos: List<AppInfo>,
    selectedRecord: SelectedRecord,
    snackbarHostState: SnackbarHostState,
    permissionState: PermissionState,
    selectSingle: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    maxSelectCount: Int = MAX_SELECT_COUNT
) {
    Box(modifier = modifier) {
        if (permissionState.status.isGranted) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                items(
                    items = appInfos,
                    key = { it.qualifiedName }
                ) { item ->
                    AppItem(
                        appInfo = item,
                        selected = selectedRecord.isSelected(item),
                        selectSingle = selectSingle,
                        enabled = canAppInfoEnabled(selectedRecord, item, maxSelectCount),
                        onSelect = { selected ->
                            onSelect(item, selected)
                        },
                        onLongClick = {
                            onLongClick(item)
                        }
                    )
                }
            }
        } else {
            PermissionPage(
                snackbarHostState = snackbarHostState,
                permissionState = permissionState
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ShortcutPage(
    onClick: (LauncherInfo) -> Unit,
    onLongClick: (LauncherInfo.ShortcutInfo) -> Unit,
    onSelect: (LauncherInfo.ShortcutInfo, Boolean) -> Unit,
    createShortcuts: List<LauncherInfo>,
    launchShortcuts: List<LauncherInfo>,
    selectedRecord: SelectedRecord,
    snackbarHostState: SnackbarHostState,
    permissionState: PermissionState,
    selectSingle: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    maxSelectCount: Int = MAX_SELECT_COUNT
) {
    Box(modifier = modifier) {
        if (permissionState.status.isGranted) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                if (createShortcuts.isNotEmpty()) {
                    stickyHeader {
                        Text(
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.background)
                                .fillMaxWidth()
                                .padding(vertical = ContentPaddingVertical)
                                .padding(horizontal = ContentPaddingHorizontal * 2),
                            text = stringResource(R.string.create_shortcut),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(
                    items = createShortcuts,
                    key = { it.qualifiedName }
                ) { item ->
                    LauncherInfoItem(
                        launcherInfo = item,
                        selectSingle = selectSingle,
                        canLauncherInfoEnabled = { canLauncherInfoEnabled(selectedRecord, it, maxSelectCount) },
                        canShortcutInfoEnabled = { canShortcutInfoEnabled(selectedRecord, it, maxSelectCount) },
                        isShortcutInfoSelected = { shortcutInfo ->
                            selectedRecord.isSelected(shortcutInfo)
                        },
                        onLongClick = { shortcutInfo ->
                            onLongClick(shortcutInfo)
                        },
                        onSelect = { shortcutInfo, selected ->
                            onSelect(shortcutInfo, selected)
                        },
                        onClick = {
                            onClick(item)
                        }
                    )
                }
                if (launchShortcuts.isNotEmpty()) {
                    stickyHeader {
                        Text(
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.background)
                                .fillMaxWidth()
                                .padding(vertical = ContentPaddingVertical)
                                .padding(horizontal = ContentPaddingHorizontal * 2),
                            text = stringResource(R.string.launch_shortcut),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(
                    items = launchShortcuts,
                    key = { it.qualifiedName }
                ) { item ->
                    LauncherInfoItem(
                        launcherInfo = item,
                        selectSingle = selectSingle,
                        canLauncherInfoEnabled = { canLauncherInfoEnabled(selectedRecord, it, maxSelectCount) },
                        canShortcutInfoEnabled = { canShortcutInfoEnabled(selectedRecord, it, maxSelectCount) },
                        isShortcutInfoSelected = { shortcutInfo ->
                            selectedRecord.isSelected(shortcutInfo)
                        },
                        onLongClick = { shortcutInfo ->
                            onLongClick(shortcutInfo)
                        },
                        onSelect = { shortcutInfo, selected ->
                            onSelect(shortcutInfo, selected)
                        },
                        onClick = {
                        }
                    )
                }
            }
        } else {
            PermissionPage(
                snackbarHostState = snackbarHostState,
                permissionState = permissionState
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionPage(
    snackbarHostState: SnackbarHostState,
    permissionState: PermissionState
) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
    onLongClick: () -> Unit,
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
            .combinedClickable(
                enabled = enabled,
                onLongClick = onLongClick,
                onClick = {
                    onSelect(!selected)
                }
            )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (appInfo.miniWindow) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Default.BrandingWatermark,
                        contentDescription = null
                    )
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = appInfo.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                modifier = Modifier.padding(end = TopBarPaddingExtra),
                enabled = enabled,
                checked = selected,
                onCheckedChange = onSelect
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherInfoItem(
    canLauncherInfoEnabled: (LauncherInfo) -> Boolean,
    canShortcutInfoEnabled: (LauncherInfo.ShortcutInfo) -> Boolean,
    isShortcutInfoSelected: (LauncherInfo.ShortcutInfo) -> Boolean,
    onClick: () -> Unit,
    onLongClick: (LauncherInfo.ShortcutInfo) -> Unit,
    onSelect: (LauncherInfo.ShortcutInfo, Boolean) -> Unit,
    launcherInfo: LauncherInfo,
    selectSingle: Boolean
) {
    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha =
                    if (canLauncherInfoEnabled(launcherInfo)) 1f else GlobalSettings.DisabledAlpha
            }
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onClick(enabled = canLauncherInfoEnabled(launcherInfo)) {
                    onClick()
                }
                .padding(vertical = ContentPaddingVertical),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            AsyncImage(
                modifier = Modifier
                    .padding(start = ContentPaddingHorizontal * 2)
                    .size(MinInteractiveSize),
                model = launcherInfo.icon,
                contentDescription = null,
                imageLoader = context.imageLoader
            )
            Column(
                modifier = Modifier
                    .padding(start = IconTextPadding, end = ItemPadding)
                    .weight(1f)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = launcherInfo.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = launcherInfo.packageName,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Column {
            launcherInfo.shortcuts.fastForEach { shortcutInfo ->
                key(shortcutInfo) {
                    val selected = isShortcutInfoSelected(shortcutInfo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                enabled = canShortcutInfoEnabled(shortcutInfo),
                                onLongClick = {
                                    onLongClick(shortcutInfo)
                                },
                                onClick = {
                                    onSelect(shortcutInfo, !selected)
                                }
                            )
                        /*.padding(start = ContentPaddingHorizontal * 2 + MinInteractiveSize)*/,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val context = LocalContext.current
                        AsyncImage(
                            modifier = Modifier
                                .padding(start = ContentPaddingHorizontal * 3)
                                .size(SubMinInteractiveSize),
                            model = shortcutInfo.icon,
                            contentDescription = null,
                            imageLoader = context.imageLoader
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = IconTextPadding, end = ItemPadding)
                                .weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (shortcutInfo.miniWindow) {
                                    Icon(
                                        modifier = Modifier.size(16.dp),
                                        imageVector = Icons.Default.BrandingWatermark,
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = shortcutInfo.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (!selectSingle) {
                            Checkbox(
                                modifier = Modifier.padding(end = TopBarPaddingExtra),
                                enabled = canShortcutInfoEnabled(shortcutInfo),
                                checked = selected,
                                onCheckedChange = { newSelected ->
                                    onSelect(shortcutInfo, newSelected)
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
private fun SelectedList(
    onClear: () -> Unit,
    onUnselected: (Any) -> Unit,
    onReordered: (newList: List<Any>) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    list: List<Any>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    expanded: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(RootPadding),
    itemPadding: Dp = RootPadding
) {
    val context = LocalContext.current
    val itemSize = MinInteractiveSize
    val itemComposable: @Composable ReorderableCollectionItemScope.(Any, Any) -> Unit = { reorderableState, item ->
        val listItem = when (item) {
            is Action -> {
                val actionIcon = actionIcon(item)
                val colorFilter = when (actionIcon) {
                    R.drawable.wechat_scan,
                    R.drawable.wechat_paycode -> ColorFilter.tint(MaterialTheme.colorScheme.wechatColor)
                    R.drawable.alipay_scan,
                    R.drawable.alipay_paycode -> ColorFilter.tint(MaterialTheme.colorScheme.alipayColor)
                    else -> null
                }
                SelectedListItem(actionIcon, colorFilter)
            }
            is AppInfo, is LauncherInfo.ShortcutInfo -> {
                val icon = when (item) {
                    is AppInfo -> item.icon
                    is LauncherInfo.ShortcutInfo -> item.icon
                    else -> null
                }
                val colorFilter = when (icon == null) {
                    true -> ColorFilter.tint(MaterialTheme.colorScheme.error)
                    else -> null
                }
                SelectedListItem(icon ?: Icons.Default.Error, colorFilter)
            }
            else -> throw IllegalArgumentException("Unknown item type: $item")
        }
        Box(
            modifier = Modifier
                .size(itemSize)
                .draggableHandle(
                    dragGestureDetector = DragGestureDetector.LongPress,
                    onDragStarted = {
                        VibrateUtils.vibrate(context)
                    }
                )
                .clipToBackground(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(RootPadding)
                )
                .onSingleClick {
                    onUnselected(item)
                }
                .let {
                    if (item !is Action) it else {
                        it.padding(12.dp)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (listItem.icon is ImageVector) {
                Image(
                    imageVector = listItem.icon,
                    contentDescription = null,
                    colorFilter = when (listItem.colorFilter != null) {
                        true -> listItem.colorFilter
                        else -> ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    }
                )
            } else {
                AsyncImage(
                    model = listItem.icon,
                    contentDescription = null,
                    imageLoader = context.imageLoader,
                    contentScale = ContentScale.Crop,
                    colorFilter = listItem.colorFilter
                )
            }
        }
    }
    Column(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(RootPadding)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onClick {
                    onExpandedChange(!expanded)
                }
                .padding(start = RootPadding, end = RootPadding + 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.drag_icon_to_reorder_click_to_unselect),
                style = MaterialTheme.typography.labelMedium,
                color = contentColorFor(backgroundColor).copy(alpha = 0.5f)
            )

            IconButton(
                onClick = onClear
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            }

            val iconRotation = animateFloatAsState(
                targetValue = when (expanded) {
                    true -> -180f
                    else -> 0f
                }
            )
            Icon(
                modifier = Modifier
                    .size(MinIconSize)
                    .graphicsLayer {
                        rotationZ = iconRotation.value
                    },
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        val navBarsHeight = WindowInsets.navigationBars.getBottom(LocalDensity.current).toDp()
        val layoutDirection = LocalLayoutDirection.current
        AnimatedContent(targetState = expanded) { expandedTargetState ->
            if (expandedTargetState) {
                val listState = rememberLazyGridState()
                val state = rememberReorderableLazyGridState(
                    lazyGridState = listState,
                    onMove = { from, to ->
                        val newList = list.toMutableList().apply {
                            val fromObj = get(from.index)
                            val toObj = get(to.index)
                            set(from.index, toObj)
                            set(to.index, fromObj)
                        }
                        onReordered(newList)
                    }
                )
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    columns = GridCells.Adaptive(itemSize),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        top = contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding() + navBarsHeight
                    ),
                    verticalArrangement = Arrangement.spacedBy(itemPadding),
                    horizontalArrangement = Arrangement.spacedBy(itemPadding)
                ) {
                    items(items = list, key = { it }) { item ->
                        ReorderableItem(state = state, key = item) {
                            itemComposable(state, item)
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()
                val state = rememberReorderableLazyListState(
                    lazyListState = listState,
                    onMove = { from, to ->
                        val newList = list.toMutableList().apply {
                            val fromObj = get(from.index)
                            val toObj = get(to.index)
                            set(from.index, toObj)
                            set(to.index, fromObj)
                        }
                        onReordered(newList)
                    }
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        top = contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding() + navBarsHeight
                    ),
                    horizontalArrangement = Arrangement.spacedBy(itemPadding)
                ) {
                    items(items = list, key = { it }) { item ->
                        ReorderableItem(state = state, key = item) {
                            itemComposable(state, item)
                        }
                    }
                }
            }
        }
    }
}

private fun canActionEnabled(
    selectedRecord: SelectedRecord,
    item: Action,
    maxSelectCount: Int
): Boolean {
    val isMoveScreenSelected = selectedRecord.list.find {
        (it as? Action)?.value == GlobalActions.MOVE_SCREEN
    } != null
    val isMoveScreenAction = item.value == GlobalActions.MOVE_SCREEN
    if (isMoveScreenSelected && !isMoveScreenAction) {
        return false
    } else if (!isMoveScreenSelected &&
        selectedRecord.list.isNotEmpty() &&
        isMoveScreenAction
    ) {
        return false
    }

    // 走完移动屏幕的检查后再走其他
    return !(selectedRecord.size >= maxSelectCount && !selectedRecord.isSelected(item))
}

private fun canAppInfoEnabled(
    selectedRecord: SelectedRecord,
    item: AppInfo,
    maxSelectCount: Int
): Boolean {
    val isMoveScreenSelected = selectedRecord.list.find {
        (it as? Action)?.value == GlobalActions.MOVE_SCREEN
    } != null
    if (isMoveScreenSelected) {
        return false
    }

    // 走完移动屏幕的检查后再走其他
    return !(selectedRecord.size >= maxSelectCount && !selectedRecord.isSelected(item))
}

private fun canLauncherInfoEnabled(
    selectedRecord: SelectedRecord,
    item: LauncherInfo,
    maxSelectCount: Int
): Boolean {
    val isMoveScreenSelected = selectedRecord.list.find {
        (it as? Action)?.value == GlobalActions.MOVE_SCREEN
    } != null
    if (isMoveScreenSelected) {
        return false
    }

    // 走完移动屏幕的检查后再走其他
    return !(selectedRecord.size >= maxSelectCount && !item.shortcuts.any { selectedRecord.isSelected(it) })
}

private fun canShortcutInfoEnabled(
    selectedRecord: SelectedRecord,
    item: LauncherInfo.ShortcutInfo,
    maxSelectCount: Int
): Boolean {
    val isMoveScreenSelected = selectedRecord.list.find {
        (it as? Action)?.value == GlobalActions.MOVE_SCREEN
    } != null
    if (isMoveScreenSelected) {
        return false
    }

    // 走完移动屏幕的检查后再走其他
    return !(selectedRecord.size >= maxSelectCount && !selectedRecord.isSelected(item))
}

private const val MAX_SELECT_COUNT = Int.MAX_VALUE

private const val PAGE_ACTION = 0
private const val PAGE_APPS = 1
private const val PAGE_SHORTCUTS = 2

private val PAGES = listOf(PAGE_ACTION, PAGE_APPS, PAGE_SHORTCUTS)

private val PAGES_QUICK_LAUNCHER = listOf(PAGE_APPS, PAGE_SHORTCUTS)

private data class SelectedListItem(
    val icon: Any?,
    val colorFilter: ColorFilter? = null
)
