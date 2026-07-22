package com.aaron.sidegesture.entity.global

import androidx.annotation.Keep
import com.aaron.sidegesture.constant.ActionSettingsDefaults.QuickLauncherColumns
import com.aaron.sidegesture.constant.ActionSettingsDefaults.QuickLauncherIconSizeDp
import com.aaron.sidegesture.constant.ActionSettingsDefaults.QuickLauncherRows
import com.aaron.sidegesture.constant.ActionSettingsDefaults.QuickLauncherTextSizeSp
import kotlinx.serialization.Serializable

@Serializable
@Keep
data class QuickLauncherSettings(
    val rows: Int = QuickLauncherRows,
    val columns: Int = QuickLauncherColumns,
    val iconSizeDp: Int = QuickLauncherIconSizeDp,
    val textSizeSp: Int = QuickLauncherTextSizeSp
) {

    companion object {
        const val MinRows = 1
        const val MaxRows = 6
        const val MinColumns = 2
        const val MaxColumns = 6
        const val MinIconSizeDp = 28
        const val MaxIconSizeDp = 56
        const val MinTextSizeSp = 9
        const val MaxTextSizeSp = 18
    }

    fun normalized(): QuickLauncherSettings {
        return copy(
            rows = rows.coerceIn(MinRows, MaxRows),
            columns = columns.coerceIn(MinColumns, MaxColumns),
            iconSizeDp = iconSizeDp.coerceIn(MinIconSizeDp, MaxIconSizeDp),
            textSizeSp = textSizeSp.coerceIn(MinTextSizeSp, MaxTextSizeSp)
        )
    }
}
