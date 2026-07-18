package com.aaron.sidegesture.action.handler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.action.ActionRequest
import com.aaron.sidegesture.action.OverlayActionHandler
import com.aaron.sidegesture.constant.GlobalActions
import com.aaron.sidegesture.entity.Position
import com.aaron.sidegesture.entity.RecentTask
import com.aaron.sidegesture.feature.taskswitcher.TaskSwitcherPanel
import com.aaron.sidegesture.feature.taskswitcher.TaskSwitcherPanelState
import com.aaron.sidegesture.platform.shell.ShellActionExecutor
import com.aaron.sidegesture.ui.theme.WallpaperAwareSideGestureTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskSwitcherActionHandler(
    private val service: SideGestureService,
    private val scope: CoroutineScope
) : OverlayActionHandler {

    private companion object {
        val RECENT_TASK_ID_REGEX = Regex("#(\\d+)")
        val RECENT_TASK_PACKAGE_REGEX = Regex("A=\\d+:(\\S+?)[\\s}]")
        val PACKAGE_NAME_REGEX = Regex("^[A-Za-z0-9_.]+$")
    }

    override val supportedActions = setOf(GlobalActions.TASK_SWITCHER)

    private val state = TaskSwitcherPanelState()
    private val lockedPackages = MutableStateFlow(emptySet<String>())
    override val touchEnabled: Flow<Boolean> = snapshotFlow { state.visible }
    private var requestVersion = 0L

    override suspend fun handle(request: ActionRequest) {
        val context = request.actionContext ?: return
        val anchor = context.anchor ?: return
        val edge = context.button?.position ?: Position.Left
        val version = ++requestVersion
        state.hide()
        val tasks = queryRecentTasks()
        if (version != requestVersion) return
        if (tasks.isEmpty()) return
        state.show(tasks, anchor, edge)
    }

    override fun onDismiss() {
        requestVersion++
        state.hide()
    }

    @Composable
    override fun Content() {
        val lockedPackageNames by lockedPackages.collectAsState()
        WallpaperAwareSideGestureTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                TaskSwitcherPanel(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    lockedPackageNames = lockedPackageNames,
                    onLaunch = { task ->
                        switchToRecentTask(task.packageName)
                        state.hide()
                    },
                    onClose = { task ->
                        scope.launch {
                            if (closeRecentTask(task.packageName)) state.remove(task)
                        }
                    },
                    onToggleLock = ::toggleLock,
                    onCloseAll = { tasks ->
                        scope.launch {
                            val closed = tasks.map { it.packageName }
                                .distinct()
                                .filter { closeRecentTask(it) }
                                .toSet()
                            state.removePackages(closed)
                        }
                    }
                )
            }
        }
    }

    private fun toggleLock(packageName: String): Boolean {
        val packages = lockedPackages.value
        val locked = packageName !in packages
        lockedPackages.value = if (locked) {
            packages + packageName
        } else {
            packages - packageName
        }
        return locked
    }

    private suspend fun queryRecentTasks(): List<RecentTask> = withContext(Dispatchers.IO) {
        val result = ShellActionExecutor.execute("dumpsys activity recents | grep 'Recent #'")
        if (!result.isSuccess) {
            service.showShellFailureToast(result)
            return@withContext emptyList()
        }
        parseRecentTasks(result.stdout)
    }

    private suspend fun closeRecentTask(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!PACKAGE_NAME_REGEX.matches(packageName)) {
            return@withContext false
        }
        val result = ShellActionExecutor.execute("am force-stop $packageName")
        if (!result.isSuccess) {
            service.showShellFailureToast(result)
        }
        result.isSuccess
    }

    private fun switchToRecentTask(packageName: String) {
        val intent = service.packageManager.getLaunchIntentForPackage(packageName) ?: return
        runCatching {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
        }
    }

    private fun parseRecentTasks(output: String): List<RecentTask> {
        return output.lineSequence()
            .filter { it.contains("Recent #") }
            .mapNotNull { line ->
                if (line.contains("type=home")) return@mapNotNull null
                val taskId = RECENT_TASK_ID_REGEX.findAll(line)
                    .lastOrNull()
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                    ?: return@mapNotNull null
                val packageName = RECENT_TASK_PACKAGE_REGEX.find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: return@mapNotNull null
                if (packageName == service.packageName ||
                    service.packageManager.getLaunchIntentForPackage(packageName) == null
                ) {
                    return@mapNotNull null
                }
                val label = runCatching {
                    val appInfo = service.packageManager.getApplicationInfo(packageName, 0)
                    service.packageManager.getApplicationLabel(appInfo).toString()
                }.getOrDefault(packageName)
                RecentTask(taskId, packageName, label)
            }
            .toList()
    }
}
