package com.aaron.sidegesture.ui.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.component.UDFComponent
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.ui.theme.SectionPadding
import com.aaron.sidegesture.ui.widget.MyColumn
import com.aaron.sidegesture.ui.widget.MySection
import com.aaron.sidegesture.ui.widget.MyTextButton
import com.aaron.sidegesture.ui.widget.TopBar
import com.aaron.sidegesture.utils.AboutUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    vm: AboutVM = viewModel()
) {
    UDFComponent(component = vm.udfComponent, onEvent = {}) { uiState ->
        Column {
            TopBar(
                onBack = onBack,
                title = stringResource(id = R.string.about)
            )
            MyColumn(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(ItemPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        modifier = Modifier.size(100.dp),
                        model = uiState.icon,
                        contentDescription = null,
                        imageLoader = LocalContext.current.imageLoader
                    )
                    Text(
                        text = uiState.appName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W900)
                    )
                    Text(
                        text = uiState.versionName,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                MySection(
                    modifier = Modifier.padding(top = SectionPadding),
                    title = stringResource(id = R.string.overview)
                ) {
                    val context = LocalContext.current
                    MyTextButton(
                        onClick = { AboutUtils.checkUpgrade(context) },
                        text = stringResource(id = R.string.check_update),
                        prefix = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.github),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    MyTextButton(
                        onClick = { AboutUtils.feedbackEmail(context) },
                        text = stringResource(id = R.string.feedback_email),
                        prefix = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    MyTextButton(
                        onClick = { AboutUtils.feedbackCoolapk(context) },
                        text = stringResource(id = R.string.feedback_coolapk),
                        prefix = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.coolapk),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
        }
    }
}