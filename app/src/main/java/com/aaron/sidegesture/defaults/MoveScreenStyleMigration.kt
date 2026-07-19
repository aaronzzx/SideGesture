package com.aaron.sidegesture.defaults

import androidx.datastore.core.DataMigration
import com.aaron.sidegesture.entity.global.ActionSettings
import com.aaron.sidegesture.entity.global.forceCrosshairMoveScreenStyle

object MoveScreenStyleMigration : DataMigration<ActionSettings> {

    override suspend fun shouldMigrate(currentData: ActionSettings): Boolean {
        return currentData.moveScreen.style != ActionSettings.MoveScreen.Style.Crosshair
    }

    override suspend fun migrate(currentData: ActionSettings): ActionSettings {
        return currentData.forceCrosshairMoveScreenStyle()
    }

    override suspend fun cleanUp() = Unit
}
