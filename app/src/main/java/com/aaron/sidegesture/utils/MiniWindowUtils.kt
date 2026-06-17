package com.aaron.sidegesture.utils

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import com.aaron.sidegesture.MiniWindowShareProxyActivity
import com.aaron.sidegesture.R
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.ActionSettings.MiniWindowMode
import com.aaron.sidegesture.miniwindow.RomDetector
import com.aaron.sidegesture.miniwindow.RomType
import com.blankj.utilcode.util.ConvertUtils
import kotlin.contracts.ExperimentalContracts
import kotlin.math.roundToInt

private const val WINDOWING_MODE_FREEFORM = 5
private const val WINDOWING_MODE_MEIZU = 11
private const val WINDOWING_MODE_OPPO = 100
private const val WINDOWING_MODE_HUAWEI_HONOR = 102
private const val WINDOWING_MODE_VIVO = 106
private const val HUAWEI_FREEFORM_STACK_ID = 2
private const val MIUI_SCALE_MIN_VERSION = 130
private const val MIUI_PORTRAIT_SCALE = 0.7f
private const val MIUI_LANDSCAPE_SCALE = 0.555f
private const val MINI_WINDOW_SHARE_MIME_TYPE = "text/plain"
private const val SUNSHINE_FREEFORM_PACKAGE = "com.sunshine.freeform"
private const val SUNSHINE_ACTION_START = "com.sunshine.freeform.start_freeform"
private const val SUNSHINE_ACTION_START_BY_MI = "com.sunshine.freeform.start_by_mi_freeform"

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/3/20
 *
 * 小窗(freeform)启动：makeBasic → 反射 setLaunchWindowingMode(code) → 华为/鸿蒙再 setLaunchStackId(2)
 * → setLaunchBounds(computeBounds) → startActivity(bundle)。
 * 隐藏 API 反射依赖 [com.aaron.sidegesture.App] 启动时的 Reflection.unseal 全局解封，无需 HiddenApiBypass。
 * code==106(vivo) 改走分享代理(显式锁定 [MiniWindowShareProxyActivity]，不弹选择器)；useMiWindow 改走小窗助手广播。
 */
