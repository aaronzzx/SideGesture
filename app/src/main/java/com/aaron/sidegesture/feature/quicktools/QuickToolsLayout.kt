package com.aaron.sidegesture.feature.quicktools

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aaron.sidegesture.entity.global.QuickToolType

internal object QuickToolsGridSpec {
    const val Columns = 4
    const val ViewportRows = 6

    val PanelWidth = 240.dp
    val PanelHeight = 300.dp
    val PanelCornerRadius = 28.dp
    val PanelOuterPadding = 0.dp
    val PanelInnerPadding = 10.dp
    val ItemSpacing = 8.dp

    val ViewportHeight = PanelHeight - (PanelInnerPadding * 2f)
    val RowHeight = (ViewportHeight - (ItemSpacing * (ViewportRows - 1).toFloat())) / ViewportRows
    val CompactButtonSize = minOf(RowHeight, 40.dp)
}

internal data class QuickToolLayoutSpan(
    val columnSpan: Int,
    val rowSpan: Int
) {
    fun itemHeight(): Dp {
        return (QuickToolsGridSpec.RowHeight * rowSpan.toFloat()) +
            (QuickToolsGridSpec.ItemSpacing * (rowSpan - 1).toFloat())
    }
}

internal fun QuickToolType.layoutSpan(): QuickToolLayoutSpan {
    return when (this) {
        QuickToolType.MediaControl -> QuickToolLayoutSpan(columnSpan = QuickToolsGridSpec.Columns, rowSpan = 2)
        QuickToolType.Brightness,
        QuickToolType.Volume -> QuickToolLayoutSpan(columnSpan = QuickToolsGridSpec.Columns, rowSpan = 1)
        QuickToolType.Flashlight,
        QuickToolType.Mute,
        QuickToolType.Wifi,
        QuickToolType.Bluetooth,
        QuickToolType.NotificationPanel,
        QuickToolType.QuickSettingsPanel,
        QuickToolType.LockScreen,
        QuickToolType.Screenshot -> QuickToolLayoutSpan(columnSpan = 1, rowSpan = 1)
    }
}
