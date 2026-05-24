package com.aaron.sidegesture.ktx

import android.content.Context
import com.aaron.sidegesture.R
import com.aaron.sidegesture.shizuku.ShizukuShellManager

fun Context.shizukuStatusLabel(status: ShizukuShellManager.ShizukuStatus): String {
    return when {
        status.permissionGranted -> getString(R.string.shizuku_status_ready)
        status.binderAlive -> getString(R.string.shizuku_status_no_permission)
        status.installed -> getString(R.string.shizuku_status_not_running)
        else -> getString(R.string.shizuku_status_not_installed)
    }
}

fun Context.shizukuStatusSummary(status: ShizukuShellManager.ShizukuStatus): String {
    return getString(
        R.string.shizuku_status_summary,
        shizukuStatusLabel(status),
        status.executorLabel
    )
}
