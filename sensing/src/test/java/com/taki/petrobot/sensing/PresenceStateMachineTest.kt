package com.taki.petrobot.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceStateMachineTest {
    @Test
    fun noEvidenceStartsAbsent() {
        val update = PresenceStateMachine().update(observation(timestampNanos = 0L))

        assertEquals(PresenceState.ABSENT, update.state)
        assertEquals(PresenceEvidence.NONE, update.evidence)
        assertFalse(update.stateChanged)
    }

    @Test
    fun visibleFaceConfirmsPresenceImmediately() {
        val update = PresenceStateMachine().update(
            observation(timestampNanos = 10L, visibleFaceCount = 1),
        )

        assertEquals(PresenceState.PRESENT, update.state)
        assertEquals(PresenceEvidence.FACE, update.evidence)
        assertTrue(update.stateChanged)
    }

    @Test
    fun sustainedActivityConfirmsPresenceAfterDelay() {
        val machine = PresenceStateMachine()

        val first = machine.update(
            observation(timestampNanos = 0L, voiceCandidate = true),
        )
        val middle = machine.update(
            observation(timestampNanos = 400_000_000L, motionDifference = 0.03),
        )
        val confirmed = machine.update(
            observation(timestampNanos = 750_000_000L, voiceCandidate = true),
        )

        assertEquals(PresenceState.POSSIBLE, first.state)
        assertEquals(PresenceState.POSSIBLE, middle.state)
        assertEquals(PresenceState.PRESENT, confirmed.state)
    }

    @Test
    fun isolatedActivityCandidateExpires() {
        val machine = PresenceStateMachine()
        machine.update(observation(timestampNanos = 0L, motionDifference = 0.03))

        val update = machine.update(observation(timestampNanos = 500_000_000L))

        assertEquals(PresenceState.ABSENT, update.state)
        assertTrue(update.stateChanged)
    }

    @Test
    fun presentStateSurvivesShortGapThenBecomesAbsent() {
        val machine = PresenceStateMachine()
        machine.update(observation(timestampNanos = 0L, visibleFaceCount = 1))

        val held = machine.update(observation(timestampNanos = 2_999_999_999L))
        val absent = machine.update(observation(timestampNanos = 3_000_000_000L))

        assertEquals(PresenceState.PRESENT, held.state)
        assertEquals(PresenceState.ABSENT, absent.state)
        assertTrue(absent.stateChanged)
    }

    @Test
    fun belowThresholdMotionIsNotEvidence() {
        val update = PresenceStateMachine().update(
            observation(timestampNanos = 0L, motionDifference = 0.0199),
        )

        assertEquals(PresenceEvidence.NONE, update.evidence)
        assertEquals(PresenceState.ABSENT, update.state)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfOrderObservationIsRejected() {
        val machine = PresenceStateMachine()
        machine.update(observation(timestampNanos = 2L))
        machine.update(observation(timestampNanos = 1L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMotionDifferenceIsRejected() {
        observation(timestampNanos = 0L, motionDifference = 1.1)
    }

    private fun observation(
        timestampNanos: Long,
        visibleFaceCount: Int = 0,
        motionDifference: Double? = null,
        voiceCandidate: Boolean = false,
    ) = PresenceObservation(
        timestampNanos = timestampNanos,
        visibleFaceCount = visibleFaceCount,
        meanAbsoluteYDifference = motionDifference,
        voiceCandidate = voiceCandidate,
    )
}
