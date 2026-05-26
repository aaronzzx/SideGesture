package com.aaron.sidegesture.screenshot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.aaron.sidegesture.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScreenshotStorage {

    private fun fileName(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "SideGesture_${formatter.format(Date())}.png"
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName())
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/gulugulu")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        ) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    error("compress failed")
                }
            } ?: error("output stream is null")
            resolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                },
                null,
                null
            )
            uri
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun createShareUri(context: Context, bitmap: Bitmap): Uri? {
        return createCacheUri(context, bitmap)
    }

    fun createClipboardUri(context: Context, bitmap: Bitmap): Uri? {
        return createCacheUri(context, bitmap)
    }

    private fun createCacheUri(context: Context, bitmap: Bitmap): Uri? {
        val root = context.externalCacheDir ?: return null
        val parent = File(root, "screenshots").apply { mkdirs() }
        val file = File(parent, fileName())
        return try {
            FileOutputStream(file).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    error("compress failed")
                }
                output.flush()
            }
            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    fun copyToClipboard(context: Context, uri: Uri): Boolean {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(context.contentResolver, fileName(), uri)
            clipboardManager.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun share(context: Context, uri: Uri): Boolean {
        return try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }
}
