package com.aaron.sidegesture.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.R
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

    fun feedbackEmail(context: Context, uri: Uri? = null) {
        try {
            val subject = "${context.getString(R.string.app_name)} ${context.getString(R.string.feedback)}"
            val text = (context.getString(R.string.feedback_email_headline) + "\n"
                    + "Device: " + Build.BRAND + "-" + Build.MODEL + "\n"
                    + "Android Version: " + Build.VERSION.RELEASE + "(SDK=" + Build.VERSION.SDK_INT + ")" + "\n"
                    + "Resolution: " + ScreenUtils.getScreenWidth() + "*" + ScreenUtils.getScreenHeight() + "\n"
                    + "System Language: " + Locale.getDefault().language + "(" + Locale.getDefault().country + ")" + "\n"
                    + "App Version: " + BuildConfig.VERSION_NAME)
            val sendMail = Intent(Intent.ACTION_SENDTO).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                data = "mailto:aaronzzxup@gmail.com".toUri()
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
            }
            context.startActivity(Intent.createChooser(sendMail, "Send Email"))
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

    fun gotoDownloadYesPdf(context: Context) {
        val github = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/aaronzzx/yespdf/releases")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(github)
    }

    fun copyBitmapToDevice(
        context: Context,
        bitmap: Bitmap?,
        fileName: String,
        savePath: String = "${PathUtils.getExternalAppCachePath()}/$fileName"
    ) {
        bitmap ?: return
        val file = File(savePath)
        file.mkdirs()
        if (file.exists()) file.delete()
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
        } catch (ignored: IOException) {
        } finally {
            try {
                fos?.close()
            } catch (ignored: IOException) {
            }
        }
        notifyMedia(context, savePath, "${BuildConfig.APPLICATION_ID}.fileprovider")
    }

    private fun notifyMedia(context: Context, path: String, authority: String) {
        try {
            // 通知相册更新
            val file = File(path)
            MediaStore.Images.Media.insertImage(context.contentResolver, BitmapFactory.decodeFile(file.absolutePath), file.name, null)
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val uri = getUri(context, authority, file)
            intent.data = uri
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getUri(context: Context, authority: String, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= 24) {
            FileProvider.getUriForFile(context, authority, file)
        } else {
            Uri.fromFile(file)
        }
    }
}