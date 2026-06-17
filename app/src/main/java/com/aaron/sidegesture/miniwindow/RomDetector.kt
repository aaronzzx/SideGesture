package com.aaron.sidegesture.miniwindow

import android.os.Build

/**
 * ROM 类型与版本判定，用于小窗(freeform)的 windowingMode 码选择与尺寸缩放守卫。
 *
 * 判定基于系统属性(getprop)，结果用 [@Volatile] 缓存，避免热路径反复 exec。
 *
 * @author aaronzzxup@gmail.com
 * @since 2025/6/16
 */
object RomDetector {

    private const val WINDOWING_MODE_FREEFORM = 5
    private const val WINDOWING_MODE_MEIZU = 11
    private const val WINDOWING_MODE_OPPO = 100
    private const val WINDOWING_MODE_HUAWEI_HONOR = 102
    private const val WINDOWING_MODE_VIVO = 106

    @Volatile
    private var cached: RomInfo? = null

    fun detect(): RomInfo {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: resolve().also { cached = it }
        }
    }

    /**
     * 不同 ROM 的 freeform windowingMode 码。
     * EMUI/鸿蒙=102、OPPO=100、VIVO=106、魅族=11、其余(MIUI/未知)=5。
     */
    fun freeFormCode(rom: RomInfo = detect()): Int {
        return when (rom.type) {
            RomType.EMUI, RomType.HARMONY_OS -> WINDOWING_MODE_HUAWEI_HONOR
            RomType.OPPO -> WINDOWING_MODE_OPPO
            RomType.VIVO -> WINDOWING_MODE_VIVO
            RomType.MEIZU -> WINDOWING_MODE_MEIZU
            RomType.MIUI, RomType.UNKNOWN -> WINDOWING_MODE_FREEFORM
        }
    }

    private fun resolve(): RomInfo {
        getProp("ro.miui.ui.version.name").let {
            if (it.isNotEmpty()) return RomInfo(RomType.MIUI, parseVersion(it))
        }
        // 鸿蒙需在 EMUI 之前判定：华为设备同时存在 emui 属性
        if (isHarmony()) {
            return RomInfo(RomType.HARMONY_OS, parseVersion(getProp("ro.build.version.emui")))
        }
        getProp("ro.build.version.emui").let {
            if (it.isNotEmpty()) return RomInfo(RomType.EMUI, parseVersion(it))
        }
        getProp("ro.build.version.opporom").let {
            if (it.isNotEmpty()) return RomInfo(RomType.OPPO, parseVersion(it))
        }
        getProp("ro.vivo.os.version").let {
            if (it.isNotEmpty()) return RomInfo(RomType.VIVO, parseVersion(it))
        }
        // 魅族 Flyme 无独立版本属性，靠 display.id 含 "Flyme" 判定，如 "Flyme 9.3.2.1A"→9
        getProp("ro.build.display.id").let {
            if (it.contains("flyme", ignoreCase = true)) {
                return RomInfo(RomType.MEIZU, parseVersion(it))
            }
        }
        // 属性缺失时按品牌兜底，realme/oppo/oneplus 归 OPPO
        return when (Build.BRAND.lowercase()) {
            "xiaomi", "redmi", "poco" -> RomInfo(RomType.MIUI, 0)
            "huawei", "honor" -> RomInfo(RomType.EMUI, 0)
            "oppo", "oneplus", "realme" -> RomInfo(RomType.OPPO, 0)
            "vivo", "iqoo" -> RomInfo(RomType.VIVO, 0)
            "meizu" -> RomInfo(RomType.MEIZU, 0)
            else -> RomInfo(RomType.UNKNOWN, 0)
        }
    }

    private fun isHarmony(): Boolean {
        return try {
            val clazz = Class.forName("com.huawei.system.BuildEx")
            val brand = clazz.getMethod("getOsBrand").invoke(null) as? String
            "harmony".equals(brand, ignoreCase = true)
        } catch (ignored: Throwable) {
            false
        }
    }

    private fun getProp(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
            process.inputStream.bufferedReader().use { it.readLine() }?.trim().orEmpty()
        } catch (ignored: Throwable) {
            ""
        }
    }

    /** 取字符串里第一个整数，如 "V130" → 130、"EmotionUI_11.0.0" → 11，无数字返回 0。 */
    private fun parseVersion(value: String): Int {
        return Regex("\\d+").find(value)?.value?.toIntOrNull() ?: 0
    }
}

enum class RomType {
    MIUI, EMUI, HARMONY_OS, OPPO, VIVO, MEIZU, UNKNOWN
}

data class RomInfo(val type: RomType, val version: Int)
