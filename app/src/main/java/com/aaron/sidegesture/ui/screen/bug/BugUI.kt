package com.aaron.sidegesture.ui.screen.bug

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.theme.alpha
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.textStyles
import com.aaron.sidegesture.ui.widget.MyAlertDialog
import com.aaron.sidegesture.ui.widget.MySnackbarHost
import com.aaron.sidegesture.ui.widget.TopBar
import com.aaron.sidegesture.utils.AboutUtils
import com.aaron.sidegesture.utils.CrashHandler
import com.aaron.sidegesture.utils.showToast
import com.aaron.sidegesture.utils.showToastLong
import com.blankj.utilcode.util.FileIOUtils
import kotlinx.coroutines.launch

/**
 * @author DS-Z
 * @since 2025/10/30
 */

@Composable
fun BugScreen(
    onBack: () -> Unit,
    vm: BugVM = viewModel()
) {
    val context = LocalContext.current

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("*/*")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val file = CrashHandler.getCrashFile() ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)!!.use { outputStream ->
            val zipFileBytes = FileIOUtils.readFile2BytesByStream(file)
            outputStream.write(zipFileBytes)
            outputStream.flush()
        }
        AboutUtils.feedbackEmail(context, uri)
    }

    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        if (uiState.showResetWarningDialog) {
            MyAlertDialog(
                onDismissRequest = { vm.showResetWarningDialog(false) },
                onConfirmClick = { vm.reset() },
                title = stringResource(id = R.string.clear_exception_cache),
                text = stringResource(id = R.string.clear_exception_cache_tips)
            )
        }

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        Scaffold(
            topBar = {
                TopBar(
                    onBack = onBack,
                    title = stringResource(id = R.string.bug_collecting),
                    actions = {
                        IconButton(onClick = { vm.showMoreMenu(true) }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(id = R.string.more)
                            )
                        }

                        DropdownMenu(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            offset = DpOffset(
                                -MaterialTheme.dimensions.topBar.popupAnchorOffset,
                                0.dp
                            ),
                            expanded = uiState.showMoreMenu,
                            onDismissRequest = { vm.showMoreMenu(false) }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    vm.showMoreMenu(false)
                                    vm.showResetWarningDialog(true)
                                },
                                text = {
                                    Text(text = stringResource(id = R.string.clear_exception_cache),)
                                }
                            )

                            val actionText = stringResource(R.string.goto_save_crashlog)
                            val actionLabel = stringResource(R.string.goto_save)
                            DropdownMenuItem(
                                onClick = onClick@{
                                    val file = CrashHandler.getCrashFile()
                                    val fileExists = file != null
                                    if (!fileExists) {
                                        vm.showMoreMenu(false)
                                        showToast(R.string.crashlog_not_found)
                                        return@onClick
                                    }
                                    showToastLong(R.string.feedback_to_dev_tips)
                                    vm.showMoreMenu(false) {
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = actionText,
                                                actionLabel = actionLabel,
                                                withDismissAction = true
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                createFileLauncher.launch("crashlog")
                                            }
                                        }
                                    }
                                },
                                text = {
                                    Text(text = stringResource(id = R.string.feedback_to_dev))
                                }
                            )
                        }
                    }
                )
            },
            snackbarHost = {
                MySnackbarHost(hostState = snackbarHostState)
            }
        ) { contentPadding ->
            val navBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val listState = rememberLazyListState()
            LaunchedEffect(listState, uiState.bugs, uiState.selectedBug) {
                val index = uiState.bugs.indexOf(uiState.selectedBug)
                if (index < 0) {
                    return@LaunchedEffect
                }
                listState.animateScrollToItem(index)
            }
            LazyColumn(
                modifier = Modifier
                    .padding(top = contentPadding.calculateTopPadding())
                    .fillMaxSize(),
                state = listState,
                contentPadding = run {
                    val direction = LocalLayoutDirection.current
                    PaddingValues(
                        start = contentPadding.calculateStartPadding(direction),
                        end = contentPadding.calculateEndPadding(direction),
                        bottom = contentPadding.calculateBottomPadding() + navBarsPadding
                    )
                }
            ) {
                itemsIndexed(items = uiState.bugs) { index, item ->
                    val selected = item == uiState.selectedBug
                    Row(
                        modifier = Modifier
                            .background(
                                color = when (selected) {
                                    true -> MaterialTheme.colorScheme.primary.copy(
                                        alpha = MaterialTheme.alpha.gestureAngleGuide
                                    )
                                    else -> Color.Transparent
                                }
                            )
                            .fillParentMaxWidth()
                            .onClick(enableRipple = false) {
                                vm.onClickItem(item)
                            }
                            .padding(MaterialTheme.dimensions.layout.screenPadding),
                        horizontalArrangement = Arrangement.spacedBy(
                            MaterialTheme.dimensions.listItem.iconTextGap
                        )
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "${index + 1}. $item",
                            style = MaterialTheme.textStyles.bugTimestamp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = when (selected) {
                                true -> Int.MAX_VALUE
                                else -> 2
                            }
                        )
                        Icon(
                            modifier = Modifier
                                .graphicsLayer {
                                    rotationZ = when (selected) {
                                        true -> 180f
                                        else -> 0f
                                    }
                                },
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
