package com.aaron.sidegesture.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import com.aaron.compose.ktx.clipToBackground
import com.aaron.compose.ui.BottomDialog
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ktx.gotoAlipayScan
import com.aaron.sidegesture.ktx.gotoWechatScan
import com.aaron.sidegesture.ui.theme.DialogTitleFontSize
import com.aaron.sidegesture.ui.theme.DialogTitlePadding
import com.aaron.sidegesture.ui.theme.ItemPadding
import com.aaron.sidegesture.utils.AboutUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/1
 */

@Composable
fun MyAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    title: String?,
    text: String,
    onCancelClick: (() -> Unit)? = null,
    autoDismissWhenClick: Boolean = true
) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        title = {
            if (!title.isNullOrEmpty()) {
                Text(text = title)
            }
        },
        text = {
            Text(text = text)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (autoDismissWhenClick) {
                        onDismissRequest()
                    }
                    onConfirmClick()
                }
            ) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (autoDismissWhenClick) {
                        onDismissRequest()
                    }
                    onCancelClick?.invoke()
                }
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun MyAppsDialog(
    onDismissRequest: () -> Unit
) {
    BottomDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBackground(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Text(
                modifier = Modifier.padding(DialogTitlePadding),
                text = stringResource(id = R.string.my_apps_dialog_title),
                fontSize = DialogTitleFontSize,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                modifier = Modifier.padding(horizontal = DialogTitlePadding),
                text = stringResource(id = R.string.my_apps_dialog_desc),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            val imgList = remember {
                listOf(
                    R.drawable.img_yespdf_screenshot1,
                    R.drawable.img_yespdf_screenshot2,
                    R.drawable.img_yespdf_screenshot3
                )
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                contentPadding = PaddingValues(ItemPadding),
                horizontalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                items(imgList) { item ->
                    AsyncImage(
                        model = item,
                        contentDescription = null,
                        imageLoader = LocalContext.current.imageLoader
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(text = stringResource(id = R.string.my_apps_dialog_cancel))
                }
                val context = LocalContext.current
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        AboutUtils.gotoDownloadYesPdf(context)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(id = R.string.my_apps_dialog_confirm))
                }
            }
        }
    }
}

@Composable
fun DonateDialog(
    onDismissRequest: () -> Unit
) {
    BottomDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBackground(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Text(
                modifier = Modifier.padding(DialogTitlePadding),
                text = stringResource(id = R.string.donate),
                fontSize = DialogTitleFontSize,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                modifier = Modifier.padding(horizontal = DialogTitlePadding),
                text = stringResource(id = R.string.donate_dialog_desc),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            val imgList = remember {
                listOf(
                    R.drawable.img_wechat_pay,
                    R.drawable.img_alipay_pay
                )
            }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                contentPadding = PaddingValues(ItemPadding),
                horizontalArrangement = Arrangement.spacedBy(ItemPadding)
            ) {
                items(imgList) { item ->
                    AsyncImage(
                        model = item,
                        contentDescription = null,
                        imageLoader = LocalContext.current.imageLoader
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(text = stringResource(id = R.string.donate_dialog_cancel))
                }
                val context = LocalContext.current
                val wechatImg = ImageBitmap.imageResource(id = R.drawable.img_wechat_pay)
                val alipayImg = ImageBitmap.imageResource(id = R.drawable.img_alipay_pay)
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val bitmap = wechatImg.asAndroidBitmap()
                        AboutUtils.copyBitmapToDevice(context, bitmap, "wechat_qrcode.jpg")
                        context.gotoWechatScan()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(id = R.string.donate_dialog_wechat))
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val bitmap = alipayImg.asAndroidBitmap()
                        AboutUtils.copyBitmapToDevice(context, bitmap, "alipay_qrcode.jpg")
                        context.gotoAlipayScan()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(id = R.string.donate_dialog_alipay))
                }
            }
        }
    }
}