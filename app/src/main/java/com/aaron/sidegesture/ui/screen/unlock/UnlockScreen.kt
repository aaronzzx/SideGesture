package com.aaron.sidegesture.ui.screen.unlock

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.widget.TopBar

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

@Composable
fun UnlockScreen(
    onBack: () -> Unit,
    vm: UnlockVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) {
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.unlock_advanced_feature)
            )
        }
    }
}