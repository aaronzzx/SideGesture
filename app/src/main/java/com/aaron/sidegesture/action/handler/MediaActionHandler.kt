package com.aaron.sidegesture.action.handler

import android.view.KeyEvent
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.ktx.dispatchMediaKeyEvent
import com.aaron.sidegesture.ktx.toggleMute
import com.aaron.sidegesture.ktx.volumeDown
import com.aaron.sidegesture.ktx.volumeUp

class MediaActionHandler(
    private val service: SideGestureService
) : ActionHandler {

    override val supportedActions = setOf(
        GlobalActions.VOLUME_UP,
        GlobalActions.VOLUME_DOWN,
        GlobalActions.MUTE,
        GlobalActions.PLAY_PAUSE_SONG,
        GlobalActions.LAST_SONG,
        GlobalActions.NEXT_SONG
    )

    override suspend fun handle(request: ActionRequest) {
        when (request.action.value) {
            GlobalActions.VOLUME_UP -> service.volumeUp()
            GlobalActions.VOLUME_DOWN -> service.volumeDown()
            GlobalActions.MUTE -> service.toggleMute()
            GlobalActions.PLAY_PAUSE_SONG -> service.dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            GlobalActions.LAST_SONG -> service.dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            GlobalActions.NEXT_SONG -> service.dispatchMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }
}
