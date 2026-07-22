package com.aaron.sidegesture.ktx

import com.aaron.sidegesture.entity.GestureAngle
import com.aaron.sidegesture.entity.GestureAngles
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.global.Backup
import com.aaron.sidegesture.utils.JsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopConfigurationSerializationTest {

    @Test
    fun oldAnglesJsonGetsIndependentTopDefault() {
        val angles = JsonHelper.decodeFromString<GestureAngles>(
            """{"bottom":{"p1":0.1,"p2":0.2,"p3":0.8,"p4":0.9}}"""
        )

        assertEquals(GestureAngle(0.1f, 0.2f, 0.8f, 0.9f), angles.bottom)
        assertEquals(GestureAngle(0.12f, 0.40f, 0.60f, 0.88f), angles.top)
        assertTrue(angles.top !== angles.bottom)
    }

    @Test
    fun oldBackupWithoutTopFieldRemainsDistinguishable() {
        val backup = JsonHelper.decodeFromString<Backup>("{}")

        assertNull(backup.topGestureButtons)
    }

    @Test
    fun positionTopRoundTripsWithoutChangingExistingEnumNames() {
        val encoded = JsonHelper.encodeToString(Position.Top)

        assertEquals("\"Top\"", encoded)
        assertEquals(Position.Top, JsonHelper.decodeFromString<Position>(encoded))
        assertEquals(listOf("Left", "Right", "Bottom", "Top"), Position.entries.map { it.name })
    }
}
