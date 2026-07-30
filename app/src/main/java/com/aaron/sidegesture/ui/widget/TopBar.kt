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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.theme.dimensions
import com.aaron.sidegesture.ui.theme.textStyles

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
    titleStyle: TextStyle = MaterialTheme.textStyles.topBarTitle,
    containerColor: Color = Color.Transparent,
    postfixTitle: (@Composable () -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
        title = {
            if (titleContent != null) {
                titleContent()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.let {
                            if (showBackIcon) it else {
                                it.padding(start = MaterialTheme.dimensions.topBar.contentInset)
                            }
                        },
                        text = title,
                        style = titleStyle
                    )
                    postfixTitle?.invoke()
                }
            }
        },
        navigationIcon = {
            if (showBackIcon) {
                IconButton(
                    modifier = Modifier.padding(start = MaterialTheme.dimensions.topBar.leadingInset),
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.go_back)
                    )
                }
            }
        },
        actions = {
            Row(modifier = Modifier.padding(end = MaterialTheme.dimensions.topBar.trailingInset)) {
                actions()
            }
        }
    )
}
