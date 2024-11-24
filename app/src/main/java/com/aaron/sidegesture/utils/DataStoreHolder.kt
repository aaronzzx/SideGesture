package com.aaron.sidegesture.utils

import androidx.datastore.core.DataStore
import com.aaron.sidegesture.App
import com.aaron.sidegesture.constant.DataStoreFiles
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.ktx.dataStore

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/24
 */
object DataStoreHolder {

    val gestureButtons: DataStore<List<GestureButton>> = run {
        val fileName = DataStoreFiles.GESTURE_BUTTONS
        val defValue = GestureButton.Defaults
        App.getContext().dataStore(fileName, defValue)
    }
}