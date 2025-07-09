package com.aaron.sidegesture.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import androidx.annotation.StringRes
import androidx.core.os.postDelayed
import com.aaron.sidegesture.App
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.widget.ToastDuration
import com.aaron.sidegesture.ui.widget.showComposeToast
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ProcessUtils
import com.blankj.utilcode.util.ToastUtils

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */

private const val DELAYED_MILLIS = 2000L

private var init = false

private val handler = Handler(Looper.getMainLooper())

fun showVersionTooLowToast(context: Context, @StringRes placeholder: Int = 0) {
    if (placeholder == 0) {
        showToast(context.getString(R.string.os_version_too_low))
    } else {
        val name = context.getString(placeholder)
        showToast(context.getString(R.string.os_version_too_low_placeholder, name))
    }
}

fun showToastDelay(text: String, continueBlock: () -> Unit) {
    showToast(text, DELAYED_MILLIS) {
        continueBlock()
    }
}

fun showToastLongDelay(text: String, continueBlock: () -> Unit) {
    showToastLong(text, DELAYED_MILLIS) {
        continueBlock()
    }
}

fun showToastDelay(@StringRes resId: Int, continueBlock: () -> Unit) {
    showToast(resId, DELAYED_MILLIS) {
        continueBlock()
    }
}

fun showToastLongDelay(@StringRes resId: Int, continueBlock: () -> Unit) {
    showToastLong(resId, DELAYED_MILLIS) {
        continueBlock()
    }
}

fun showToast(text: String, delayMs: Long = 0, continueBlock: (() -> Unit)? = null) {
//    if (canShowToast()) {
//        if (!init) {
//            init()
//        }
//        ToastUtils.showShort(text)
//    } else {
//    }
    showComposeToast(text)
    if (delayMs > 0 && continueBlock != null) {
        handler.postDelayed(delayMs) {
            continueBlock()
        }
    }
}

fun showToastLong(text: String, delayMs: Long = 0, continueBlock: (() -> Unit)? = null) {
//    if (canShowToast()) {
//        if (!init) {
//            init()
//        }
//        ToastUtils.showShort(text)
//    } else {
//    }
    showComposeToast(text, ToastDuration.Long)
    if (delayMs > 0 && continueBlock != null) {
        handler.postDelayed(delayMs) {
            continueBlock()
        }
    }
}

fun showToast(@StringRes resId: Int, delayMs: Long = 0, continueBlock: (() -> Unit)? = null) {
//    if (canShowToast()) {
//        if (!init) {
//            init()
//        }
//        ToastUtils.showShort(resId)
//    } else {
//    }
    showComposeToast(resId)
    if (delayMs > 0 && continueBlock != null) {
        handler.postDelayed(delayMs) {
            continueBlock()
        }
    }
}

fun showToastLong(@StringRes resId: Int, delayMs: Long = 0, continueBlock: (() -> Unit)? = null) {
//    if (canShowToast()) {
//        if (!init) {
//            init()
//        }
//        ToastUtils.showShort(resId)
//    } else {
//    }
    showComposeToast(resId, ToastDuration.Long)
    if (delayMs > 0 && continueBlock != null) {
        handler.postDelayed(delayMs) {
            continueBlock()
        }
    }
}

private fun canShowToast(): Boolean {
    return ProcessUtils.isMainProcess() ||
            PopBackgroundPermissionUtil.hasPopupBackgroundPermission(App.getContext())
}

private fun init() {
    init = true
    ToastUtils
        .getDefaultMaker()
        .setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, ConvertUtils.dp2px(100f))
}