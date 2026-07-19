package com.aaron.sidegesture.feature.quicktools

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.ktx.canWriteSystemSettings
import com.aaron.sidegesture.ktx.dispatchMediaKeyEvent
import com.aaron.sidegesture.ktx.gotoManageWriteSettings
import com.aaron.sidegesture.ktx.gotoNotificationListenerSettings
import com.aaron.sidegesture.ktx.readGlobalInt
import com.aaron.sidegesture.ktx.readSystemInt
import com.aaron.sidegesture.platform.shizuku.ShizukuShellManager
import com.aaron.sidegesture.utils.FlashlightController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

object QuickToolsExecutor {

    fun brightnessGateway(context: Context): QuickToolsBrightnessGateway {
        return SystemQuickToolsBrightnessGateway(context)
    }

    fun currentVolumeRatio(service: SideGestureService): Float {
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
    }

    fun currentWifiEnabled(service: SideGestureService): Boolean {
        return service.readGlobalInt("wifi_on", 0) == 1
    }

    fun currentBluetoothEnabled(service: SideGestureService): Boolean {
        return service.readGlobalInt("bluetooth_on", 0) == 1
    }

    fun currentMuteEnabled(service: SideGestureService): Boolean {
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
    }

    fun currentFlashlightEnabled(service: SideGestureService): Boolean {
        return FlashlightController.isEnabled(service)
    }

    fun setVolumeRatio(service: SideGestureService, ratio: Float) {
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (ratio.coerceIn(0f, 1f) * maxVolume).roundToInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
    }

    suspend fun toggleWifi(service: SideGestureService): QuickToolsOperationResult {
        if (!ShizukuShellManager.currentStatus().permissionGranted) {
            return QuickToolsOperationResult.NeedsShizuku
        }
        val enable = !currentWifiEnabled(service)
        return ShizukuShellManager.execute(
            "cmd wifi set-wifi-enabled ${if (enable) "enabled" else "disabled"}"
        ).toOperationResult()
    }

    suspend fun toggleBluetooth(service: SideGestureService): QuickToolsOperationResult {
        if (!ShizukuShellManager.currentStatus().permissionGranted) {
            return QuickToolsOperationResult.NeedsShizuku
        }
        val enable = !currentBluetoothEnabled(service)
        return ShizukuShellManager.execute(
            "cmd bluetooth_manager ${if (enable) "enable" else "disable"}"
        ).toOperationResult()
    }

