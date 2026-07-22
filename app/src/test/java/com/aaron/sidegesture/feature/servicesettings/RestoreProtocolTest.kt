package com.aaron.sidegesture.feature.servicesettings

import com.aaron.sidegesture.entity.global.RestoreCoordination
import com.aaron.sidegesture.entity.global.RestorePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreProtocolTest {

    @Test
    fun runningServiceCannotApplyBeforeUniqueCompleteCommitPoint() {
        val generation = 1L
        val digest = "target-digest"
        val session = "service-a"
        var state = RestoreProtocol.begin(RestoreCoordination(), generation, digest)

        assertTrue(state.blocksRuntime)
        assertEquals(RestorePhase.BlockRequested, state.phase)

        state = RestoreProtocol.acknowledgeBlocked(state, session)
        state = RestoreProtocol.markBlocked(state, session)
        state = RestoreProtocol.markWriting(state)
        state = RestoreProtocol.requestCommit(state)
        state = RestoreProtocol.acknowledgeCommitReady(state, session, digest)

        assertTrue(state.blocksRuntime)
        assertEquals(RestorePhase.CommitRequested, state.phase)
        assertNull(state.appliedAck)

        state = RestoreProtocol.complete(state, session)

        assertFalse(state.blocksRuntime)
        assertEquals(RestorePhase.Complete, state.phase)

        state = RestoreProtocol.acknowledgeApplied(state, session, digest)
        assertEquals(session, state.appliedAck?.serviceSession)
    }

    @Test
    fun staleServiceSessionCannotCompleteNewHandshake() {
        val digest = "target-digest"
        var state = RestoreProtocol.begin(RestoreCoordination(), 1L, digest)
        state = RestoreProtocol.acknowledgeBlocked(state, "old-session")
        state = RestoreProtocol.acknowledgeBlocked(state, "new-session")

        val staleResult = runCatching {
            RestoreProtocol.markBlocked(state, "old-session")
        }

        assertTrue(staleResult.isFailure)
        state = RestoreProtocol.markBlocked(state, "new-session")
        state = RestoreProtocol.markWriting(state)
        state = RestoreProtocol.requestCommit(state)

        val ignored = RestoreProtocol.acknowledgeCommitReady(
            current = state,
            serviceSession = "old-session",
            digest = "wrong-digest"
        )
        assertNull(ignored.commitReadyAck)
    }

    @Test
    fun noConsumerPathStillRequiresVerifiedCommitReadyBeforeComplete() {
        val digest = "target-digest"
        var state = RestoreProtocol.begin(RestoreCoordination(), 1L, digest)
        state = RestoreProtocol.markNoConsumerBlocked(state)
        state = RestoreProtocol.markWriting(state)
        state = RestoreProtocol.requestCommit(state)

        val prematureComplete = runCatching {
            RestoreProtocol.complete(state, RestoreProtocol.NO_CONSUMER_SESSION)
        }
        assertTrue(prematureComplete.isFailure)

        state = RestoreProtocol.acknowledgeNoConsumerCommitReady(state)
        state = RestoreProtocol.complete(state, RestoreProtocol.NO_CONSUMER_SESSION)
        assertFalse(state.blocksRuntime)
    }

    @Test
    fun recoveryRestartKeepsGenerationAndClearsAllAcks() {
        val digest = "target-digest"
        var state = RestoreProtocol.begin(RestoreCoordination(), 1L, digest)
        state = RestoreProtocol.acknowledgeBlocked(state, "service-a")
        state = state.copy(failureReason = "crash")

        val restarted = RestoreProtocol.restart(state, "rollback-digest")

        assertEquals(1L, restarted.generation)
        assertEquals(RestorePhase.BlockRequested, restarted.phase)
        assertEquals("rollback-digest", restarted.targetDigest)
        assertEquals(0L, restarted.blockedGenerationAck)
        assertTrue(restarted.blockedServiceSession.isEmpty())
        assertNull(restarted.commitReadyAck)
        assertNull(restarted.appliedAck)
        assertNull(restarted.failureReason)
    }
}