object MiniWindowUtils {

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
            if (miniWindowSettings.useMiWindow) {
                return startSunshineFreeform(context, intent.component, showFailureToast)
            }
            val code = resolveCode(miniWindowSettings)
            if (code == WINDOWING_MODE_VIVO) {
                return startVivoShareProxy(context, intent, showFailureToast)
            }
            val activityOptions = buildFreeformActivityOptions(context, miniWindowSettings, code)
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
            if (miniWindowSettings.useMiWindow) {
                return startSunshineFreeform(context, intents.lastOrNull()?.component, showFailureToast)
            }
            val code = resolveCode(miniWindowSettings)
            if (code == WINDOWING_MODE_VIVO) {
                return startVivoShareProxy(context, intents.lastOrNull(), showFailureToast)
            }
            val activityOptions = buildFreeformActivityOptions(context, miniWindowSettings, code)
            context.startActivities(intents, activityOptions.toBundle())
            true
        } catch (ignored: Exception) {
            if (showFailureToast) {
                showToast(context.getString(R.string.launch_mini_window_failed))
            }
            false
        }
    }

    private fun resolveCode(settings: ActionSettings.MiniWindow): Int {
        return when (settings.mode) {
            MiniWindowMode.Auto -> RomDetector.freeFormCode()
            MiniWindowMode.Default -> WINDOWING_MODE_FREEFORM
            MiniWindowMode.Oppo -> WINDOWING_MODE_OPPO
            MiniWindowMode.Huawei -> WINDOWING_MODE_HUAWEI_HONOR
            MiniWindowMode.Vivo -> WINDOWING_MODE_VIVO
            MiniWindowMode.Meizu -> WINDOWING_MODE_MEIZU
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun buildFreeformActivityOptions(
        context: Context,
        settings: ActionSettings.MiniWindow,
        code: Int
    ): ActivityOptions {
        return ActivityOptions.makeBasic().also { options ->
            invokeIntMethod(options, "setLaunchWindowingMode", code)
            // 华为/鸿蒙需额外指定 freeform stack id，绑真实 ROM 而非 code，try 吞异常
            val rom = RomDetector.detect()
            if (rom.type == RomType.EMUI || rom.type == RomType.HARMONY_OS) {
                runCatching { invokeIntMethod(options, "setLaunchStackId", HUAWEI_FREEFORM_STACK_ID) }
            }
            options.setLaunchBounds(computeBounds(context, settings))
        }
    }

    /**
     * 按当前屏幕朝向计算小窗矩形。
     * 缩放补偿系数 scale：launchBounds 实际尺寸 = 设定尺寸 / scale。
     * 优先用用户配置的横竖屏补偿，未配置(null)时按 ROM 自动([autoScale])。
     */
    private fun computeBounds(
        context: Context,
        settings: ActionSettings.MiniWindow
    ): Rect {
        val portrait =
            context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val bounds = if (portrait) settings.portrait else settings.landscape
        val left = ConvertUtils.dp2px(bounds.leftDp.toFloat())
        val top = ConvertUtils.dp2px(bounds.topDp.toFloat())
        val width = ConvertUtils.dp2px(bounds.widthDp.toFloat())
        val height = ConvertUtils.dp2px(bounds.heightDp.toFloat())

        val scale = resolveScale(settings, portrait)
        return Rect(left, top, left + (width / scale).roundToInt(), top + (height / scale).roundToInt())
    }

    private fun resolveScale(settings: ActionSettings.MiniWindow, portrait: Boolean): Float {
        val configured = if (portrait) settings.portraitScale else settings.landscapeScale
        if (configured != null && configured > 0f) return configured
        return autoScale(portrait)
    }

    /**
     * 按当前 ROM 的自动缩放补偿：MIUI 13+(version>=130) 竖 0.7 / 横 0.555，其余 ROM 1.0(不补偿)。
     * 供设置页在「自动」模式下展示生效值。
     */
    fun autoScale(portrait: Boolean): Float {
        val rom = RomDetector.detect()
        val needScale = rom.type == RomType.MIUI && rom.version >= MIUI_SCALE_MIN_VERSION
        return when {
            !needScale -> 1f
            portrait -> MIUI_PORTRAIT_SCALE
            else -> MIUI_LANDSCAPE_SCALE
        }
    }

    private fun invokeIntMethod(target: ActivityOptions, methodName: String, value: Int) {
        val method = ActivityOptions::class.java.getMethod(methodName, Int::class.javaPrimitiveType)
        method.invoke(target, value)
    }

    /**
     * 第三方「小窗助手」路径：连发两条显式广播。fire-and-forget，设备未装则静默失败。
     */
    private fun startSunshineFreeform(
        context: Context,
        component: ComponentName?,
        showFailureToast: Boolean
    ): Boolean {
        if (component == null) {
            if (showFailureToast) {
                showToast(context.getString(R.string.launch_mini_window_failed))
            }
            return false
        }
        return try {
            sendSunshineBroadcast(context, SUNSHINE_ACTION_START, component)
            sendSunshineBroadcast(context, SUNSHINE_ACTION_START_BY_MI, component)
            true
        } catch (ignored: Exception) {
            if (showFailureToast) {
                showToast(context.getString(R.string.launch_mini_window_failed))
            }
            false
        }
    }

    private fun sendSunshineBroadcast(context: Context, action: String, component: ComponentName) {
        val intent = Intent(action).apply {
            setPackage(SUNSHINE_FREEFORM_PACKAGE)
            putExtra("packageName", component.packageName)
            putExtra("activityName", component.flattenToString())
        }
        context.sendBroadcast(intent)
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
            // 显式锁定本应用代理 Activity：chooser 只解析出唯一目标，系统直接拉起，不弹分享选择器
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                component = ComponentName(context, MiniWindowShareProxyActivity::class.java)
                type = MINI_WINDOW_SHARE_MIME_TYPE
                putExtra(Intent.EXTRA_INTENT, Intent(targetIntent))
                putExtra(Intent.EXTRA_TEXT, targetIntent.`package` ?: targetIntent.component?.packageName.orEmpty())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // 仍包一层 chooser：vivo 的分享小窗化挂在 chooser/resolver 流程上，去掉则不进小窗
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
