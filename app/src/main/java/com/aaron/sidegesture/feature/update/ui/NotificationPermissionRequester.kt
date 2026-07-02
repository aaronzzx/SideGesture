package com.aaron.sidegesture.feature.update.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ktx.findActivity
import com.aaron.sidegesture.ktx.gotoAppNotificationSettings
import com.aaron.sidegesture.ui.widget.MyAlertDialog

/**
 * 通知权限请求器（POST_NOTIFICATIONS），返回触发函数供按钮调用。
 *
 * 关键：当系统直接拒绝、根本没弹授权框（USER_FIXED：用户曾「拒绝且不再询问」）时，
 * 自动跳系统通知设置页让用户手动开 —— 避免「点了确认没反应」的死路。
 * 首启弹窗与设置页开关共用同一逻辑，行为一致。
 *
 * @author aaronzzxup@gmail.com
 * @since 2026/6/19
 */
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) return@rememberLauncherForActivityResult
        val activity = context.findActivity() ?: return@rememberLauncherForActivityResult
        // shouldShowRationale=false 且未授予 = 系统不会再弹框（USER_FIXED）→ 跳设置页兜底
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            context.gotoAppNotificationSettings()
        }
    }
    return { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

/** 通知权限说明弹窗（首启 host 与设置页共用同一文案）。 */
@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    MyAlertDialog(
        onDismissRequest = onDismiss,
        onConfirmClick = onConfirm,
        title = stringResource(id = R.string.notification_permission_title),
        text = stringResource(id = R.string.notification_permission_rationale)
    )
}
