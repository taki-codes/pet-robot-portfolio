package com.taki.petrobot

enum class ArrivalMeowResult {
    NONE,
    SHORT_REST,
    PLAYED,
    UNAVAILABLE,
    BUSY,
}

/** Main-thread coordinator. All time values use elapsed realtime nanoseconds. */
class ArrivalMeowCoordinator(
    private val playMeow: () -> Boolean,
    private val playbackGuard: LocalAudioPlaybackGuard,
) {
    private var restingSinceNanos: Long? = null

    fun onMood(
        mood: PetFaceMood,
        observationTimestampNanos: Long,
        playbackTimestampNanos: Long = observationTimestampNanos,
    ): ArrivalMeowResult {
        if (mood == PetFaceMood.RESTING) {
            if (restingSinceNanos == null) restingSinceNanos = observationTimestampNanos
            return ArrivalMeowResult.NONE
        }

        val restingSince = restingSinceNanos ?: return ArrivalMeowResult.NONE
        // Consume this return even when playback is unavailable or another sound is active.
        restingSinceNanos = null
        if (observationTimestampNanos - restingSince < ARRIVAL_MIN_REST_NANOS) {
            return ArrivalMeowResult.SHORT_REST
        }
        if (!playbackGuard.shouldProcessCapturedAudio(playbackTimestampNanos)) {
            return ArrivalMeowResult.BUSY
        }
        if (!playMeow()) return ArrivalMeowResult.UNAVAILABLE

        playbackGuard.suppressFor(playbackTimestampNanos, LOCAL_MEOW_ECHO_GUARD_NANOS)
        return ArrivalMeowResult.PLAYED
    }

    /** Reset only arrival timing; a sound may still be playing during onStop. */
    fun reset() {
        restingSinceNanos = null
    }
}

const val ARRIVAL_MIN_REST_NANOS = 180_000_000_000L
