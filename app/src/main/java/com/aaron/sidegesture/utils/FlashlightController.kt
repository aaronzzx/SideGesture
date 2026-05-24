package com.aaron.sidegesture.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

object FlashlightController {

    @Volatile
    private var cameraId: String? = null

    @Volatile
    private var initialized = false

    @Volatile
    private var torchEnabled = false

    @Volatile
    private var callbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == this@FlashlightController.cameraId) {
                torchEnabled = enabled
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == this@FlashlightController.cameraId) {
                torchEnabled = false
            }
        }
    }

    fun isAvailable(context: Context): Boolean {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }
        if (!appContext.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)) {
            return false
        }
        ensureInitialized(appContext)
        return cameraId != null
    }

    fun isEnabled(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!isAvailable(appContext)) {
            return false
        }
        ensureCallbackRegistered(appContext)
        return torchEnabled
    }

    fun toggle(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!isAvailable(appContext)) {
            return false
        }
        val target = !torchEnabled
        return setEnabled(appContext, target)
    }

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        val appContext = context.applicationContext
        if (!isAvailable(appContext)) {
            return false
        }
        val targetCameraId = cameraId ?: return false
        val cameraManager = appContext.getSystemService(CameraManager::class.java) ?: return false
        return runCatching {
            ensureCallbackRegistered(appContext)
            cameraManager.setTorchMode(targetCameraId, enabled)
            torchEnabled = enabled
            true
        }.getOrDefault(false)
    }

    private fun ensureInitialized(context: Context) {
        if (initialized) {
            return
        }
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return
        cameraId = runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val flashAvailable =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                flashAvailable && lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
        initialized = true
    }

    private fun ensureCallbackRegistered(context: Context) {
        if (callbackRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return
        runCatching {
            cameraManager.registerTorchCallback(torchCallback, null)
            callbackRegistered = true
        }
    }
}
