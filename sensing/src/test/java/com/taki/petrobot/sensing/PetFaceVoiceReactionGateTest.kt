package com.taki.petrobot.sensing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFaceVoiceReactionGateTest {
    @Test
    fun firstKeywordMatch_triggersReaction() {
        val gate = PetFaceVoiceReactionGate()
        assertTrue(gate.tryTrigger(timestampNanos = 1_000L))
    }

    @Test
    fun cooldownSuppressesRepeatedRecognizerCallbacks() {
        val gate = PetFaceVoiceReactionGate()
        assertTrue(gate.tryTrigger(timestampNanos = 0L))
        assertFalse(gate.tryTrigger(PET_FACE_VOICE_REACTION_COOLDOWN_NANOS - 1L))
        assertTrue(gate.tryTrigger(PET_FACE_VOICE_REACTION_COOLDOWN_NANOS))
    }

    @Test
    fun resetAllowsNewSessionToReactImmediately() {
        val gate = PetFaceVoiceReactionGate()
        assertTrue(gate.tryTrigger(timestampNanos = 10_000L))
        gate.reset()
        assertTrue(gate.tryTrigger(timestampNanos = 20_000L))
    }
}
