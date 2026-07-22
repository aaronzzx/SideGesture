package com.aaron.sidegesture.utils

import androidx.datastore.core.DataStore
import com.aaron.sidegesture.App
import com.aaron.sidegesture.constant.DataStoreFiles
import com.aaron.sidegesture.defaults.MoveScreenStyleMigration
import com.aaron.sidegesture.entity.GestureButton
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.AdvancedSettings
import com.aaron.sidegesture.entity.global.GestureSettings
import com.aaron.sidegesture.entity.global.InitialSettings
import com.aaron.sidegesture.entity.global.RestoreCoordination
import com.aaron.sidegesture.entity.global.RestoreJournal
import com.aaron.sidegesture.entity.global.RestorePhase
import com.aaron.sidegesture.entity.global.UpdateState
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

    val actionSettings: DataStore<ActionSettings> = run {
        val fileName = DataStoreFiles.ACTION_SETTINGS
        val defValue = ActionSettings()
        App.getContext().dataStore(
            fileName = fileName,
            defValue = defValue,
            migrations = listOf(MoveScreenStyleMigration)
        )
    }

    val bottomGestureButtons: DataStore<List<GestureButton>> = run {
        val fileName = DataStoreFiles.BOTTOM_GESTURE_BUTTONS
        val defValue = GestureButton.BottomDefaults
        App.getContext().dataStore(fileName, defValue)
    }

    val sideGestureButtons: DataStore<List<GestureButton>> = run {
        val fileName = DataStoreFiles.SIDE_GESTURE_BUTTONS
        val defValue = GestureButton.SideDefaults
        App.getContext().dataStore(fileName, defValue)
    }

    val topGestureButtons: DataStore<List<GestureButton>> = run {
        val fileName = DataStoreFiles.TOP_GESTURE_BUTTONS
        val defValue = GestureButton.TopDefaults
        App.getContext().dataStore(fileName, defValue)
    }

    val updateState: DataStore<UpdateState> = run {
        val fileName = DataStoreFiles.UPDATE_STATE
        val defValue = UpdateState()
        App.getContext().dataStore(fileName, defValue)
    }

    val restoreCoordination: DataStore<RestoreCoordination> = run {
        val fileName = DataStoreFiles.RESTORE_COORDINATION
        val defValue = RestoreCoordination()
        val corruptionValue = RestoreCoordination(
            phase = RestorePhase.BlockRequested,
            inProgress = true,
            failureReason = "Restore coordination is corrupt"
        )
        App.getContext().dataStore(fileName, defValue, corruptionValue)
    }

    val restoreJournal: DataStore<RestoreJournal> = run {
        val fileName = DataStoreFiles.RESTORE_JOURNAL
        val defValue = RestoreJournal()
        App.getContext().dataStore(fileName, defValue)
    }

    suspend fun resetAll() {
        initialSettings.updateData { InitialSettings() }
        advancedSettings.updateData { AdvancedSettings() }
        gestureSettings.updateData { GestureSettings() }
        actionSettings.updateData { ActionSettings() }
        sideGestureButtons.updateData { GestureButton.SideDefaults }
        bottomGestureButtons.updateData { GestureButton.BottomDefaults }
        topGestureButtons.updateData { GestureButton.TopDefaults }
    }
}
