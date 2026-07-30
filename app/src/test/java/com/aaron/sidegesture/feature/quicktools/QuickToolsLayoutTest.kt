package com.aaron.sidegesture.feature.quicktools

import com.aaron.sidegesture.entity.global.QuickToolType
import com.aaron.sidegesture.ui.theme.QuickToolsDimensions
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickToolsLayoutTest {

    private val layout = calculateQuickToolsLayout(QuickToolsDimensions())

    @Test
    fun mediaControlUsesFourColumnsAndTwoRows() {
        val span = QuickToolType.MediaControl.layoutSpan()

        assertEquals(4, span.columnSpan)
        assertEquals(2, span.rowSpan)
        assertEquals(
            layout.rowHeight * 2f + layout.itemSpacing,
            span.itemHeight(layout)
        )
    }

    @Test
    fun brightnessAndVolumeUseFourColumnsAndOneRow() {
        listOf(QuickToolType.Brightness, QuickToolType.Volume).forEach { type ->
            val span = type.layoutSpan()

            assertEquals(4, span.columnSpan)
            assertEquals(1, span.rowSpan)
            assertEquals(layout.rowHeight, span.itemHeight(layout))
        }
    }

    @Test
    fun otherToolsUseOneColumnAndOneRow() {
        listOf(
            QuickToolType.Flashlight,
            QuickToolType.Mute,
            QuickToolType.Wifi,
            QuickToolType.Bluetooth,
            QuickToolType.NotificationPanel,
            QuickToolType.QuickSettingsPanel,
            QuickToolType.LockScreen,
            QuickToolType.Screenshot
        ).forEach { type ->
            val span = type.layoutSpan()

            assertEquals(1, span.columnSpan)
            assertEquals(1, span.rowSpan)
            assertEquals(layout.rowHeight, span.itemHeight(layout))
        }
    }
}
