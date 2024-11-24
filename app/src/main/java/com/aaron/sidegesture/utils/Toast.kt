package com.aaron.sidegesture.utils

import android.view.Gravity
import androidx.annotation.StringRes
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ToastUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

private var init = false

fun showToast(text: String) {
    if (!init) {
        init()
    }
    ToastUtils.showShort(text)
}

fun showToast(@StringRes resId: Int) {
    if (!init) {
        init()
    }
    ToastUtils.showShort(resId)
}

private fun init() {
    init = true
    ToastUtils
        .getDefaultMaker()
        .setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, ConvertUtils.dp2px(100f))
}