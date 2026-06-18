package com.aaron.sidegesture.ui.update

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ui.BottomDialog
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.theme.DialogTitleFontSize
import com.aaron.sidegesture.ui.theme.DialogTitlePadding
import com.aaron.sidegesture.ui.theme.ItemPadding

/**
 * 发现新版本底部卡片弹窗。
 *
 * 默认态：标题 + 版本对比 + 更新内容滚动区 + [忽略此版本 / 立即更新]。
 * 下载中态：底部按钮行整体替换为进度条，期间禁止关闭。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/18
 */
@Composable
fun UpdateDialog(
    localVersion: String,
    state: UpdateViewModel.UiState,
    onDismissRequest: () -> Unit,
    onIgnore: () -> Unit,
    onConfirm: () -> Unit,
    onOpenRelease: () -> Unit
) {
    BottomDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = !state.downloading,
            dismissOnClickOutside = !state.downloading
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBackground(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DialogTitlePadding,
                        end = ItemPadding / 2,
                        top = ItemPadding,
                        bottom = 4.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = R.string.update_available_title),
                    fontSize = DialogTitleFontSize,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onOpenRelease) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(id = R.string.update_view_on_github),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                modifier = Modifier.padding(
                    start = DialogTitlePadding,
                    end = DialogTitlePadding,
                    bottom = ItemPadding
                ),
                text = stringResource(
                    id = R.string.update_version_compare,
                    displayVersion(localVersion),
                    displayVersion(state.version)
                ),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            val maxNotesHeight = (LocalConfiguration.current.screenHeightDp * 0.4f).dp
            Text(
                modifier = Modifier
                    .padding(horizontal = ItemPadding)
                    .fillMaxWidth()
                    .clipToBackground(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .heightIn(max = maxNotesHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(ItemPadding),
                text = state.notes.ifBlank { "—" },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )

            if (state.downloading) {
                DownloadingContent(progress = state.progress)
            } else {
                ActionRow(onIgnore = onIgnore, onConfirm = onConfirm)
            }
        }
    }
}

@Composable
private fun ActionRow(
    onIgnore: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ItemPadding, vertical = ItemPadding),
        horizontalArrangement = Arrangement.spacedBy(ItemPadding)
    ) {
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = onIgnore,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(text = stringResource(id = R.string.update_ignore_version))
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = onConfirm
        ) {
            Text(text = stringResource(id = R.string.update_now))
        }
    }
}

@Composable
private fun DownloadingContent(progress: Int) {
    val animatedFraction by animateFloatAsState(
        targetValue = (progress / 100f).coerceIn(0f, 1f),
        label = "downloadProgress"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DialogTitlePadding, vertical = ItemPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.update_downloading_label),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "$progress%",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(ItemPadding / 2))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.height(ItemPadding / 2))
        Text(
            text = stringResource(id = R.string.update_download_hint),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun displayVersion(raw: String): String {
    val trimmed = raw.trim().removePrefix("v").removePrefix("V")
    return "v$trimmed"
}
