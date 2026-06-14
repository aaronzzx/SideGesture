package com.aaron.sidegesture.entity

import androidx.annotation.Keep

@Keep
data class RecentTask(
    val taskId: Int,
    val packageName: String,
    val label: String
)
