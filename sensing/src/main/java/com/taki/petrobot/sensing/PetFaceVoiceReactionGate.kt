package com.taki.petrobot.sensing

const val PET_FACE_VOICE_REACTION_COOLDOWN_NANOS = 1_500_000_000L

class PetFaceVoiceReactionGate(
    private val cooldownNanos: Long = PET_FACE_VOICE_REACTION_COOLDOWN_NANOS,
) {
    private var lastTriggeredAtNanos: Long? = null

    fun tryTrigger(timestampNanos: Long): Boolean {
        val previousTrigger = lastTriggeredAtNanos
        val cooldownElapsed = previousTrigger == null ||
            timestampNanos - previousTrigger >= cooldownNanos
        if (!cooldownElapsed) return false

        lastTriggeredAtNanos = timestampNanos
        return true
    }

    fun reset() {
        lastTriggeredAtNanos = null
    }
}
