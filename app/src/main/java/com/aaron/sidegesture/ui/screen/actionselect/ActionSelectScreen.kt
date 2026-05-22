package com.aaron.sidegesture.ui.screen.actionselect

import android.app.Activity
import android.content.Intent
import android.content.Intent.ShortcutIconResource
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Window
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.LoadingComponent
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ktx.onClick
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.constant.GlobalSettings
import com.aaron.sidegesture.entity.Action
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
import com.aaron.sidegesture.ui.theme.ScrollBottomPadding
import com.aaron.sidegesture.ui.theme.SubMinInteractiveSize
import com.aaron.sidegesture.ui.theme.TopBarPaddingExtra
import com.aaron.sidegesture.ui.widget.ActionSettingsDialog
import com.aaron.sidegesture.ui.widget.MySnackbarHost
import com.aaron.sidegesture.ui.widget.TopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch


/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/2
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActionSelectScreen(
    onBack: () -> Unit,
    onNavToIconResize: (IconResize) -> Unit,
    vm: ActionSelectVM = viewModel()
) {
    UDFComponent(
        component = vm.udfComponent,
        onEvent = { event ->
            when (event) {
                is UiEvent.GotoIconResize -> onNavToIconResize(event.iconResize)
            }
        }
    ) { uiState ->
        if (uiState.actionSettingsDialog.show) {
            ActionSettingsDialog(
                onDismissRequest = { vm.actionSettingsDialog.show(false) },
                action = uiState.actionSettingsDialog.action
            )
        }

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
                MySnackbarHost(hostState = snackbarHostState)
            }
        ) { contentPadding ->
            Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
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
                LaunchedEffect(vm, pagerState, permissionState) {
                    launch {
                        var init = true
                        snapshotFlow { pagerState.currentPage }
                            .drop(1)
                            .filter { init && it == PAGE_APPS }
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
                            .filter { init && it == PAGE_SHORTCUTS }
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
                    modifier = Modifier.fillMaxSize(),
                    state = pagerState
                ) { page ->
                    when (page) {
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
                                    vm.actionSettingsDialog.show(true, action)
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
                        /*PAGE_SHORTCUTS -> {
                            LoadingComponent(
                                modifier = Modifier.fillMaxSize(),
                                component = vm.loadingComponent
                            ) {
                                val context = LocalContext.current
                                val activities = remember(context) {
                                    AppInfoUtils.queryCreateShortcutActivities(context)
                                }
                                val shortcuts = remember(context) {
                                    ShortcutUtils.getAllAppsWithShortcut(context)
                                }
                                var currentLauncherInfo: LauncherInfo? by remember {
                                    mutableStateOf(null)
                                }
                                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                                    val launcherInfo = currentLauncherInfo
                                    if (launcherInfo != null) {
                                        val intent = it.data?.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)?.toUri(Intent.URI_INTENT_SCHEME)
                                        val shortcutInfo = LauncherInfo.ShortcutInfo(
                                            packageName = launcherInfo.packageName,
                                            className = launcherInfo.className,
                                            label = it.data?.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "",
                                            iconRes = 0,
                                            intents = intent?.let { listOf(it) } ?: emptyList()
                                        )
                                        vm.addNewShortcut(launcherInfo, shortcutInfo)
                                        currentLauncherInfo = null
                                    }
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stickyHeader {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(color = MaterialTheme.colorScheme.primary)
                                                .padding(16.dp),
                                            text = "Create Shortcuts",
                                            color = Color.White
                                        )
                                    }
                                    items(items = activities) { launcherInfo ->
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onSingleClick {
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
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    modifier = Modifier.size(40.dp),
                                                    model = launcherInfo.icon,
                                                    contentDescription = null
                                                )
                                                Text(text = launcherInfo.label)
                                            }

                                            launcherInfo.shortcuts.fastForEach { shortcutInfo ->
                                                Row(
                                                    modifier = Modifier
                                                        .padding(start = 16.dp)
                                                        .fillMaxWidth()
                                                        .onSingleClick {
                                                        },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        modifier = Modifier.size(40.dp),
                                                        model = shortcutInfo.getIcon(context),
                                                        contentDescription = null
                                                    )
                                                    Text(text = shortcutInfo.label)
                                                }
                                            }
                                        }
                                    }
                                    stickyHeader {
                                        Text(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(color = MaterialTheme.colorScheme.primary)
                                                .padding(16.dp),
                                            text = "Go to Shortcuts",
                                            color = Color.White
                                        )
                                    }
                                    items(items = shortcuts) { item ->
                                        Column {
                                            Text(text = item.label)
                                            Text(text = item.packageName)
                                            item.shortcuts.fastForEach { shortcut ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .onSingleClick {
                                                            try {
                                                                val intents = shortcut
                                                                    .intents
                                                                    .map {
                                                                        Intent.parseUri(
                                                                            it,
                                                                            Intent.URI_INTENT_SCHEME
                                                                        )
                                                                    }
                                                                    .toTypedArray()
                                                                context.startActivities(intents)
                                                            } catch (ex: Exception) {
                                                                ex.printStackTrace()
                                                            }
                                                        },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val icon = item.icon
                                                    AsyncImage(
                                                        modifier = Modifier.size(40.dp),
                                                        model = icon,
                                                        contentDescription = null
                                                    )
                                                    Column {
                                                        Text(text = "package: ${shortcut.packageName}")
                                                        Text(text = "label: ${shortcut.label}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }*/
                    }
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
                    onSelect(item, selected)
                },
                showSettings = item.value == GlobalActions.MOVE_SCREEN ||
                    item.value == GlobalActions.PREVIOUS_APP ||
                    item.value == GlobalActions.GOTO_BOTTOM,
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
                        imageVector = Icons.Default.Window,
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

@Composable
private fun LauncherInfoItem(
    canLauncherInfoEnabled: (LauncherInfo) -> Boolean,
    canShortcutInfoEnabled: (LauncherInfo.ShortcutInfo) -> Boolean,
    isShortcutInfoSelected: (LauncherInfo.ShortcutInfo) -> Boolean,
    onClick: () -> Unit,
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
                            .onClick(enabled = canShortcutInfoEnabled(shortcutInfo)) {
                                onSelect(shortcutInfo, !selected)
                            }
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
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = shortcutInfo.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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