package com.aaron.sidegesture.utils

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import com.aaron.sidegesture.R
import com.blankj.utilcode.util.ScreenUtils
import kotlin.contracts.ExperimentalContracts
import kotlin.math.roundToInt

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/3/20
 */
object MiniWindowUtils {

    @OptIn(ExperimentalContracts::class)
    fun isMiniWindowSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val name = PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT
            context.packageManager.hasSystemFeature(name)
        } else false
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun startActivity(context: Context, component: ComponentName): Boolean {
        val intent = Intent().apply {
            setComponent(component)
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startActivity(context, intent)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun startActivity(
        context: Context,
        intent: Intent,
        showFailureToast: Boolean = true
    ): Boolean {
        return try {
            val activityOptions = getActivityOptions()
            context.startActivity(intent, activityOptions.toBundle())
            true
        } catch (ignored: Exception) {
            if (showFailureToast) {
                showToast(context.getString(R.string.launch_mini_window_failed))
            }
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun startActivities(
        context: Context,
        intents: Array<Intent>,
        showFailureToast: Boolean = true
    ): Boolean {
        return try {
            val activityOptions = getActivityOptions()
            context.startActivities(intents, activityOptions.toBundle())
            true
        } catch (ignored: Exception) {
            if (showFailureToast) {
                showToast(context.getString(R.string.launch_mini_window_failed))
            }
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getActivityOptions(): ActivityOptions {
        val brand = Build.BRAND.lowercase()
        return when (brand) {
            "huawei", "honor" -> makeActivityOptions(102)
            "oppo", "oneplus", "realme" -> makeActivityOptions(100)
            else -> makeActivityOptions(5)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun makeActivityOptions(mode: Int): ActivityOptions {
        return ActivityOptions.makeBasic().also {
            try {
                val method = ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(it, mode/*WINDOWING_MODE_FREEFORM*/)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            val screenWidth = ScreenUtils.getScreenWidth()
            val screenHeight = ScreenUtils.getScreenHeight()

            var bounds: Rect? = null
            if (mode == 5) {
                val width = screenWidth
                val scaledWidth = width * 0.7f
                val left = ((screenWidth - scaledWidth) / 2f).roundToInt()
                val right = left + width
                val height = (width / 0.625f).roundToInt()
                val top = (screenHeight - height) / 2
                val bottom = top + height
                bounds = Rect(left, top, right, bottom)
            }

            it.setLaunchBounds(bounds)
        }
    }
}
