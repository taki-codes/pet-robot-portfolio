package com.taki.petrobot

enum class TouchPurrResult {
    PLAYED,
    COOLDOWN,
    UNAVAILABLE,
}

class TouchPurrCoordinator(
    private val playPurr: () -> Boolean,
    private val playbackGuard: LocalAudioPlaybackGuard,
    private val cooldownNanos: Long = TOUCH_PURR_COOLDOWN_NANOS,
    private val echoGuardNanos: Long = TOUCH_PURR_ECHO_GUARD_NANOS,
) {
    private var lastPlayedAtNanos = Long.MIN_VALUE

    @Synchronized
    fun onTouch(timestampNanos: Long): TouchPurrResult {
        if (
            lastPlayedAtNanos != Long.MIN_VALUE &&
            timestampNanos < lastPlayedAtNanos + cooldownNanos
        ) {
            return TouchPurrResult.COOLDOWN
        }

        if (!playPurr()) {
            return TouchPurrResult.UNAVAILABLE
        }

        lastPlayedAtNanos = timestampNanos
        playbackGuard.suppressFor(
            playbackStartedAtNanos = timestampNanos,
            durationNanos = echoGuardNanos,
        )
        return TouchPurrResult.PLAYED
    }

    @Synchronized
    fun reset() {
        lastPlayedAtNanos = Long.MIN_VALUE
    }
}

const val TOUCH_PURR_COOLDOWN_NANOS = 2_000_000_000L
const val TOUCH_PURR_ECHO_GUARD_NANOS = 1_200_000_000L
