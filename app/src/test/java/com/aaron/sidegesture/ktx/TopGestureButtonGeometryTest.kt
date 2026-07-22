package com.aaron.sidegesture.ktx

import androidx.compose.ui.unit.IntSize
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import org.junit.Assert.assertEquals
import org.junit.Test

class TopGestureButtonGeometryTest {

    @Test
    fun topBoundsUseFullScreenWidthAndStayAtYZero() {
        val button = button(Position.Top)

        val bounds = button.bounds(
            rootSize = IntSize(width = 1000, height = 2000),
            imePadding = 640
        )

        assertEquals(200f, bounds.left)
        assertEquals(0f, bounds.top)
        assertEquals(600f, bounds.right)
        assertEquals(24f, bounds.bottom)
    }

    @Test
    fun bottomBoundsRemainDirectlyAttachedToBottomEdge() {
        val button = button(Position.Bottom)

        val bounds = button.bounds(rootSize = IntSize(width = 1000, height = 2000))

        assertEquals(200f, bounds.left)
        assertEquals(1976f, bounds.top)
        assertEquals(600f, bounds.right)
        assertEquals(2000f, bounds.bottom)
    }

    private fun button(position: Position): GestureButton {
        return GestureButton(
            id = "test",
            position = position,
            enabled = true,
            start = 0.2f,
            end = 0.6f,
            width = 24,
            slideActions = GestureActions(),
            longSlideActions = GestureActions(),
            color = 0,
            alignRegion = false,
            excludeSystemGestureRects = false,
            limitMaxExcludeSystemGestureLength = true
        )
    }
}
