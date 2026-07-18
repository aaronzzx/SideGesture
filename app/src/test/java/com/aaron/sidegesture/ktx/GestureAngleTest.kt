package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.TriggerDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GestureAngleTest {

    private val angle = GestureAngle(p1 = 0.1f, p2 = 0.3f, p3 = 0.7f, p4 = 0.9f)

    @Test
    fun getTriggerDirectionMapsBoundariesAndDirections() {
        assertEquals(TriggerDirection.Up2, angle.getTriggerDirection(17.9f))
        assertEquals(TriggerDirection.Up, angle.getTriggerDirection(18f))
        assertEquals(TriggerDirection.Up, angle.getTriggerDirection(54f))
        assertEquals(TriggerDirection.Center, angle.getTriggerDirection(54.1f))
        assertEquals(TriggerDirection.Center, angle.getTriggerDirection(126f))
        assertEquals(TriggerDirection.Down, angle.getTriggerDirection(126.1f))
        assertEquals(TriggerDirection.Down, angle.getTriggerDirection(162f))
        assertEquals(TriggerDirection.Down2, angle.getTriggerDirection(162.1f))
    }

    @Test
    fun getTriggerDirectionMapsNaNToCenter2() {
        assertEquals(TriggerDirection.Center2, angle.getTriggerDirection(Float.NaN))
    }

    @Test
    fun copyNewClampsEachPointToConfiguredGaps() {
        val minGap = 0.05f

        assertFloatEquals(0.05f, angle.copyNew("p1", 0f, minGap).p1)
        assertFloatEquals(0.2f, angle.copyNew("p1", 0.8f, minGap).p1)
        assertFloatEquals(0.2f, angle.copyNew("p2", 0f, minGap).p2)
        assertFloatEquals(0.45f, angle.copyNew("p2", 0.8f, minGap).p2)
        assertFloatEquals(0.55f, angle.copyNew("p3", 0f, minGap).p3)
        assertFloatEquals(0.8f, angle.copyNew("p3", 0.99f, minGap).p3)
        assertFloatEquals(0.8f, angle.copyNew("p4", 0f, minGap).p4)
        assertFloatEquals(0.95f, angle.copyNew("p4", 1f, minGap).p4)
    }

    @Test
    fun copyNewReturnsOriginalForUnknownField() {
        assertSame(angle, angle.copyNew("unknown", 0.5f, 0.05f))
    }

    private fun assertFloatEquals(expected: Float, actual: Float) {
        assertEquals(expected, actual, 0.0001f)
    }
}
