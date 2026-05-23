package com.aaron.sidegesture.shizuku

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShellResult(
    val exitCode: Int = -1,
    val stdout: String = "",
    val stderr: String = "",
    val timedOut: Boolean = false,
    val errorMessage: String = ""
) : Parcelable {

    val isSuccess: Boolean
        get() = !timedOut && errorMessage.isEmpty() && exitCode == 0
}
