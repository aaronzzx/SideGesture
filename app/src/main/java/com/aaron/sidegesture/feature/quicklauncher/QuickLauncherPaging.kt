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

fun calculateQuickLauncherPageLayout(
    itemCount: Int,
    availableHeight: Float,
    minPanelHeight: Float,
    maxPanelHeight: Float,
    itemHeight: Float,
    rowSpacing: Float,
    contentPadding: Float,
    indicatorHeight: Float,
    indicatorSpacing: Float,
    columns: Int
): QuickLauncherPageLayout {
    val safeItemCount = itemCount.coerceAtLeast(0)
    val safeColumns = columns.coerceAtLeast(1)
    val heightLimit = availableHeight.coerceAtLeast(0f).coerceAtMost(maxPanelHeight)
    val effectiveMinHeight = minPanelHeight.coerceAtMost(heightLimit)
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

    val rowsWithoutIndicator = requiredRows.coerceAtMost(rowsThatFit(0f))
    val needsPageIndicator = safeItemCount > rowsWithoutIndicator * safeColumns
    val indicatorReservedHeight = if (needsPageIndicator) {
        indicatorHeight + indicatorSpacing
    } else {
        0f
    }
    val rowCount = requiredRows.coerceAtMost(rowsThatFit(indicatorReservedHeight))
    val itemsPerPage = rowCount * safeColumns
    val pageCount = ceil(safeItemCount.toFloat() / itemsPerPage)
        .toInt()
        .coerceAtLeast(1)
    val showPageIndicator = pageCount > 1
    val desiredHeight = contentPadding * 2f +
        rowCount * itemHeight +
        (rowCount - 1) * rowSpacing +
        if (showPageIndicator) indicatorHeight + indicatorSpacing else 0f
    val panelHeight = desiredHeight.coerceIn(effectiveMinHeight, heightLimit)

    return QuickLauncherPageLayout(
        rowCount = rowCount,
        itemsPerPage = itemsPerPage,
        pageCount = pageCount,
        panelHeight = panelHeight,
        showPageIndicator = showPageIndicator
    )
}

fun <T> buildQuickLauncherPages(items: List<T>, itemsPerPage: Int): List<List<T>> {
    if (items.isEmpty()) return listOf(emptyList())
    return items.chunked(itemsPerPage.coerceAtLeast(1))
}