    fun openWifiFallback(service: SideGestureService) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Panel.ACTION_INTERNET_CONNECTIVITY
        } else {
            Settings.ACTION_WIFI_SETTINGS
        }
        service.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openBluetoothFallback(service: SideGestureService) {
        service.startActivity(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openWriteSettings(service: SideGestureService) {
        service.gotoManageWriteSettings()
    }

    fun openNotificationListenerSettings(service: SideGestureService) {
        service.gotoNotificationListenerSettings()
    }

    fun dispatchMediaPlayPause(service: SideGestureService) {
        service.dispatchMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun dispatchMediaPrevious(service: SideGestureService) {
        service.dispatchMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun dispatchMediaNext(service: SideGestureService) {
        service.dispatchMediaKeyEvent(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    private fun com.aaron.sidegesture.platform.shell.ShellResult.toOperationResult(): QuickToolsOperationResult {
        return if (isSuccess) {
            QuickToolsOperationResult.Success
        } else {
            QuickToolsOperationResult.Failed(errorMessage.ifBlank { stderr })
        }
    }

    private class SystemQuickToolsBrightnessGateway(
        private val context: Context
    ) : QuickToolsBrightnessGateway {

        override fun readSnapshot(): QuickToolsBrightnessSnapshot {
            val range = brightnessRange()
            val rawValue = context.readSystemInt(
                Settings.System.SCREEN_BRIGHTNESS,
                (range.minimum + range.maximum) / 2
            )
            val mode = context.readSystemInt(
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            return QuickToolsBrightnessSnapshot(
                rawValue = rawValue,
                ratio = QuickToolsBrightnessMapping.rawToRatio(
                    rawValue = rawValue,
                    range = range,
                    sdkInt = Build.VERSION.SDK_INT
                ),
                autoEnabled = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                range = range,
                writeCapability = writeCapability()
            )
        }

        override fun observeChanges(onChanged: () -> Unit): AutoCloseable {
            val resolver = context.contentResolver
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    onChanged()
                }
            }
            return try {
                resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    false,
                    observer
                )
                resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                    false,
                    observer
                )
                AutoCloseable {
                    runCatching { resolver.unregisterContentObserver(observer) }
                }
            } catch (_: Exception) {
                runCatching { resolver.unregisterContentObserver(observer) }
                AutoCloseable { }
            }
        }

        override suspend fun setRatio(ratio: Float): QuickToolsBrightnessOperation =
            withContext(Dispatchers.IO) {
                val range = brightnessRange()
                val targetRawValue = QuickToolsBrightnessMapping.ratioToRaw(
                    ratio = ratio,
                    range = range,
                    sdkInt = Build.VERSION.SDK_INT
                )
                val writeResult = writeSystemInt(
                    name = Settings.System.SCREEN_BRIGHTNESS,
                    value = targetRawValue
                )
                val snapshot = readSnapshot()
                val result = if (
                    writeResult == QuickToolsOperationResult.Success &&
                    abs(snapshot.rawValue - targetRawValue) > 1
                ) {
                    QuickToolsOperationResult.PendingSystemSync
                } else {
                    writeResult
                }
                QuickToolsBrightnessOperation(result = result, snapshot = snapshot)
            }

        override suspend fun toggleAuto(): QuickToolsBrightnessOperation =
            withContext(Dispatchers.IO) {
                val currentSnapshot = readSnapshot()
                val targetMode = if (currentSnapshot.autoEnabled) {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                } else {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                }
                val writeResult = writeSystemInt(
                    name = Settings.System.SCREEN_BRIGHTNESS_MODE,
                    value = targetMode
                )
                val snapshot = readSnapshot()
                val targetAutoEnabled =
                    targetMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                val result = if (
                    writeResult == QuickToolsOperationResult.Success &&
                    snapshot.autoEnabled != targetAutoEnabled
                ) {
                    QuickToolsOperationResult.PendingSystemSync
                } else {
                    writeResult
                }
                QuickToolsBrightnessOperation(result = result, snapshot = snapshot)
            }

        private fun brightnessRange(): QuickToolsBrightnessRange {
            return QuickToolsBrightnessMapping.resolveRange(
                sdkInt = Build.VERSION.SDK_INT,
                configuredMinimum = androidInteger("config_screenBrightnessSettingMinimum"),
                configuredMaximum = androidInteger("config_screenBrightnessSettingMaximum")
            )
        }

        private fun androidInteger(name: String): Int? {
            val resourceId = context.resources.getIdentifier(name, "integer", "android")
            if (resourceId == 0) return null
            return runCatching { context.resources.getInteger(resourceId) }.getOrNull()
        }

        private fun writeCapability(): QuickToolsBrightnessWriteCapability {
            return when {
                context.canWriteSystemSettings() ->
                    QuickToolsBrightnessWriteCapability.WriteSettings
                ShizukuShellManager.currentStatus().permissionGranted ->
                    QuickToolsBrightnessWriteCapability.Shizuku
                else -> QuickToolsBrightnessWriteCapability.None
            }
        }

        private suspend fun writeSystemInt(
            name: String,
            value: Int
        ): QuickToolsOperationResult {
            if (context.canWriteSystemSettings()) {
                return runCatching {
                    if (Settings.System.putInt(context.contentResolver, name, value)) {
                        QuickToolsOperationResult.Success
                    } else {
                        QuickToolsOperationResult.Failed("System settings rejected the write")
                    }
                }.getOrElse { error ->
                    QuickToolsOperationResult.Failed(error.message.orEmpty())
                }
            }
            if (ShizukuShellManager.currentStatus().permissionGranted) {
                return ShizukuShellManager.execute(
                    "settings put system $name $value"
                ).toOperationResult()
            }
            return QuickToolsOperationResult.NeedsWriteSettingsOrShizuku
        }
    }
}

sealed interface QuickToolsOperationResult {
    data object Success : QuickToolsOperationResult
    data object PendingSystemSync : QuickToolsOperationResult
    data object Superseded : QuickToolsOperationResult
    data object NeedsShizuku : QuickToolsOperationResult
    data object NeedsWriteSettingsOrShizuku : QuickToolsOperationResult
    data class Failed(val message: String) : QuickToolsOperationResult
}
