package com.aaron.sidegesture.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.net.Uri
import com.aaron.sidegesture.entity.LauncherInfo
import com.aaron.sidegesture.ktx.queryIntentActivitiesCompat
import org.xmlpull.v1.XmlPullParser
import java.util.UUID

/**
 * @author aaronzzxup@gmail.com
 * @since 2025/6/25
 */
object ShortcutUtils {

    private const val XMLNS_ANDROID = "http://schemas.android.com/apk/res/android"

    fun getAllAppsWithShortcut(context: Context): List<LauncherInfo> {
        val result = arrayListOf<LauncherInfo>()
        val intent2 = Intent(Intent.ACTION_MAIN)
        intent2.addCategory(Intent.CATEGORY_LAUNCHER)
        for (resolveInfo in context.packageManager.queryIntentActivitiesCompat(intent2, PackageManager.GET_META_DATA)) {
            val activityInfo = resolveInfo.activityInfo

            val shortcutsMetadata = activityInfo.loadXmlMetaData(context.packageManager, "android.app.shortcuts")
                ?: continue

            shortcutsMetadata.use { parser ->
                val shortcuts = parseShortcuts(context, activityInfo, parser)
                if (shortcuts.isNotEmpty()) {
                    val info = LauncherInfo(
                        packageName = activityInfo?.packageName ?: "",
                        className = activityInfo?.name ?: "",
                        label = activityInfo.loadLabel(context.packageManager).toString(),
                        shortcuts = shortcuts
                    )
                    result.add(info)
                }
            }
        }
        return result
    }

    private fun parseShortcuts(context: Context, actInfo: ActivityInfo, parser: XmlResourceParser): List<LauncherInfo.ShortcutInfo> {
        val result = arrayListOf<LauncherInfo.ShortcutInfo>()

        var eventType = parser.eventType
        var currentId: String? = null
        var currentLabel: String? = null
        var currentIconRes = 0
        var currentIntent: Intent? = null
        var currentIntents = arrayListOf<String>()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when {
                // End of shortcut, add it to the list
                eventType == XmlPullParser.END_TAG && parser.name == "shortcut" -> {
                    if (currentId != null && currentLabel != null && currentIntents.isNotEmpty()) {
                        val info = LauncherInfo.ShortcutInfo(
                            packageName = actInfo.packageName,
                            className = actInfo.name,
                            label = currentLabel,
                            iconRes = currentIconRes,
                            intents = currentIntents
                        )
                        result.add(info)
                    }
                    currentId = null
                    currentLabel = null
                    currentIconRes = 0
                    currentIntents = arrayListOf()
                }
                // End of intent, add it to the list
                eventType == XmlPullParser.END_TAG && parser.name == "intent" && currentIntent != null -> {
                    currentIntents.add(currentIntent.toUri(Intent.URI_INTENT_SCHEME))
                }
                // Ignore other end tags
                eventType != XmlPullParser.START_TAG -> { }
                // If shortcut has android:enabled="false", ignore it
                parser.name == "shortcut" && parser.getAttributeBooleanValue(XMLNS_ANDROID, "enabled", true) -> {
                    currentId = parser.getAttributeValue("shortcutId") ?: UUID.randomUUID().toString()
                    var labelRes = parser.getAttributeResourceValue("shortcutShortLabel")
                    if (labelRes == 0) {
                        labelRes = parser.getAttributeResourceValue("shortcutLongLabel")
                    }
                    currentLabel = if (labelRes == 0) {
                        null
                    } else {
                        val resources = context.packageManager.getResourcesForApplication(actInfo.packageName)
                        resources.getString(labelRes)
                    }
                    currentIconRes = parser.getAttributeResourceValue("icon")
                }
                // If shortcut is disabled, currentId should be null. Ignore <intent>s.
                parser.name == "intent" && currentId != null -> {
                    try {
                        val shortcutIntent = Intent().apply {
                            action = parser.getAttributeValue("action")
                            val pkg = parser.getAttributeValue("targetPackage")
                            val cls = parser.getAttributeValue("targetClass")
                                ?: parser.getAttributeValue("targetActivity")?.replace('$', '_')
                            if (pkg != null && cls != null) {
                                setClassName(pkg, cls)
                            }
                            // Use last activity name as shortcut label
                            // Workaround for older versions where some XML attrs are unavailable (shortcutShortLabel...)
                            currentLabel = currentLabel ?: cls?.substringAfterLast('.')
                            data = parser.getAttributeValue("data")?.let(Uri::parse)
                        }
                        currentIntent = shortcutIntent
                    } catch (_: Exception) { }
                }
                // Intent extra. Just assume it is a String
                parser.name == "extra" && currentIntent != null -> {
                    val name = parser.getAttributeValue("name")
                    val value = parser.getAttributeValue("value")
                    if (name != null && value != null) {
                        currentIntent.putExtra(name, value)
                    }
                }
                // Intent categories
                parser.name == "categories" && currentIntent != null -> {
                    val name = parser.getAttributeValue("name")
                    if (name != null) {
                        currentIntent.addCategory(name)
                    }
                }
            }
            eventType = parser.next()
        }

        return result
    }

    /**
     * These two methods try to get attributes with and without the android: namespace
     * They are needed because some shortcuts.xml (e.g. Chrome) doesn't use namespaces
     */
    private fun XmlResourceParser.getAttributeValue(attribute: String): String? {
        return getAttributeValue(XMLNS_ANDROID, attribute) ?: getAttributeValue(null, attribute)
    }

    private fun XmlResourceParser.getAttributeResourceValue(attribute: String): Int {
        val res = getAttributeResourceValue(XMLNS_ANDROID, attribute, 0)
        if (res != 0) {
            return res
        }
        return getAttributeResourceValue(null, attribute, 0)
    }
}