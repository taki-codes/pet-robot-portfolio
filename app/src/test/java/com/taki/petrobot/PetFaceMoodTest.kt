package com.taki.petrobot

import com.taki.petrobot.sensing.BreakCandidateState
import com.taki.petrobot.sensing.PresenceState
import org.junit.Assert.assertEquals
import org.junit.Test

class PetFaceMoodTest {
    @Test
    fun absentAlwaysRests() {
        BreakCandidateState.entries.forEach { breakState ->
            assertEquals(
                PetFaceMood.RESTING,
                resolvePetFaceMood(PresenceState.ABSENT, breakState),
            )
        }
    }

    @Test
    fun possiblePresenceIsCurious() {
        assertEquals(
            PetFaceMood.CURIOUS,
            resolvePetFaceMood(PresenceState.POSSIBLE, BreakCandidateState.IDLE),
        )
    }

    @Test
    fun presentIdleIsAttentive() {
        assertEquals(
            PetFaceMood.ATTENTIVE,
            resolvePetFaceMood(PresenceState.PRESENT, BreakCandidateState.IDLE),
        )
    }

    @Test
    fun presentActivityIsEngaged() {
        assertEquals(
            PetFaceMood.ENGAGED,
            resolvePetFaceMood(PresenceState.PRESENT, BreakCandidateState.ACTIVE),
        )
    }

    @Test
    fun presentCandidateIsBreakReady() {
        assertEquals(
            PetFaceMood.BREAK_READY,
            resolvePetFaceMood(PresenceState.PRESENT, BreakCandidateState.CANDIDATE),
        )
    }
}
