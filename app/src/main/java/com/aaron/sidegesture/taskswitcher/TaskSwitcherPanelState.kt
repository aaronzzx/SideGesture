package com.aaron.sidegesture.taskswitcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.RecentTask

class TaskSwitcherPanelState {

    var visible: Boolean by mutableStateOf(false)
        private set

    var items: List<RecentTask> by mutableStateOf(emptyList())
        private set

    var fingerAnchor: Offset by mutableStateOf(Offset.Unspecified)
        private set

    var triggerEdge: Position by mutableStateOf(Position.Left)
        private set

    fun show(items: List<RecentTask>, anchor: Offset, edge: Position) {
        this.items = items
        this.fingerAnchor = anchor
        this.triggerEdge = edge
        visible = true
    }

    fun remove(task: RecentTask) {
        items = items.filterNot {
            it.taskId == task.taskId && it.packageName == task.packageName
        }
    }

    fun removePackages(packageNames: Set<String>) {
        items = items.filterNot { it.packageName in packageNames }
    }

    fun hide() {
        visible = false
    }
}

@Composable
fun rememberTaskSwitcherPanelState(): TaskSwitcherPanelState {
    return remember { TaskSwitcherPanelState() }
}
