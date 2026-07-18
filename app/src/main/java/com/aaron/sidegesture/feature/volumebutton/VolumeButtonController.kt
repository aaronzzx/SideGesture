package com.aaron.sidegesture.feature.volumebutton

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import com.aaron.sidegesture.ktx.dispatchMediaKeyEvent
import com.aaron.sidegesture.ktx.volumeDown
import com.aaron.sidegesture.ktx.volumeUp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VolumeButtonController(
    private val service: SideGestureService,
    private val scope: CoroutineScope,
    private val settingsStore: ServiceSettingsStore
) {

    private var switchSongJob: Job? = null

    fun handle(event: KeyEvent?): Boolean {
        event ?: return false
        val settings = settingsStore.currentSnapshotOrNull() ?: return false
        val keyCode = event.keyCode
        val powerManager = service.getSystemService(Context.POWER_SERVICE) as PowerManager
        val audioManager = service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!settings.advancedSettings.volumeButtonSwitchSong ||
            !audioManager.isMusicActive ||
            powerManager.isInteractive ||
            keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
        ) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                switchSongJob = scope.launch {
                    delay(ViewConfiguration.getLongPressTimeout().toLong())
                    when (keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> {
                            service.dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                        }
                        KeyEvent.KEYCODE_VOLUME_DOWN -> {
                            service.dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                val isCompleted = switchSongJob?.isCompleted == true
                switchSongJob?.cancel()
                switchSongJob = null
                if (!isCompleted) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> service.volumeUp()
                        KeyEvent.KEYCODE_VOLUME_DOWN -> service.volumeDown()
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                switchSongJob?.cancel()
                switchSongJob = null
            }
        }
        return true
    }
}
