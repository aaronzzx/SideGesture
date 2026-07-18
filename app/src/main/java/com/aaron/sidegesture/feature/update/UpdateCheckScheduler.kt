package com.aaron.sidegesture.feature.update

import com.aaron.sidegesture.SideGestureService
import com.aaron.sidegesture.feature.servicesettings.ServiceSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UpdateCheckScheduler(
    private val service: SideGestureService,
    private val scope: CoroutineScope,
    private val settingsStore: ServiceSettingsStore
) {

    private companion object {
        const val UPDATE_CHECK_TICK_INTERVAL_MS = 30 * 60 * 1000L
    }

    private var tickerJob: Job? = null

    fun start() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                try {
                    val settings = settingsStore.awaitSnapshot()
                    if (settings.advancedSettings.autoCheckUpdate &&
                        UpdateRepository.shouldCheck()
                    ) {
                        val result = UpdateRepository.checkAndCache(force = false)
                        if (result is UpdateRepository.CheckResult.NewVersion) {
                            val version = result.state.latestVersion
                            val ignoredVersion = settingsStore.awaitSnapshot()
                                .initialSettings
                                .ignoredUpdateVersion
                            if (version.isNotBlank() && version != ignoredVersion) {
                                UpdateNotifications.showNewVersion(service, version)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // 检查异常完全隔离，不影响无障碍手势主流程。
                }
                delay(UPDATE_CHECK_TICK_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
