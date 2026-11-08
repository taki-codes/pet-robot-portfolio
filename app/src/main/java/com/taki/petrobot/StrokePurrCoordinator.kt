package com.taki.petrobot

enum class StrokePurrResult { PLAYED, BUSY, UNAVAILABLE }

class StrokePurrCoordinator(
    private val playPurr: () -> Boolean,
    private val playbackGuard: LocalAudioPlaybackGuard,
) {
    private var blockingUntilNanos = Long.MIN_VALUE

    @Synchronized
    fun onStroke(timestampNanos: Long): StrokePurrResult {
        if (!playbackGuard.shouldProcessCapturedAudio(timestampNanos)) return StrokePurrResult.BUSY
        if (!playPurr()) return StrokePurrResult.UNAVAILABLE
        blockingUntilNanos = timestampNanos + STROKE_PURR_ECHO_GUARD_NANOS
        playbackGuard.suppressFor(timestampNanos, STROKE_PURR_ECHO_GUARD_NANOS)
        return StrokePurrResult.PLAYED
    }

    @Synchronized
    fun isBlockingOtherSounds(timestampNanos: Long): Boolean =
        timestampNanos < blockingUntilNanos
}

const val STROKE_PURR_ECHO_GUARD_NANOS = 1_700_000_000L
