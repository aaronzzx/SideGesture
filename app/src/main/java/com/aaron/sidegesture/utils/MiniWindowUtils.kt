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
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindowMode
import com.blankj.utilcode.util.ScreenUtils
import kotlin.contracts.ExperimentalContracts
import kotlin.math.roundToInt

private const val WINDOWING_MODE_FREEFORM = 5
private const val WINDOWING_MODE_OPPO = 100
private const val WINDOWING_MODE_HUAWEI_HONOR = 102
private const val MINI_WINDOW_SHARE_MIME_TYPE = "text/plain"

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
    fun startActivity(
        context: Context,
        component: ComponentName,
        miniWindowSettings: ActionSettings.MiniWindow = ActionSettings.MiniWindow()
    ): Boolean {
        val intent = Intent().apply {
            setComponent(component)
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startActivity(context, intent, miniWindowSettings)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun startActivity(
        context: Context,
        intent: Intent,
        miniWindowSettings: ActionSettings.MiniWindow = ActionSettings.MiniWindow(),
        showFailureToast: Boolean = true
    ): Boolean {
        return try {
            if (resolveMode() == MiniWindowMode.Vivo) {
                return startVivoShareProxy(context, intent, showFailureToast)
            }
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
        miniWindowSettings: ActionSettings.MiniWindow = ActionSettings.MiniWindow(),
        showFailureToast: Boolean = true
    ): Boolean {
        return try {
            if (resolveMode() == MiniWindowMode.Vivo) {
                return startVivoShareProxy(context, intents.lastOrNull(), showFailureToast)
            }
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
        return when (resolveMode()) {
            MiniWindowMode.Huawei -> makeActivityOptions(WINDOWING_MODE_HUAWEI_HONOR)
            MiniWindowMode.Oppo -> makeActivityOptions(WINDOWING_MODE_OPPO)
            MiniWindowMode.Default,
            MiniWindowMode.Auto -> makeActivityOptions(WINDOWING_MODE_FREEFORM)
            MiniWindowMode.Vivo -> makeActivityOptions(WINDOWING_MODE_FREEFORM)
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
                method.invoke(it, mode)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            val screenWidth = ScreenUtils.getScreenWidth()
            val screenHeight = ScreenUtils.getScreenHeight()
            val width = screenWidth
            val scaledWidth = width * 0.7f
            val left = ((screenWidth - scaledWidth) / 2f).roundToInt()
            val right = left + width
            val height = (width / 0.625f).roundToInt()
            val top = (screenHeight - height) / 2
            val bottom = top + height
            val bounds =  Rect(left, top, right, bottom)
            it.setLaunchBounds(bounds)
        }
    }

    private fun resolveMode(): MiniWindowMode {
        return when (Build.BRAND.lowercase()) {
            "vivo", "iqoo" -> MiniWindowMode.Vivo
            "oppo", "oneplus", "realme" -> MiniWindowMode.Oppo
            "huawei", "honor" -> MiniWindowMode.Huawei
            else -> MiniWindowMode.Default
        }
    }

    private fun startVivoShareProxy(
        context: Context,
        targetIntent: Intent?,
        showFailureToast: Boolean
    ): Boolean {
        if (targetIntent == null) {
            if (showFailureToast) {
                showToast(context.getString(R.string.launch_mini_window_failed))
            }
            return false
        }
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MINI_WINDOW_SHARE_MIME_TYPE
                putExtra(Intent.EXTRA_INTENT, Intent(targetIntent))
                putExtra(Intent.EXTRA_TEXT, targetIntent.`package` ?: targetIntent.component?.packageName.orEmpty())
            }
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.action_popup_screen)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (ignored: Exception) {
            if (showFailureToast) {
                showToast(context.getString(R.string.launch_mini_window_failed))
            }
            false
        }
    }
}
