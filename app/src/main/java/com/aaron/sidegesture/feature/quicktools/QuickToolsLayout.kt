package com.aaron.sidegesture.feature.quicktools

import androidx.compose.ui.unit.Dp
import com.aaron.sidegesture.entity.global.QuickToolType
import com.aaron.sidegesture.ui.theme.QuickToolsDimensions

object QuickToolsGridSpec {
    const val Columns = 4
    const val ViewportRows = 6
}

data class QuickToolsLayoutMetrics(
    val panelWidth: Dp,
    val panelHeight: Dp,
    val panelOuterPadding: Dp,
    val panelInnerPadding: Dp,
    val itemSpacing: Dp,
    val rowHeight: Dp,
    val compactButtonSize: Dp
)

fun calculateQuickToolsLayout(dimensions: QuickToolsDimensions): QuickToolsLayoutMetrics {
    val viewportHeight = dimensions.panelHeight - (dimensions.panelInnerPadding * 2f)
    val rowHeight = (
        viewportHeight -
            (dimensions.itemSpacing * (QuickToolsGridSpec.ViewportRows - 1).toFloat())
        ) / QuickToolsGridSpec.ViewportRows
    return QuickToolsLayoutMetrics(
        panelWidth = dimensions.panelWidth,
        panelHeight = dimensions.panelHeight,
        panelOuterPadding = dimensions.panelOuterPadding,
        panelInnerPadding = dimensions.panelInnerPadding,
        itemSpacing = dimensions.itemSpacing,
        rowHeight = rowHeight,
        compactButtonSize = minOf(rowHeight, dimensions.compactButtonMaxSize)
    )
}

data class QuickToolLayoutSpan(
    val columnSpan: Int,
    val rowSpan: Int
) {
    fun itemHeight(layout: QuickToolsLayoutMetrics): Dp {
        return (layout.rowHeight * rowSpan.toFloat()) +
            (layout.itemSpacing * (rowSpan - 1).toFloat())
    }
}

fun QuickToolType.layoutSpan(): QuickToolLayoutSpan {
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
