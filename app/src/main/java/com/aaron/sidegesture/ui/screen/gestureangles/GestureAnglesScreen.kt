package com.aaron.sidegesture.ui.screen.gestureangles

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.widget.TopBar
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/26
 */

@Serializable
data object GestureAngles

@Composable
fun GestureAnglesScreen(
    onBack: () -> Unit,
    vm: GestureAnglesVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        if (uiState.showResetWarningDialog) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.surface,
                onDismissRequest = {
                    vm.showResetWarningDialog(false)
                },
                title = { Text(text = stringResource(id = R.string.reset_default_settings)) },
                text = {
                    Text(text = stringResource(id = R.string.reset_gesture_angles_warning))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.showResetWarningDialog(false)
                            vm.saveSettings()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vm.showResetWarningDialog(false)
                            vm.reset()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            )
        }
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.gesture_angles),
                actions = {
                    IconButton(onClick = { vm.showResetWarningDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Reset"
                        )
                    }
                    IconButton(onClick = { vm.saveSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Save"
                        )
                    }
                }
            )
        }
    }
}