package com.aaron.sidegesture.ui.screen.gesturebuttonsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.widget.TopBar
import kotlinx.serialization.Serializable

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

@Serializable
data object GestureButtonSettings

@Composable
fun GestureButtonSettingsScreen(
    onBack: () -> Unit,
    vm: GestureButtonSettingsVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) {
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.gesture_button_settings)
            )
        }
    }
}