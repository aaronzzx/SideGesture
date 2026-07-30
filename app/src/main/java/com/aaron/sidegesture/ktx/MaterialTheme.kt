package com.aaron.sidegesture.ktx

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aaron.sidegesture.ui.theme.appColors

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/7
 */

val ColorScheme.wechatColor: Color
    @Composable get() = MaterialTheme.appColors.weChat

val ColorScheme.alipayColor: Color
    @Composable get() = MaterialTheme.appColors.aliPay
