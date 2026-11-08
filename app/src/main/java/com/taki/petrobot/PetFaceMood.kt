package com.taki.petrobot

import com.taki.petrobot.sensing.BreakCandidateState
import com.taki.petrobot.sensing.PresenceState

enum class PetFaceMood {
    RESTING,
    CURIOUS,
    ATTENTIVE,
    ENGAGED,
    BREAK_READY,
}

fun resolvePetFaceMood(
    presenceState: PresenceState,
    breakState: BreakCandidateState,
): PetFaceMood = when {
    presenceState == PresenceState.ABSENT -> PetFaceMood.RESTING
    presenceState == PresenceState.POSSIBLE -> PetFaceMood.CURIOUS
    breakState == BreakCandidateState.CANDIDATE -> PetFaceMood.BREAK_READY
    breakState == BreakCandidateState.ACTIVE -> PetFaceMood.ENGAGED
    else -> PetFaceMood.ATTENTIVE
}
