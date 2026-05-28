package com.aaron.sidegesture.entity

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class QuickLauncherActionData(
    val items: List<Action> = emptyList()
)
