package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.TriggerDirection
import com.aaron.sidegesture.utils.JsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureConfigurationSerializationTest {

    @Test
    fun oldGestureActionsJsonDefaultsDoubleClickToEmptyWithoutChangingClick() {
        val actions = JsonHelper.decodeFromString<GestureActions>(
            """{"click":[{"value":"click","data":""}]}"""
        )

        assertEquals(listOf(Action("click")), actions.click)
        assertTrue(actions.doubleClick.isEmpty())
    }

    @Test
    fun newDoubleTapConfigurationRoundTripsIndependentlyFromSingleTap() {
        val expected = GestureActions(
            click = listOf(Action("click")),
            doubleClick = listOf(Action("double"))
        )

        val decoded = JsonHelper.decodeFromString<GestureActions>(
            JsonHelper.encodeToString(expected)
        )

        assertEquals(expected, decoded)
        assertEquals(expected.click, decoded.actionsBy(TriggerDirection.Click))
        assertEquals(expected.doubleClick, decoded.actionsBy(TriggerDirection.DoubleClick))
        assertEquals(
            expected.copy(doubleClick = listOf(Action("replacement"))),
            expected.copyActionsBy(
                direction = TriggerDirection.DoubleClick,
                actions = listOf(Action("replacement"))
            )
        )
        assertEquals(
            TriggerDirection.DoubleClick,
            JsonHelper.decodeFromString<TriggerDirection>(
                JsonHelper.encodeToString(TriggerDirection.DoubleClick)
            )
        )
    }

}
