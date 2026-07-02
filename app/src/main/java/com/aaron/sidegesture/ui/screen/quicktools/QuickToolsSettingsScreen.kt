package com.aaron.sidegesture.ui.screen.quicktools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.compose.ktx.onSingleClick
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.global.QuickToolItem
import com.aaron.sidegesture.feature.quicktools.quickToolIcon
import com.aaron.sidegesture.feature.quicktools.quickToolText
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.MinItemHeightNoSecondary
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.TopBar
import sh.calvin.reorderable.DragGestureDetector
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QuickToolsSettingsScreen(
    onBack: () -> Unit,
    vm: QuickToolsSettingsVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(R.string.quick_tools_settings)
            )
            MyColumn {
                MySection {
                    MyTextButton(
                        onClick = vm::resetDefault,
                        text = stringResource(R.string.quick_tools_reset_default),
                        secondaryText = stringResource(R.string.quick_tools_reset_default_hint)
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(R.string.quick_tools_reorder_hint)
                ) {
                    val listState = rememberLazyListState()
                    val reorderableState = rememberReorderableLazyListState(
                        lazyListState = listState,
                        onMove = { from, to ->
                            vm.reorder(from.index, to.index)
                        }
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 560.dp),
                        state = listState
                    ) {
                        items(uiState.items, key = { it.type }) { item ->
                            ReorderableItem(state = reorderableState, key = item.type) {
                                QuickToolRow(
                                    item = item,
                                    onEnabledChange = { enabled -> vm.setEnabled(item, enabled) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.QuickToolRow(
    item: QuickToolItem,
    onEnabledChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinItemHeightNoSecondary)
            .draggableHandle(
                dragGestureDetector = DragGestureDetector.LongPress
            )
            .onSingleClick { onEnabledChange(!item.enabled) }
            .padding(
                horizontal = ContentPaddingHorizontal,
                vertical = ContentPaddingVerticalWithSection
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ItemPadding)
    ) {
        Icon(
            imageVector = quickToolIcon(item.type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            modifier = Modifier.weight(1f),
            text = context.quickToolText(item.type),
            style = MaterialTheme.typography.titleMedium
        )
        Icon(
            modifier = Modifier.padding(end = 4.dp),
            imageVector = Icons.Default.DragIndicator,
            contentDescription = stringResource(R.string.quick_tools_reorder_hint),
            tint = MaterialTheme.colorScheme.secondary
        )
        Switch(
            checked = item.enabled,
            onCheckedChange = onEnabledChange
        )
    }
}
