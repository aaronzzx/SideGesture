package com.aaron.sidegesture.quicktools

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.ktx.canWriteSystemSettings
import com.aaron.sidegesture.ktx.dispatchMediaKeyEvent
import com.aaron.sidegesture.ktx.gotoManageWriteSettings
import com.aaron.sidegesture.ktx.gotoNotificationListenerSettings
import com.aaron.sidegesture.ktx.readGlobalInt
import com.aaron.sidegesture.ktx.readSystemInt
import com.aaron.sidegesture.shizuku.ShizukuShellManager
import com.aaron.sidegesture.utils.FlashlightController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

object QuickToolsExecutor {

    fun currentBrightnessRatio(service: SideGestureService): Float {
        val value = service.readSystemInt(Settings.System.SCREEN_BRIGHTNESS, 128)
        return (value / 255f).coerceIn(0f, 1f)
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

    suspend fun setBrightnessRatio(
        service: SideGestureService,
        ratio: Float
    ): QuickToolsOperationResult = withContext(Dispatchers.IO) {
        val value = (ratio.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        if (service.canWriteSystemSettings()) {
            Settings.System.putInt(service.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
            Settings.System.putInt(service.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            return@withContext QuickToolsOperationResult.Success
        }
        if (ShizukuShellManager.currentStatus().permissionGranted) {
            val result = ShizukuShellManager.execute(
                "settings put system screen_brightness_mode 0 && settings put system screen_brightness $value"
            )
            return@withContext result.toOperationResult()
        }
        QuickToolsOperationResult.NeedsWriteSettingsOrShizuku
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

    fun performExistingAction(service: SideGestureService, actionValue: String) {
        service.performAction(Action(value = actionValue))
    }

    private fun com.aaron.sidegesture.shizuku.ShellResult.toOperationResult(): QuickToolsOperationResult {
        return if (isSuccess) {
            QuickToolsOperationResult.Success
        } else {
            QuickToolsOperationResult.Failed(errorMessage.ifBlank { stderr })
        }
    }
}

sealed interface QuickToolsOperationResult {
    data object Success : QuickToolsOperationResult
    data object NeedsShizuku : QuickToolsOperationResult
    data object NeedsWriteSettingsOrShizuku : QuickToolsOperationResult
    data class Failed(val message: String) : QuickToolsOperationResult
}
