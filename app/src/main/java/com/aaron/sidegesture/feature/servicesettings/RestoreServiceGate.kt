package com.aaron.sidegesture.feature.servicesettings

import com.aaron.sidegesture.constant.Paths
import com.aaron.sidegesture.entity.global.RestoreCoordination
import com.aaron.sidegesture.entity.global.RestorePhase
import com.aaron.sidegesture.utils.DataStoreHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RestoreServiceGate(
    private val scope: CoroutineScope,
    private val settingsStore: ServiceSettingsStore,
    private val onBlocked: () -> Unit,
    private val onApply: (ServiceSettingsSnapshot) -> Unit
) {

    val serviceSession: String = UUID.randomUUID().toString()

    private var gateJob: Job? = null
    private var blockedGeneration = 0L
    private var appliedGeneration = Long.MIN_VALUE
    private var runtimeApplied = false

    fun start() {
        if (gateJob?.isActive == true) return
        gateJob = scope.launch(Dispatchers.Main.immediate) {
            DataStoreHolder.restoreCoordination.data.collectLatest(::handle)
        }
    }

    fun release() {
        gateJob?.cancel()
        gateJob = null
    }

    private suspend fun handle(coordination: RestoreCoordination) {
        if (coordination.blocksRuntime) {
            blockRuntime(coordination.generation)
            if (coordination.blockedGenerationAck != coordination.generation ||
                coordination.blockedServiceSession != serviceSession ||
                coordination.serviceSession != serviceSession
            ) {
                DataStoreHolder.restoreCoordination.updateData { current ->
                    if (current.generation != coordination.generation || !current.inProgress) {
                        current
                    } else {
                        RestoreProtocol.acknowledgeBlocked(current, serviceSession)
                    }
                }
                return
            }
            if (coordination.phase == RestorePhase.CommitRequested &&
                coordination.commitReadyAck?.let {
                    it.generation == coordination.generation &&
                        it.serviceSession == serviceSession &&
                        it.digest == coordination.targetDigest
                } != true
            ) {
                acknowledgeCommitReady(coordination)
            }
            return
        }

        if (coordination.serviceSession != serviceSession) {
            DataStoreHolder.restoreCoordination.updateData { current ->
                if (current.generation != coordination.generation || current.blocksRuntime) {
                    current
                } else {
                    current.copy(serviceSession = serviceSession)
                }
            }
            return
        }

        if (!runtimeApplied || appliedGeneration != coordination.generation) {
            val snapshot = settingsStore.awaitSnapshot()
            try {
                onApply(snapshot)
                runtimeApplied = true
                blockedGeneration = 0L
                appliedGeneration = coordination.generation
            } catch (error: Throwable) {
                blockRuntime(coordination.generation)
                recordFailure(coordination.generation, "apply:${error.message.orEmpty()}")
                return
            }
        }

        if (coordination.generation > 0L &&
            coordination.targetDigest.isNotBlank() &&
            coordination.appliedAck?.let {
                it.generation == coordination.generation &&
                    it.serviceSession == serviceSession &&
                    it.digest == coordination.targetDigest
            } != true
        ) {
            DataStoreHolder.restoreCoordination.updateData { current ->
                if (current.generation != coordination.generation || current.blocksRuntime) {
                    current
                } else {
                    RestoreProtocol.acknowledgeApplied(
                        current = current,
                        serviceSession = serviceSession,
                        digest = current.targetDigest
                    )
                }
            }
        }
    }

    private fun blockRuntime(generation: Long) {
        if (runtimeApplied || blockedGeneration != generation) {
            onBlocked()
        }
        runtimeApplied = false
        blockedGeneration = generation
    }

    private suspend fun acknowledgeCommitReady(coordination: RestoreCoordination) {
        val snapshot = settingsStore.awaitRawSnapshot()
        val digest = withContext(Dispatchers.IO) {
            RestoreDigest.digest(
                payload = RestoreDigest.fromSnapshot(snapshot),
                imageDirectory = File(Paths.Image)
            )
        }
        if (digest != coordination.targetDigest) {
            recordFailure(
                coordination.generation,
                "service-digest:${digest.take(16)}"
            )
            return
        }
        DataStoreHolder.restoreCoordination.updateData { current ->
            if (current.generation != coordination.generation ||
                current.phase != RestorePhase.CommitRequested ||
                current.blockedServiceSession != serviceSession
            ) {
                current
            } else {
                RestoreProtocol.acknowledgeCommitReady(
                    current = current,
                    serviceSession = serviceSession,
                    digest = digest
                )
            }
        }
    }

    private suspend fun recordFailure(generation: Long, reason: String) {
        DataStoreHolder.restoreCoordination.updateData { current ->
            if (current.generation != generation) current else RestoreProtocol.fail(current, reason)
        }
    }
}
