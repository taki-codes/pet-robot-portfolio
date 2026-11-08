package com.taki.petrobot

import java.io.File
import javax.sound.sampled.AudioSystem
import kotlin.math.ceil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokePurrCoordinatorTest {
    @Test
    fun playbackGuardsAudioAndBlocksOtherSoundsUntilBoundary() {
        var count = 0
        val guard = LocalAudioPlaybackGuard()
        val coordinator = StrokePurrCoordinator({ count++; true }, guard)
        assertEquals(StrokePurrResult.PLAYED, coordinator.onStroke(0L))
        assertFalse(guard.shouldProcessCapturedAudio(STROKE_PURR_ECHO_GUARD_NANOS - 1L))
        assertTrue(coordinator.isBlockingOtherSounds(STROKE_PURR_ECHO_GUARD_NANOS - 1L))
        assertEquals(StrokePurrResult.BUSY, coordinator.onStroke(1L))
        assertEquals(1, count)
        assertFalse(coordinator.isBlockingOtherSounds(STROKE_PURR_ECHO_GUARD_NANOS))
        assertTrue(guard.shouldProcessCapturedAudio(STROKE_PURR_ECHO_GUARD_NANOS))
        assertEquals(StrokePurrResult.PLAYED, coordinator.onStroke(STROKE_PURR_ECHO_GUARD_NANOS))
    }

    @Test
    fun anotherSoundOrUnavailableResourceCannotStartPurrOrExtendGuard() {
        val guard = LocalAudioPlaybackGuard()
        var attempts = 0
        val coordinator = StrokePurrCoordinator({ attempts++; false }, guard)
        guard.suppressFor(0L, 100L)
        assertEquals(StrokePurrResult.BUSY, coordinator.onStroke(99L))
        assertEquals(0, attempts)
        assertEquals(StrokePurrResult.UNAVAILABLE, coordinator.onStroke(100L))
        assertEquals(1, attempts)
        assertTrue(guard.shouldProcessCapturedAudio(100L))
        assertFalse(coordinator.isBlockingOtherSounds(100L))
    }

    @Test
    fun strokeAndBlockedTapPreserveNormalTapPlayback() {
        val guard = LocalAudioPlaybackGuard()
        val stroke = StrokePurrCoordinator({ true }, guard)
        var now = 0L
        val sounds = mutableListOf<String>()
        val tap = TouchPurrCoordinator(
            playPurr = {
                if (stroke.isBlockingOtherSounds(now)) false else { sounds.add("meow"); true }
            },
            playbackGuard = guard,
        )
        assertEquals(TouchPurrResult.PLAYED, tap.onTouch(now))
        now = 2_000_000_000L
        assertEquals(StrokePurrResult.PLAYED, stroke.onStroke(now))
        assertEquals(TouchPurrResult.UNAVAILABLE, tap.onTouch(now))
        now = 4_000_000_000L
        assertEquals(TouchPurrResult.PLAYED, tap.onTouch(now))
        now = 6_000_000_000L
        assertEquals(TouchPurrResult.PLAYED, tap.onTouch(now))
        assertEquals(listOf("meow", "meow", "meow"), sounds)
    }

    @Test
    fun bundledRecordingFitsGuardAtSlowestRateWithTail() {
        AudioSystem.getAudioInputStream(File("src/main/res/raw/miina_affection.wav")).use { audio ->
            assertEquals(16, audio.format.sampleSizeInBits)
            assertEquals(2, audio.format.channels)
            assertEquals(44_100f, audio.format.frameRate, 0f)
            assertTrue(audio.frameLength > 0L)
            val duration = ceil(
                audio.frameLength.toDouble() / audio.format.frameRate /
                    CAT_VOICE_MIN_RATE * 1_000_000_000,
            ).toLong()
            assertTrue(duration + 300_000_000L <= STROKE_PURR_ECHO_GUARD_NANOS)
        }
    }
}
