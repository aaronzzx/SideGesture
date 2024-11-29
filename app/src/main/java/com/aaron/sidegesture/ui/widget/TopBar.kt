package com.aaron.sidegesture.ui.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.aaron.sidegesture.ui.theme.TopBarPaddingExtra

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/22
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    showBackIcon: Boolean = true,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    postfixTitle: (@Composable () -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.let {
                        if (showBackIcon) it else {
                            it.padding(start = TopBarPaddingExtra)
                        }
                    },
                    text = title,
                    style = titleStyle
                )
                postfixTitle?.invoke()
            }
        },
        navigationIcon = {
            if (showBackIcon) {
                IconButton(
                    modifier = Modifier
                        .padding(start = TopBarPaddingExtra / 2),
                    onClick = onBack
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go Back")
                }
            }
        },
        actions = actions
    )
}