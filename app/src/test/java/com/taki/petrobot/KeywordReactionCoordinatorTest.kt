package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordReactionCoordinatorTest {
    @Test
    fun acceptedKeywordTriggersVisualAndOneMeow() {
        var playCount = 0
        val coordinator = KeywordReactionCoordinator(
            playMeow = {
                playCount += 1
                true
            },
        )

        val result = coordinator.onKeywordDetected(timestampNanos = 1_000_000_000L)

        assertTrue(result.visualTriggered)
        assertTrue(result.meowPlayed)
        assertEquals(1, playCount)
    }

    @Test
    fun repeatedCallbackInsideCooldownDoesNotReplayMeow() {
        var playCount = 0
        val coordinator = KeywordReactionCoordinator(
            playMeow = {
                playCount += 1
                true
            },
        )

        val first = coordinator.onKeywordDetected(timestampNanos = 1_000_000_000L)
        val repeated = coordinator.onKeywordDetected(timestampNanos = 2_499_999_999L)

        assertTrue(first.visualTriggered)
        assertFalse(repeated.visualTriggered)
        assertFalse(repeated.meowPlayed)
        assertEquals(1, playCount)
    }

    @Test
    fun unavailableMeowDoesNotBlockVisualReaction() {
        val coordinator = KeywordReactionCoordinator(playMeow = { false })

        val result = coordinator.onKeywordDetected(timestampNanos = 1_000_000_000L)

        assertTrue(result.visualTriggered)
        assertFalse(result.meowPlayed)
        assertTrue(coordinator.shouldProcessCapturedAudio(timestampNanos = 1_000_000_001L))
    }

    @Test
    fun playedMeowTemporarilyExcludesCapturedAudioFromRecognitionAndSensing() {
        val coordinator = KeywordReactionCoordinator(playMeow = { true })
        val playedAt = 1_000_000_000L

        coordinator.onKeywordDetected(timestampNanos = playedAt)

        assertFalse(coordinator.shouldProcessCapturedAudio(timestampNanos = playedAt))
        assertFalse(
            coordinator.shouldProcessCapturedAudio(
                timestampNanos = playedAt + LOCAL_MEOW_ECHO_GUARD_NANOS - 1L,
            ),
        )
        assertTrue(
            coordinator.shouldProcessCapturedAudio(
                timestampNanos = playedAt + LOCAL_MEOW_ECHO_GUARD_NANOS,
            ),
        )
    }

    @Test
    fun sharedPlaybackGuardAlsoExcludesOtherLocalSounds() {
        val guard = LocalAudioPlaybackGuard()
        val coordinator = KeywordReactionCoordinator(
            playMeow = { true },
            playbackGuard = guard,
        )
        guard.suppressFor(
            playbackStartedAtNanos = 1_000_000_000L,
            durationNanos = TOUCH_PURR_ECHO_GUARD_NANOS,
        )

        assertFalse(
            coordinator.shouldProcessCapturedAudio(
                timestampNanos = 1_000_000_000L +
                    TOUCH_PURR_ECHO_GUARD_NANOS - 1L,
            ),
        )
    }

    @Test
    fun resetPreservesActiveSharedPlaybackGuard() {
        val guard = LocalAudioPlaybackGuard()
        val coordinator = KeywordReactionCoordinator(
            playMeow = { true },
            playbackGuard = guard,
        )
        val playedAt = 1_000_000_000L
        coordinator.onKeywordDetected(playedAt)

        coordinator.reset()

        assertFalse(coordinator.shouldProcessCapturedAudio(playedAt + 1L))
        assertTrue(
            coordinator.shouldProcessCapturedAudio(
                playedAt + LOCAL_MEOW_ECHO_GUARD_NANOS,
            ),
        )
    }

    @Test
    fun resetAllowsNextKeywordToPlayImmediately() {
        var playCount = 0
        val coordinator = KeywordReactionCoordinator(
            playMeow = {
                playCount += 1
                true
            },
        )
        coordinator.onKeywordDetected(timestampNanos = 1_000_000_000L)

        coordinator.reset()
        assertFalse(coordinator.shouldProcessCapturedAudio(timestampNanos = 1_000_000_001L))
        val result = coordinator.onKeywordDetected(timestampNanos = 1_000_000_001L)

        assertTrue(result.visualTriggered)
        assertTrue(result.meowPlayed)
        assertFalse(coordinator.shouldProcessCapturedAudio(timestampNanos = 1_000_000_001L))
        assertEquals(2, playCount)
    }
}
