package com.aaron.sidegesture.feature.quicklauncher

import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.QuickLauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickLauncherPagingTest {

    @Test
    fun sixteenItemsFitOneFourRowPageWithoutIndicator() {
        val layout = calculateQuickLauncherPageLayout(itemCount = 16)

        assertEquals(4, layout.rowCount)
        assertEquals(16, layout.itemsPerPage)
        assertEquals(1, layout.pageCount)
        assertEquals(328f, layout.panelHeight, 0f)
        assertFalse(layout.showPageIndicator)
    }

    @Test
    fun seventeenthItemCreatesSecondPageAndReservesIndicatorHeight() {
        val layout = calculateQuickLauncherPageLayout(itemCount = 17)

        assertEquals(4, layout.rowCount)
        assertEquals(16, layout.itemsPerPage)
        assertEquals(2, layout.pageCount)
        assertEquals(344f, layout.panelHeight, 0f)
        assertTrue(layout.showPageIndicator)
    }

    @Test
    fun shortAvailableHeightReducesCapacityWithoutExceedingBounds() {
        val layout = calculateQuickLauncherPageLayout(
            itemCount = 9,
            availableHeight = 160f
        )

        assertEquals(1, layout.rowCount)
        assertEquals(4, layout.itemsPerPage)
        assertEquals(3, layout.pageCount)
        assertEquals(110f, layout.panelHeight, 0f)
        assertTrue(layout.showPageIndicator)
    }

    @Test
    fun largerFontRowHeightReducesCapacityAndKeepsPanelWithinMaximum() {
        val layout = calculateQuickLauncherPageLayout(
            itemCount = 17,
            availableHeight = 1_000f,
            itemHeight = 90f
        )

        assertEquals(3, layout.rowCount)
        assertEquals(12, layout.itemsPerPage)
        assertEquals(2, layout.pageCount)
        assertEquals(326f, layout.panelHeight, 0f)
        assertTrue(layout.panelHeight <= 360f)
    }

    @Test
    fun customRowsAndColumnsSetPageCapacity() {
        val layout = calculateQuickLauncherPageLayout(
            itemCount = 13,
            columns = 3,
            maxRows = 2
        )

        assertEquals(2, layout.rowCount)
        assertEquals(6, layout.itemsPerPage)
        assertEquals(3, layout.pageCount)
        assertTrue(layout.showPageIndicator)
    }

    @Test
    fun defaultHorizontalLayoutKeepsOriginalFourColumnWidth() {
        val layout = calculateQuickLauncherHorizontalLayout(
            columns = 4,
            requestedIconSize = 44f,
            availableWidth = 1_000f,
            itemHorizontalPadding = 6f,
            itemSpacing = 4f,
            contentPadding = 12f
        )

        assertEquals(260f, layout.panelWidth, 0f)
        assertEquals(44f, layout.iconSize, 0f)
    }

    @Test
    fun compactRowsAndColumnsShrinkPanelToContent() {
        val verticalLayout = calculateQuickLauncherPageLayout(
            itemCount = 20,
            columns = 2,
            maxRows = 1
        )
        val horizontalLayout = calculateQuickLauncherHorizontalLayout(
            columns = 2,
            requestedIconSize = 44f,
            availableWidth = 1_000f,
            itemHorizontalPadding = 6f,
            itemSpacing = 4f,
            contentPadding = 12f
        )

        assertEquals(110f, verticalLayout.panelHeight, 0f)
        assertEquals(140f, horizontalLayout.panelWidth, 0f)
    }

    @Test
    fun wideSixColumnLayoutExpandsPanelWithoutShrinkingIcon() {
        val layout = calculateQuickLauncherHorizontalLayout(
            columns = 6,
            requestedIconSize = 56f,
            availableWidth = 1_000f,
            itemHorizontalPadding = 6f,
            itemSpacing = 4f,
            contentPadding = 12f
        )

        assertEquals(452f, layout.panelWidth, 0f)
        assertEquals(56f, layout.iconSize, 0f)
    }

    @Test
    fun narrowSafeWidthCapsPanelAndShrinksIconToFit() {
        val layout = calculateQuickLauncherHorizontalLayout(
            columns = 6,
            requestedIconSize = 56f,
            availableWidth = 300f,
            itemHorizontalPadding = 6f,
            itemSpacing = 4f,
            contentPadding = 12f
        )

        assertEquals(300f, layout.panelWidth, 0f)
        assertEquals(30.666666f, layout.iconSize, 0.0001f)
        assertTrue(layout.iconSize < 56f)
    }

    @Test
    fun invalidQuickLauncherSettingsAreNormalizedToSupportedBounds() {
        val settings = QuickLauncherSettings(
            rows = 0,
            columns = 99,
            iconSizeDp = 0,
            textSizeSp = 99
        ).normalized()

        assertEquals(QuickLauncherSettings.MinRows, settings.rows)
        assertEquals(QuickLauncherSettings.MaxColumns, settings.columns)
        assertEquals(QuickLauncherSettings.MinIconSizeDp, settings.iconSizeDp)
        assertEquals(QuickLauncherSettings.MaxTextSizeSp, settings.textSizeSp)
    }

    @Test
    fun emptyItemsKeepOneStablePageWithoutIndicator() {
        val layout = calculateQuickLauncherPageLayout(itemCount = 0)
        val pages = buildQuickLauncherPages(emptyList<Int>(), layout.itemsPerPage)

        assertEquals(1, layout.pageCount)
        assertEquals(listOf(emptyList<Int>()), pages)
        assertFalse(layout.showPageIndicator)
    }

    @Test
    fun pagesPreserveOriginalOrderAndIncompleteLastPage() {
        val pages = buildQuickLauncherPages((1..18).toList(), itemsPerPage = 16)

        assertEquals((1..16).toList(), pages[0])
        assertEquals(listOf(17, 18), pages[1])
    }

    @Test
    fun everyShowStartsANewPanelSession() {
        val state = QuickLauncherPanelState()
        val firstItems = mutableListOf(Action(value = "first"))
        val secondItems = listOf(Action(value = "second"))

        state.show(firstItems, Offset(10f, 20f), Position.Left)
        val firstSessionId = state.sessionId
        firstItems += Action(value = "mutated-after-show")
        assertEquals(listOf(Action(value = "first")), state.items)
        state.hide()
        state.show(secondItems, Offset(30f, 40f), Position.Right)

        assertEquals(firstSessionId + 1, state.sessionId)
        assertEquals(secondItems, state.items)
        assertEquals(Offset(30f, 40f), state.fingerAnchor)
        assertEquals(Position.Right, state.triggerEdge)
        assertTrue(state.visible)
    }

    private fun calculateQuickLauncherPageLayout(
        itemCount: Int,
        availableHeight: Float = 1_000f,
        itemHeight: Float = 70f,
        columns: Int = 4,
        maxRows: Int = 4
    ): QuickLauncherPageLayout {
        return calculateQuickLauncherPageLayout(
            itemCount = itemCount,
            availableHeight = availableHeight,
            maxPanelHeight = 360f,
            itemHeight = itemHeight,
            rowSpacing = 8f,
            contentPadding = 12f,
            indicatorHeight = 8f,
            indicatorSpacing = 8f,
            columns = columns,
            maxRows = maxRows
        )
    }
}
