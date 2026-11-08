package com.taki.petrobot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioPlaybackGuardTest {
    @Test
    fun suppressesCapturedAudioUntilGuardEnds() {
        val guard = LocalAudioPlaybackGuard()

        guard.suppressFor(
            playbackStartedAtNanos = 1_000L,
            durationNanos = 500L,
        )

        assertFalse(guard.shouldProcessCapturedAudio(1_000L))
        assertFalse(guard.shouldProcessCapturedAudio(1_499L))
        assertTrue(guard.shouldProcessCapturedAudio(1_500L))
    }

    @Test
    fun laterPlaybackExtendsExistingGuard() {
        val guard = LocalAudioPlaybackGuard()
        guard.suppressFor(1_000L, 500L)
        guard.suppressFor(1_200L, 700L)

        assertFalse(guard.shouldProcessCapturedAudio(1_899L))
        assertTrue(guard.shouldProcessCapturedAudio(1_900L))
    }

    @Test
    fun resetAllowsCapturedAudioImmediately() {
        val guard = LocalAudioPlaybackGuard()
        guard.suppressFor(1_000L, 500L)

        guard.reset()

        assertTrue(guard.shouldProcessCapturedAudio(1_001L))
    }
}
