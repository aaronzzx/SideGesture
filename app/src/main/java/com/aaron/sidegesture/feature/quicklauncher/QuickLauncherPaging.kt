package com.aaron.sidegesture.feature.quicklauncher

import kotlin.math.ceil
import kotlin.math.floor

data class QuickLauncherPageLayout(
    val rowCount: Int,
    val itemsPerPage: Int,
    val pageCount: Int,
    val panelHeight: Float,
    val showPageIndicator: Boolean
)

data class QuickLauncherHorizontalLayout(
    val panelWidth: Float,
    val iconSize: Float
)

fun calculateQuickLauncherPageLayout(
    itemCount: Int,
    availableHeight: Float,
    maxPanelHeight: Float,
    itemHeight: Float,
    rowSpacing: Float,
    contentPadding: Float,
    indicatorHeight: Float,
    indicatorSpacing: Float,
    columns: Int,
    maxRows: Int
): QuickLauncherPageLayout {
    val safeItemCount = itemCount.coerceAtLeast(0)
    val safeColumns = columns.coerceAtLeast(1)
    val safeMaxRows = maxRows.coerceAtLeast(1)
    val heightLimit = availableHeight.coerceAtLeast(0f).coerceAtMost(maxPanelHeight)
    val requiredRows = ceil(safeItemCount.toFloat() / safeColumns)
        .toInt()
        .coerceAtLeast(1)

    fun rowsThatFit(reservedHeight: Float): Int {
        val gridHeight = (
            heightLimit - contentPadding * 2f - reservedHeight
        ).coerceAtLeast(0f)
        return floor((gridHeight + rowSpacing) / (itemHeight + rowSpacing))
            .toInt()
            .coerceAtLeast(1)
    }

    val rowsWithoutIndicator = requiredRows
        .coerceAtMost(safeMaxRows)
        .coerceAtMost(rowsThatFit(0f))
    val needsPageIndicator = safeItemCount > rowsWithoutIndicator * safeColumns
    val indicatorReservedHeight = if (needsPageIndicator) {
        indicatorHeight + indicatorSpacing
    } else {
        0f
    }
    val rowCount = requiredRows
        .coerceAtMost(safeMaxRows)
        .coerceAtMost(rowsThatFit(indicatorReservedHeight))
    val itemsPerPage = rowCount * safeColumns
    val pageCount = ceil(safeItemCount.toFloat() / itemsPerPage)
        .toInt()
        .coerceAtLeast(1)
    val showPageIndicator = pageCount > 1
    val desiredHeight = contentPadding * 2f +
        rowCount * itemHeight +
        (rowCount - 1) * rowSpacing +
        if (showPageIndicator) indicatorHeight + indicatorSpacing else 0f
    val panelHeight = desiredHeight.coerceAtMost(heightLimit)

    return QuickLauncherPageLayout(
        rowCount = rowCount,
        itemsPerPage = itemsPerPage,
        pageCount = pageCount,
        panelHeight = panelHeight,
        showPageIndicator = showPageIndicator
    )
}

fun calculateQuickLauncherHorizontalLayout(
    columns: Int,
    requestedIconSize: Float,
    availableWidth: Float,
    itemHorizontalPadding: Float,
    itemSpacing: Float,
    contentPadding: Float
): QuickLauncherHorizontalLayout {
    val safeColumns = columns.coerceAtLeast(1)
    val safeIconSize = requestedIconSize.coerceAtLeast(0f)
    val safeAvailableWidth = availableWidth.coerceAtLeast(0f)
    val desiredWidth = contentPadding.coerceAtLeast(0f) * 2f +
        safeColumns * (safeIconSize + itemHorizontalPadding.coerceAtLeast(0f) * 2f) +
        (safeColumns - 1) * itemSpacing.coerceAtLeast(0f)
    val panelWidth = desiredWidth.coerceAtMost(safeAvailableWidth)
    val availableCellWidth = (
        panelWidth - contentPadding.coerceAtLeast(0f) * 2f -
            (safeColumns - 1) * itemSpacing.coerceAtLeast(0f)
    ).coerceAtLeast(0f) / safeColumns
    val iconSize = safeIconSize.coerceAtMost(
        (availableCellWidth - itemHorizontalPadding.coerceAtLeast(0f) * 2f).coerceAtLeast(0f)
    )
    return QuickLauncherHorizontalLayout(
        panelWidth = panelWidth,
        iconSize = iconSize
    )
}

fun <T> buildQuickLauncherPages(items: List<T>, itemsPerPage: Int): List<List<T>> {
    if (items.isEmpty()) return listOf(emptyList())
    return items.chunked(itemsPerPage.coerceAtLeast(1))
}
