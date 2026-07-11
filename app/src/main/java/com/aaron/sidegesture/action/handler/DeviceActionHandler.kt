package com.aaron.sidegesture.action.handler

import android.Manifest
import android.content.Context
import android.os.PowerManager
import com.aaron.sidegesture.R
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionHandler
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.ktx.gotoAppDetailSettings
import com.aaron.sidegesture.utils.FlashlightController
import com.aaron.sidegesture.utils.showToast
import com.blankj.utilcode.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceActionHandler(
    private val service: SideGestureService,
    private val scope: CoroutineScope
) : ActionHandler {

    override val supportedActions = setOf(
        GlobalActions.FLASHLIGHT,
        GlobalActions.KEEP_SCREEN_ON
    )

    private var wakeLock: PowerManager.WakeLock? = null

    init {
        scope.coroutineContext[Job]?.invokeOnCompletion {
            wakeLock?.takeIf { it.isHeld }?.release()
            wakeLock = null
        }
    }

    override suspend fun handle(request: ActionRequest) {
        when (request.action.value) {
            GlobalActions.FLASHLIGHT -> toggleFlashlight()
            GlobalActions.KEEP_SCREEN_ON -> toggleKeepScreenOn()
        }
    }

    private suspend fun toggleFlashlight() {
        if (!FlashlightController.isAvailable(service)) {
            showToast(R.string.flashlight_failed)
            return
        }
        if (!PermissionUtils.isGranted(Manifest.permission.CAMERA)) {
            showToast(R.string.grant_camera_permission)
            PermissionUtils.permission(Manifest.permission.CAMERA)
                .callback { granted, _, deniedForever, _ ->
                    if (granted) {
                        scope.launch(Dispatchers.Default) {
                            if (!FlashlightController.toggle(service)) {
                                showToast(R.string.flashlight_failed)
                            }
                        }
                    } else if (deniedForever.isNotEmpty()) {
                        showToast(R.string.goto_grant_camera_permission)
                        service.gotoAppDetailSettings()
                    }
                }
                .request()
            return
        }
        val success = withContext(Dispatchers.Default) {
            FlashlightController.toggle(service)
        }
        if (!success) {
            showToast(R.string.flashlight_failed)
        }
    }

    private fun toggleKeepScreenOn() {
        val current = wakeLock
        if (current != null) {
            current.release()
            wakeLock = null
            showToast(R.string.disable_keep_screen_on)
            return
        }
        val powerManager = service.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager
            .newWakeLock(PowerManager.FULL_WAKE_LOCK, "gulugulu:KeepScreenOn")
            .apply { acquire() }
        showToast(R.string.enable_keep_screen_on)
    }
}
