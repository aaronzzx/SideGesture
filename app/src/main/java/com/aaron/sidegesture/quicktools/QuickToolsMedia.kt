package com.aaron.sidegesture.quicktools

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aaron.sidegesture.ktx.isNotificationListenerEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class QuickToolsMediaInfo(
    val permissionGranted: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val artwork: Bitmap? = null
)

class QuickToolsMediaControllerState(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mediaSessionManager =
        appContext.getSystemService(MediaSessionManager::class.java)
    private val componentName =
        ComponentName(appContext, QuickToolsNotificationListenerService::class.java)

    var info by mutableStateOf(QuickToolsMediaInfo())
        private set

    private var controller: MediaController? = null
    private var progressJob: Job? = null
    private var basePositionMs by mutableLongStateOf(0L)
    private var baseRealtimeMs by mutableLongStateOf(0L)

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            syncCurrentController()
        }

        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
            syncCurrentController()
        }
    }

    private val activeSessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bindController(selectController(controllers ?: emptyList()))
        }

    init {
        refresh()
        runCatching {
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                componentName
            )
        }
    }

    fun refresh() {
        if (!appContext.isNotificationListenerEnabled(QuickToolsNotificationListenerService::class.java)) {
            bindController(null)
            info = QuickToolsMediaInfo(permissionGranted = false)
            return
        }
        bindController(selectController(mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()))
    }

    fun togglePlayPause() {
        controller?.transportControls?.run {
            if (info.isPlaying) pause() else play()
        }
    }

    fun skipPrevious() {
        controller?.transportControls?.skipToPrevious()
    }

    fun skipNext() {
        controller?.transportControls?.skipToNext()
    }

    fun release() {
        runCatching {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        }
        progressJob?.cancel()
        bindController(null)
        scope.cancel()
    }

    private fun bindController(target: MediaController?) {
        if (controller?.sessionToken == target?.sessionToken) {
            syncCurrentController()
            return
        }
        controller?.unregisterCallback(controllerCallback)
        controller = target
        controller?.registerCallback(controllerCallback)
        syncCurrentController()
    }

    private fun syncCurrentController() {
        val currentController = controller
        if (currentController == null) {
            progressJob?.cancel()
            info = info.copy(
                permissionGranted = true,
                title = "",
                artist = "",
                isPlaying = false,
                durationMs = 0L,
                positionMs = 0L,
                artwork = null
            )
            return
        }
        val metadata = currentController.metadata
        val playbackState = currentController.playbackState
        basePositionMs = playbackState?.position ?: 0L
        baseRealtimeMs = SystemClock.elapsedRealtime()
        val isPlaying = playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        info = QuickToolsMediaInfo(
            permissionGranted = true,
            title = metadata?.description?.title?.toString().orEmpty(),
            artist = metadata?.description?.subtitle?.toString().orEmpty(),
            isPlaying = isPlaying,
            durationMs = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = currentPosition(playbackState),
            artwork = metadata?.description?.iconBitmap
                ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
        )
        progressJob?.cancel()
        if (isPlaying) {
            progressJob = scope.launch {
                while (true) {
                    delay(1000)
                    info = info.copy(positionMs = currentPosition(controller?.playbackState))
                }
            }
        }
    }

    private fun currentPosition(playbackState: android.media.session.PlaybackState?): Long {
        val speed = playbackState?.playbackSpeed ?: 1f
        if (playbackState?.state != android.media.session.PlaybackState.STATE_PLAYING) {
            return basePositionMs
        }
        val elapsed = SystemClock.elapsedRealtime() - baseRealtimeMs
        return (basePositionMs + elapsed * speed).toLong().coerceAtLeast(0L)
    }

    private fun selectController(controllers: List<MediaController>): MediaController? {
        return controllers.firstOrNull {
            it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull()
    }
}

@Composable
fun rememberQuickToolsMediaControllerState(): QuickToolsMediaControllerState {
    val context = LocalContext.current
    val state = remember(context) {
        QuickToolsMediaControllerState(context)
    }
    DisposableEffect(state) {
        onDispose {
            state.release()
        }
    }
    return state
}
