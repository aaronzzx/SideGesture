package com.aaron.sidegesture.feature.servicesettings

import com.aaron.sidegesture.entity.global.RestoreAck
import com.aaron.sidegesture.entity.global.RestoreCoordination
import com.aaron.sidegesture.entity.global.RestorePhase

object RestoreProtocol {

    const val NO_CONSUMER_SESSION = "no-consumer"

    fun nextGeneration(current: RestoreCoordination): Long {
        check(current.generation < Long.MAX_VALUE) { "Restore generation exhausted" }
        return current.generation + 1L
    }

    fun begin(
        current: RestoreCoordination,
        generation: Long,
        targetDigest: String
    ): RestoreCoordination {
        require(generation > current.generation) { "Restore generation must increase" }
        require(targetDigest.isNotBlank()) { "Restore target digest is empty" }
        return RestoreCoordination(
            generation = generation,
            phase = RestorePhase.BlockRequested,
            inProgress = true,
            targetDigest = targetDigest
        )
    }

    fun restart(
        current: RestoreCoordination,
        targetDigest: String = current.targetDigest
    ): RestoreCoordination {
        require(current.generation > 0L) { "Restore generation is missing" }
        require(targetDigest.isNotBlank()) { "Restore target digest is empty" }
        return current.copy(
            phase = RestorePhase.BlockRequested,
            inProgress = true,
            blockedGenerationAck = 0L,
            blockedServiceSession = "",
            noConsumerPath = false,
            commitGeneration = 0L,
            commitReadyAck = null,
            appliedAck = null,
            targetDigest = targetDigest,
            failureReason = null
        )
    }

    fun acknowledgeBlocked(
        current: RestoreCoordination,
        serviceSession: String
    ): RestoreCoordination {
        if (!current.inProgress || serviceSession.isBlank()) return current
        return current.copy(
            serviceSession = serviceSession,
            blockedGenerationAck = current.generation,
            blockedServiceSession = serviceSession
        )
    }

    fun markNoConsumerBlocked(current: RestoreCoordination): RestoreCoordination {
        require(current.inProgress && current.phase == RestorePhase.BlockRequested)
        return current.copy(
            phase = RestorePhase.Blocked,
            noConsumerPath = true
        )
    }

    fun markBlocked(
        current: RestoreCoordination,
        expectedServiceSession: String
    ): RestoreCoordination {
        require(current.inProgress && current.phase == RestorePhase.BlockRequested)
        require(current.blockedGenerationAck == current.generation)
        require(current.blockedServiceSession == expectedServiceSession)
        return current.copy(phase = RestorePhase.Blocked)
    }

    fun markWriting(current: RestoreCoordination): RestoreCoordination {
        require(current.inProgress && current.phase == RestorePhase.Blocked)
        return current.copy(phase = RestorePhase.Writing)
    }

    fun requestCommit(current: RestoreCoordination): RestoreCoordination {
        require(current.inProgress && current.phase == RestorePhase.Writing)
        return current.copy(
            phase = RestorePhase.CommitRequested,
            commitGeneration = current.generation,
            commitReadyAck = null,
            appliedAck = null
        )
    }

    fun acknowledgeCommitReady(
        current: RestoreCoordination,
        serviceSession: String,
        digest: String
    ): RestoreCoordination {
        if (!current.inProgress || current.phase != RestorePhase.CommitRequested) return current
        if (current.commitGeneration != current.generation || digest != current.targetDigest) return current
        return current.copy(
            serviceSession = serviceSession,
            commitReadyAck = RestoreAck(current.generation, serviceSession, digest)
        )
    }

    fun acknowledgeNoConsumerCommitReady(current: RestoreCoordination): RestoreCoordination {
        require(current.noConsumerPath)
        return acknowledgeCommitReady(
            current = current,
            serviceSession = NO_CONSUMER_SESSION,
            digest = current.targetDigest
        )
    }

    fun complete(
        current: RestoreCoordination,
        expectedServiceSession: String
    ): RestoreCoordination {
        require(current.inProgress && current.phase == RestorePhase.CommitRequested)
        val ack = requireNotNull(current.commitReadyAck)
        require(ack.generation == current.generation)
        require(ack.serviceSession == expectedServiceSession)
        require(ack.digest == current.targetDigest)
        return current.copy(
            phase = RestorePhase.Complete,
            inProgress = false,
            failureReason = null
        )
    }

    fun acknowledgeApplied(
        current: RestoreCoordination,
        serviceSession: String,
        digest: String
    ): RestoreCoordination {
        if (current.inProgress || current.phase != RestorePhase.Complete) return current
        if (current.generation <= 0L || digest != current.targetDigest) return current
        return current.copy(
            serviceSession = serviceSession,
            appliedAck = RestoreAck(current.generation, serviceSession, digest)
        )
    }

    fun fail(current: RestoreCoordination, reason: String): RestoreCoordination {
        return current.copy(failureReason = reason.take(512))
    }
}
