package com.taki.petrobot

import java.io.File
import javax.sound.sampled.AudioSystem
import kotlin.math.ceil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleVoiceCoordinatorTest {
    private fun coordinator(
        sounds: MutableList<IdleVoice> = mutableListOf(),
        guard: LocalAudioPlaybackGuard = LocalAudioPlaybackGuard(),
        delay: Long = RANDOM_PURR_MIN_NANOS,
    ) = IdleVoiceCoordinator({ sounds.add(it); true }, guard, { delay })

    @Test
    fun statusRetainsPlaybackResultAfterOriginalLogIsGone() {
        val c = coordinator()
        c.startSession(0L)
        assertTrue(c.status(0L).contains("purrRemainingSeconds=600"))
        c.tick(RANDOM_PURR_MIN_NANOS, false)
        c.onInteraction(RANDOM_PURR_MIN_NANOS + 1)
        assertTrue(c.status(IDLE_SAD_AFTER_NANOS).contains("purr=played"))
        assertTrue(c.status(IDLE_SAD_AFTER_NANOS).contains("sad=pending"))
        c.stopSession()
        assertEquals("session=inactive", c.status(IDLE_SAD_AFTER_NANOS))
    }

    @Test
    fun randomRangeEndpointsAndPresenceIndependentPurr() {
        for (delay in listOf(RANDOM_PURR_MIN_NANOS, RANDOM_PURR_MAX_NANOS)) {
            val sounds = mutableListOf<IdleVoice>()
            val c = coordinator(sounds, delay = delay)
            assertEquals(delay, c.startSession(0L))
            assertEquals(IdleVoiceResult.NONE, c.tick(delay - 1, false))
            assertEquals(IdleVoiceResult.PURR_PLAYED, c.tick(delay, false))
            assertEquals(IdleVoiceResult.NONE, c.tick(delay + IDLE_PURR_GUARD_NANOS, false))
            assertEquals(listOf(IdleVoice.PURR), sounds)
        }
    }

    @Test
    fun interactionResetsSadButNeverReschedulesOrRearmsPurr() {
        val sounds = mutableListOf<IdleVoice>()
        val c = coordinator(sounds)
        c.startSession(0L)
        c.onInteraction(RANDOM_PURR_MIN_NANOS - 1)
        assertEquals(IdleVoiceResult.PURR_PLAYED, c.tick(RANDOM_PURR_MIN_NANOS, false))
        c.onInteraction(RANDOM_PURR_MIN_NANOS + 1)
        val sadAt = RANDOM_PURR_MIN_NANOS + 1 + IDLE_SAD_AFTER_NANOS
        assertEquals(IdleVoiceResult.NONE, c.tick(sadAt - 1, true))
        assertEquals(IdleVoiceResult.SAD_PLAYED, c.tick(sadAt, true))
        assertEquals(IdleVoiceResult.NONE, c.tick(sadAt * 10, true))
        c.onInteraction(sadAt * 10)
        assertEquals(IdleVoiceResult.NONE, c.tick(sadAt * 10 + RANDOM_PURR_MAX_NANOS, true))
        assertEquals(IdleVoiceResult.SAD_PLAYED, c.tick(sadAt * 10 + IDLE_SAD_AFTER_NANOS, true))
        assertEquals(listOf(IdleVoice.PURR, IdleVoice.SAD, IdleVoice.SAD), sounds)
    }

    @Test
    fun sadStillRequiresPresenceAndOwnPurrDoesNotResetSadClock() {
        val c = coordinator()
        c.startSession(0L)
        c.tick(RANDOM_PURR_MIN_NANOS, false)
        assertEquals(IdleVoiceResult.NONE, c.tick(IDLE_SAD_AFTER_NANOS, false))
        assertEquals(IdleVoiceResult.SAD_PLAYED, c.tick(IDLE_SAD_AFTER_NANOS + 1, true))
    }

    @Test
    fun stoppingDisarmsAndRestartDrawsNewDelay() {
        var draws = 0
        val c = IdleVoiceCoordinator({ true }, LocalAudioPlaybackGuard(), {
            draws++
            if (draws == 1) RANDOM_PURR_MIN_NANOS else RANDOM_PURR_MAX_NANOS
        })
        c.startSession(0L)
        c.stopSession()
        c.onInteraction(1L)
        assertEquals(IdleVoiceResult.NONE, c.tick(IDLE_SAD_AFTER_NANOS, true))
        assertEquals(RANDOM_PURR_MAX_NANOS, c.startSession(IDLE_SAD_AFTER_NANOS))
        assertEquals(IdleVoiceResult.NONE, c.tick(IDLE_SAD_AFTER_NANOS + RANDOM_PURR_MIN_NANOS, true))
        assertEquals(IdleVoiceResult.PURR_PLAYED, c.tick(IDLE_SAD_AFTER_NANOS + RANDOM_PURR_MAX_NANOS, true))
        assertEquals(2, draws)
    }

    @Test
    fun busyDefersAndResetPreservesGuard() {
        val guard = LocalAudioPlaybackGuard()
        val c = coordinator(guard = guard)
        c.startSession(0L)
        guard.suppressFor(RANDOM_PURR_MIN_NANOS, 100L)
        assertEquals(IdleVoiceResult.NONE, c.tick(RANDOM_PURR_MIN_NANOS, false))
        assertEquals(IdleVoiceResult.PURR_PLAYED, c.tick(RANDOM_PURR_MIN_NANOS + 100, false))
        val end = RANDOM_PURR_MIN_NANOS + 100 + IDLE_PURR_GUARD_NANOS
        c.onInteraction(RANDOM_PURR_MIN_NANOS + 101)
        c.stopSession()
        assertTrue(c.isBlockingOtherSounds(end - 1))
        assertFalse(guard.shouldProcessCapturedAudio(end - 1))
        assertFalse(c.isBlockingOtherSounds(end))
        assertTrue(guard.shouldProcessCapturedAudio(end))
    }

    @Test
    fun unavailableIsAttemptedOncePerIndependentVoice() {
        var attempts = 0
        val c = IdleVoiceCoordinator({ attempts++; false }, LocalAudioPlaybackGuard(), { RANDOM_PURR_MIN_NANOS })
        c.startSession(0L)
        assertEquals(IdleVoiceResult.UNAVAILABLE, c.tick(RANDOM_PURR_MIN_NANOS, false))
        assertEquals(IdleVoiceResult.NONE, c.tick(RANDOM_PURR_MIN_NANOS + 1, false))
        assertEquals(IdleVoiceResult.UNAVAILABLE, c.tick(IDLE_SAD_AFTER_NANOS, true))
        assertEquals(IdleVoiceResult.NONE, c.tick(IDLE_SAD_AFTER_NANOS + 1, true))
        assertEquals(2, attempts)
    }

    @Test
    fun delayedTickPlaysDueVoicesWithoutOverlap() {
        val guard = LocalAudioPlaybackGuard()
        val c = coordinator(guard = guard)
        c.startSession(0L)
        assertEquals(IdleVoiceResult.PURR_PLAYED, c.tick(IDLE_SAD_AFTER_NANOS, true))
        assertEquals(IdleVoiceResult.NONE, c.tick(IDLE_SAD_AFTER_NANOS + 1, true))
        assertEquals(IdleVoiceResult.SAD_PLAYED, c.tick(IDLE_SAD_AFTER_NANOS + IDLE_PURR_GUARD_NANOS, true))
        assertFalse(guard.shouldProcessCapturedAudio(IDLE_SAD_AFTER_NANOS + IDLE_PURR_GUARD_NANOS + IDLE_SAD_GUARD_NANOS - 1))
    }

    @Test
    fun backwardTimeRestartsSession() {
        val c = coordinator()
        c.startSession(100L)
        assertEquals(IdleVoiceResult.NONE, c.tick(0L, true))
        assertEquals(IdleVoiceResult.PURR_PLAYED, c.tick(RANDOM_PURR_MIN_NANOS, false))
    }

    @Test
    fun bundledSoundsFitSoundPoolAndEchoGuardsAtSlowestRate() {
        listOf("miina_stroke_purr.wav" to IDLE_PURR_GUARD_NANOS, "miina_sad.wav" to IDLE_SAD_GUARD_NANOS)
            .forEach { (name, guard) ->
                AudioSystem.getAudioInputStream(File("src/main/res/raw/$name")).use { audio ->
                    assertEquals(16, audio.format.sampleSizeInBits)
                    assertEquals(44_100f, audio.format.sampleRate, 0f)
                    assertTrue(audio.frameLength > 0)
                    assertTrue(audio.frameLength * audio.format.frameSize < 1_000_000L)
                    val duration = ceil(audio.frameLength / audio.format.frameRate.toDouble() /
                        CAT_VOICE_MIN_RATE * 1_000_000_000).toLong()
                    assertTrue(duration + 300_000_000L <= guard)
                }
            }
    }
}
