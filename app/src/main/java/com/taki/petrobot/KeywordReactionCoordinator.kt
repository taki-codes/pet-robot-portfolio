package com.taki.petrobot

import com.taki.petrobot.sensing.PetFaceVoiceReactionGate

data class KeywordReactionResult(
    val visualTriggered: Boolean,
    val meowPlayed: Boolean,
)

class KeywordReactionCoordinator(
    private val gate: PetFaceVoiceReactionGate = PetFaceVoiceReactionGate(),
    private val playMeow: () -> Boolean,
    private val playbackGuard: LocalAudioPlaybackGuard = LocalAudioPlaybackGuard(),
    private val echoGuardNanos: Long = LOCAL_MEOW_ECHO_GUARD_NANOS,
) {
    fun onKeywordDetected(timestampNanos: Long): KeywordReactionResult {
        if (!gate.tryTrigger(timestampNanos)) {
            return KeywordReactionResult(
                visualTriggered = false,
                meowPlayed = false,
            )
        }

        val meowPlayed = playMeow()
        if (meowPlayed) {
            playbackGuard.suppressFor(
                playbackStartedAtNanos = timestampNanos,
                durationNanos = echoGuardNanos,
            )
        }

        return KeywordReactionResult(
            visualTriggered = true,
            meowPlayed = meowPlayed,
        )
    }

    fun shouldProcessCapturedAudio(timestampNanos: Long): Boolean =
        playbackGuard.shouldProcessCapturedAudio(timestampNanos)

    fun reset() {
        gate.reset()
    }
}

const val LOCAL_MEOW_ECHO_GUARD_NANOS = 900_000_000L
