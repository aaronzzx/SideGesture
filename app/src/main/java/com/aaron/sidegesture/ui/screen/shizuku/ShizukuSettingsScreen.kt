package com.aaron.sidegesture.ui.screen.shizuku

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ktx.launchPackage
import com.aaron.sidegesture.ktx.shizukuStatusLabel
import com.aaron.sidegesture.ktx.shizukuStatusSummary
import com.aaron.sidegesture.ui.theme.ContentPaddingHorizontal
import com.aaron.sidegesture.ui.theme.ContentPaddingVerticalWithSection
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.TopBar

@Composable
fun ShizukuSettingsScreen(
    onBack: () -> Unit,
    vm: ShizukuSettingsVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        val context = LocalContext.current
        val status = uiState.status
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(R.string.shizuku_settings)
            )
            MyColumn {
                MySection(title = stringResource(R.string.shizuku_status_title)) {
                    MyTextButton(
                        onClick = vm::refreshStatus,
                        text = stringResource(R.string.shizuku_status_title),
                        secondaryText = context.shizukuStatusSummary(status),
                        prefix = {
                            Icon(
                                imageVector = Icons.Default.SettingsEthernet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    MyTextButton(
                        onClick = {},
                        enabled = false,
                        text = stringResource(R.string.shizuku_status_label),
                        secondaryText = context.shizukuStatusLabel(status),
                        prefix = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (status.permissionGranted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(R.string.shizuku_actions_title)
                ) {
                    if (!status.permissionGranted) {
                        MyTextButton(
                            onClick = vm::requestPermission,
                            enabled = status.installed && status.binderAlive,
                            text = stringResource(R.string.shizuku_request_permission),
                            secondaryText = when {
                                !status.installed -> stringResource(R.string.shizuku_not_installed_hint)
                                !status.binderAlive -> stringResource(R.string.shizuku_not_running_hint)
                                else -> stringResource(R.string.shizuku_permission_hint)
                            },
                            prefix = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    MyTextButton(
                        onClick = { context.launchPackage("moe.shizuku.privileged.api") },
                        enabled = status.installed,
                        text = stringResource(R.string.shizuku_open_app),
                        secondaryText = stringResource(R.string.shizuku_open_app_hint),
                        prefix = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    MyTextButton(
                        onClick = vm::refreshStatus,
                        text = stringResource(R.string.refresh),
                        secondaryText = stringResource(R.string.shizuku_refresh_hint),
                        prefix = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(R.string.shizuku_desc_title)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = ContentPaddingHorizontal)
                            .padding(vertical = ContentPaddingVerticalWithSection),
                        text = stringResource(R.string.shizuku_desc_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
