package com.aaron.sidegesture.ktx

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.feature.servicesettings.RestoreDigest
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopDataStoreInstrumentedTest {

    @Test
    fun topStoreIsIndependentAndResetAllRestoresEmptyDefault() = runBlocking {
        val original = RestoreDigest.readPayload()
        val top = GestureButton(
            id = "top-store-test",
            position = Position.Top,
            enabled = true,
            start = 0.1f,
            end = 0.9f,
            width = 36,
            slideActions = GestureActions(),
            longSlideActions = GestureActions(),
            color = 0,
            alignRegion = false,
            excludeSystemGestureRects = false,
            limitMaxExcludeSystemGestureLength = true
        )
        try {
            DataStoreHolder.topGestureButtons.updateData { listOf(top) }

            assertEquals(listOf(top), DataStoreHolder.topGestureButtons.data.first())
            assertEquals(original.bottomGestureButtons, DataStoreHolder.bottomGestureButtons.data.first())

            DataStoreHolder.resetAll()

            assertTrue(DataStoreHolder.topGestureButtons.data.first().isEmpty())
        } finally {
            DataStoreHolder.initialSettings.updateData { original.initialSettings }
            DataStoreHolder.advancedSettings.updateData { original.advancedSettings }
            DataStoreHolder.gestureSettings.updateData { original.gestureSettings }
            DataStoreHolder.actionSettings.updateData { original.actionSettings }
            DataStoreHolder.sideGestureButtons.updateData { original.sideGestureButtons }
            DataStoreHolder.bottomGestureButtons.updateData { original.bottomGestureButtons }
            DataStoreHolder.topGestureButtons.updateData { original.topGestureButtons }
        }
    }
}
