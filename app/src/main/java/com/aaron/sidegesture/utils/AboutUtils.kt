package com.aaron.sidegesture.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.R
import com.blankj.utilcode.util.ScreenUtils
import java.util.Locale

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/12/7
 */
object AboutUtils {

    fun checkUpgrade(context: Context) {
        val github = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/aaronzzx/gulugulu/releases")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(github)
    }

    fun feedbackEmail(context: Context) {
        try {
            val subject = "${context.getString(R.string.app_name)} ${context.getString(R.string.feedback)}"
            val text = (context.getString(R.string.feedback_email_headline) + "\n"
                    + "Device: " + Build.BRAND + "-" + Build.MODEL + "\n"
                    + "Android Version: " + Build.VERSION.RELEASE + "(SDK=" + Build.VERSION.SDK_INT + ")" + "\n"
                    + "Resolution: " + ScreenUtils.getScreenWidth() + "*" + ScreenUtils.getScreenHeight() + "\n"
                    + "System Language: " + Locale.getDefault().language + "(" + Locale.getDefault().country + ")" + "\n"
                    + "App Version: " + BuildConfig.VERSION_NAME)
            val sendMail = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:aaronzzxup@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendMail)
        } catch (ignored: Exception) {
            showToast(R.string.email_app_not_found)
        }
    }

    fun feedbackCoolapk(context: Context) {
        try {
            val uri = Uri.parse("coolmarket://u/1012199")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.coolapk.market")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ignored: Exception) {
            showToast(R.string.coolapk_app_not_found)
        }
    }
}