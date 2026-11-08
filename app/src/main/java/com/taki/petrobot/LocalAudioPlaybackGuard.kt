package com.taki.petrobot

import java.util.concurrent.atomic.AtomicLong

class LocalAudioPlaybackGuard {
    private val suppressCapturedAudioUntilNanos = AtomicLong(Long.MIN_VALUE)

    fun suppressFor(
        playbackStartedAtNanos: Long,
        durationNanos: Long,
    ) {
        require(durationNanos >= 0L)
        val suppressUntilNanos = playbackStartedAtNanos + durationNanos
        suppressCapturedAudioUntilNanos.updateAndGet { current ->
            maxOf(current, suppressUntilNanos)
        }
    }

    fun shouldProcessCapturedAudio(timestampNanos: Long): Boolean =
        timestampNanos >= suppressCapturedAudioUntilNanos.get()

    fun reset() {
        suppressCapturedAudioUntilNanos.set(Long.MIN_VALUE)
    }
}
