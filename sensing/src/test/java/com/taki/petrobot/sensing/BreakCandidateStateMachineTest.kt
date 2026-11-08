package com.taki.petrobot.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakCandidateStateMachineTest {
    @Test
    fun confirmedPresenceWithoutPriorActivityStaysIdle() {
        val update = BreakCandidateStateMachine().update(
            observation(timestampNanos = 0L, presenceState = PresenceState.PRESENT),
        )

        assertEquals(BreakCandidateState.IDLE, update.state)
        assertFalse(update.activityDetected)
        assertFalse(update.becameCandidate)
    }

    @Test
    fun voiceActivityArmsMachine() {
        val update = BreakCandidateStateMachine().update(
            observation(
                timestampNanos = 0L,
                presenceState = PresenceState.PRESENT,
                voiceCandidate = true,
            ),
        )

        assertEquals(BreakCandidateState.ACTIVE, update.state)
        assertTrue(update.activityDetected)
    }

    @Test
    fun motionActivityArmsMachineAtThreshold() {
        val update = BreakCandidateStateMachine().update(
            observation(
                timestampNanos = 0L,
                presenceState = PresenceState.PRESENT,
                motionDifference = 0.02,
            ),
        )

        assertEquals(BreakCandidateState.ACTIVE, update.state)
        assertTrue(update.activityDetected)
    }

    @Test
    fun shortQuietPeriodDoesNotProduceCandidate() {
        val machine = BreakCandidateStateMachine()
        machine.update(
            observation(0L, PresenceState.PRESENT, voiceCandidate = true),
        )

        val update = machine.update(
            observation(1_499_999_999L, PresenceState.PRESENT),
        )

        assertEquals(BreakCandidateState.ACTIVE, update.state)
        assertFalse(update.becameCandidate)
    }

    @Test
    fun quietPeriodProducesOneCandidate() {
        val machine = BreakCandidateStateMachine()
        machine.update(
            observation(0L, PresenceState.PRESENT, voiceCandidate = true),
        )

        val candidate = machine.update(
            observation(1_500_000_000L, PresenceState.PRESENT),
        )
        val held = machine.update(
            observation(2_000_000_000L, PresenceState.PRESENT),
        )

        assertEquals(BreakCandidateState.CANDIDATE, candidate.state)
        assertTrue(candidate.becameCandidate)
        assertEquals(BreakCandidateState.CANDIDATE, held.state)
        assertFalse(held.becameCandidate)
    }

    @Test
    fun newActivityRearmsCandidate() {
        val machine = BreakCandidateStateMachine()
        machine.update(
            observation(0L, PresenceState.PRESENT, voiceCandidate = true),
        )
        machine.update(
            observation(1_500_000_000L, PresenceState.PRESENT),
        )

        val update = machine.update(
            observation(
                2_000_000_000L,
                PresenceState.PRESENT,
                motionDifference = 0.03,
            ),
        )

        assertEquals(BreakCandidateState.ACTIVE, update.state)
        assertTrue(update.stateChanged)
    }

    @Test
    fun lossOfConfirmedPresenceResetsMachine() {
        val machine = BreakCandidateStateMachine()
        machine.update(
            observation(0L, PresenceState.PRESENT, voiceCandidate = true),
        )

        val update = machine.update(
            observation(100_000_000L, PresenceState.POSSIBLE),
        )

        assertEquals(BreakCandidateState.IDLE, update.state)
        assertTrue(update.stateChanged)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfOrderObservationIsRejected() {
        val machine = BreakCandidateStateMachine()
        machine.update(observation(2L, PresenceState.PRESENT))
        machine.update(observation(1L, PresenceState.PRESENT))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMotionDifferenceIsRejected() {
        observation(
            timestampNanos = 0L,
            presenceState = PresenceState.PRESENT,
            motionDifference = -0.1,
        )
    }

    private fun observation(
        timestampNanos: Long,
        presenceState: PresenceState,
        motionDifference: Double? = null,
        voiceCandidate: Boolean = false,
    ) = BreakCandidateObservation(
        timestampNanos = timestampNanos,
        presenceState = presenceState,
        meanAbsoluteYDifference = motionDifference,
        voiceCandidate = voiceCandidate,
    )
}
