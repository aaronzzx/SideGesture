package com.aaron.sidegesture.ktx

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aaron.sidegesture.constant.GlobalSettings
import com.aaron.sidegesture.entity.GestureActions
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.feature.servicesettings.RestoreDigest
import com.aaron.sidegesture.utils.DataStoreHolder
import com.blankj.utilcode.util.ConvertUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TopDataStoreInstrumentedTest {

    @Test
    fun topStoreIsIndependentAndResetAllRestoresMainDefault() = runBlocking {
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

            val defaults = DataStoreHolder.topGestureButtons.data.first()
            assertEquals(GestureButton.TopDefaults, defaults)
            assertEquals(1, defaults.size)
            val mainButton = defaults.single()
            assertTrue(mainButton.isDefault)
            assertEquals(Position.Top, mainButton.position)
            assertFalse(mainButton.enabled)
            assertEquals(0f, mainButton.start)
            assertEquals(1f, mainButton.end)
            assertFalse(mainButton.alignRegion)
            assertEquals(GestureActions(), mainButton.slideActions)
            assertEquals(GestureActions(), mainButton.longSlideActions)
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

    @Test
    fun gestureButtonWidthLimitRemainsSharedAcrossEdges() {
        assertEquals(
            ConvertUtils.dp2px(60f),
            GlobalSettings.MaxGestureButtonWidth
        )
    }
}
