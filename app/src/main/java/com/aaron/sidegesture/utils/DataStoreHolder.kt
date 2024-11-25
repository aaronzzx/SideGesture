package com.aaron.sidegesture.utils

import androidx.datastore.core.DataStore
import com.aaron.sidegesture.App
import com.aaron.sidegesture.constant.DataStoreFiles
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.entity.global.InitialSettings
import com.aaron.sidegesture.ktx.dataStore

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/24
 */
object DataStoreHolder {

    val initialSettings: DataStore<InitialSettings> = run {
        val fileName = DataStoreFiles.INITIAL_SETTINGS
        val defValue = InitialSettings()
        App.getContext().dataStore(fileName, defValue)
    }

    val advancedSettings: DataStore<AdvancedSettings> = run {
        val fileName = DataStoreFiles.ADVANCED_SETTINGS
        val defValue = AdvancedSettings()
        App.getContext().dataStore(fileName, defValue)
    }

    val gestureSettings: DataStore<GestureSettings> = run {
        val fileName = DataStoreFiles.GESTURE_SETTINGS
        val defValue = GestureSettings()
        App.getContext().dataStore(fileName, defValue)
    }

    val gestureButtons: DataStore<List<GestureButton>> = run {
        val fileName = DataStoreFiles.GESTURE_BUTTONS
        val defValue = GestureButton.Defaults
        App.getContext().dataStore(fileName, defValue)
    }
}