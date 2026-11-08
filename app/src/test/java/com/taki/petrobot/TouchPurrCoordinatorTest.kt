package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchPurrCoordinatorTest {
    @Test
    fun allSixSpacedTapsUseNormalVoiceIncludingThirdAndSixth() {
        var count = 0
        val guard = LocalAudioPlaybackGuard()
        val c = TouchPurrCoordinator({ count++; true }, playbackGuard = guard)
        for (i in 0..5) {
            val at = i * TOUCH_PURR_COOLDOWN_NANOS
            assertEquals(TouchPurrResult.PLAYED, c.onTouch(at))
            assertEquals(TouchPurrResult.COOLDOWN, c.onTouch(at + 1))
            assertFalse(guard.shouldProcessCapturedAudio(at + TOUCH_PURR_ECHO_GUARD_NANOS - 1))
            assertTrue(guard.shouldProcessCapturedAudio(at + TOUCH_PURR_ECHO_GUARD_NANOS))
        }
        assertEquals(6, count)
    }

    @Test
    fun firstTouchPlaysPurrAndGuardsCapturedAudio() {
        var playCount = 0
        val guard = LocalAudioPlaybackGuard()
        val coordinator = TouchPurrCoordinator(
            playPurr = {
                playCount += 1
                true
            },
            playbackGuard = guard,
        )
        val touchedAt = 1_000_000_000L

        val result = coordinator.onTouch(touchedAt)

        assertEquals(TouchPurrResult.PLAYED, result)
        assertEquals(1, playCount)
        assertFalse(guard.shouldProcessCapturedAudio(touchedAt))
        assertTrue(
            guard.shouldProcessCapturedAudio(
                touchedAt + TOUCH_PURR_ECHO_GUARD_NANOS,
            ),
        )
    }

    @Test
    fun rapidTouchesDoNotReplayOrExtendPurr() {
        var playCount = 0
        val guard = LocalAudioPlaybackGuard()
        val coordinator = TouchPurrCoordinator(
            playPurr = {
                playCount += 1
                true
            },
            playbackGuard = guard,
        )
        val firstTouchAt = 1_000_000_000L
        coordinator.onTouch(firstTouchAt)

        val repeated = coordinator.onTouch(
            firstTouchAt + TOUCH_PURR_COOLDOWN_NANOS - 1L,
        )

        assertEquals(TouchPurrResult.COOLDOWN, repeated)
        assertEquals(1, playCount)
        assertTrue(
            guard.shouldProcessCapturedAudio(
                firstTouchAt + TOUCH_PURR_ECHO_GUARD_NANOS,
            ),
        )
    }

    @Test
    fun touchAtCooldownBoundaryCanReplay() {
        var playCount = 0
        val coordinator = TouchPurrCoordinator(
            playPurr = {
                playCount += 1
                true
            },
            playbackGuard = LocalAudioPlaybackGuard(),
        )
        val firstTouchAt = 1_000_000_000L
        coordinator.onTouch(firstTouchAt)

        val result = coordinator.onTouch(
            firstTouchAt + TOUCH_PURR_COOLDOWN_NANOS,
        )

        assertEquals(TouchPurrResult.PLAYED, result)
        assertEquals(2, playCount)
    }

    @Test
    fun unavailablePurrDoesNotStartCooldownOrAudioGuard() {
        var available = false
        val guard = LocalAudioPlaybackGuard()
        val coordinator = TouchPurrCoordinator(
            playPurr = { available },
            playbackGuard = guard,
        )
        val touchedAt = 1_000_000_000L

        assertEquals(TouchPurrResult.UNAVAILABLE, coordinator.onTouch(touchedAt))
        assertTrue(guard.shouldProcessCapturedAudio(touchedAt))

        available = true
        assertEquals(TouchPurrResult.PLAYED, coordinator.onTouch(touchedAt + 1L))
    }

    @Test
    fun resetAllowsImmediateReplay() {
        var playCount = 0
        val coordinator = TouchPurrCoordinator(
            playPurr = {
                playCount += 1
                true
            },
            playbackGuard = LocalAudioPlaybackGuard(),
        )
        val touchedAt = 1_000_000_000L
        coordinator.onTouch(touchedAt)

        coordinator.reset()
        val result = coordinator.onTouch(touchedAt + 1L)

        assertEquals(TouchPurrResult.PLAYED, result)
        assertEquals(2, playCount)
    }
}
