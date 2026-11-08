package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalMeowCoordinatorTest {
    private val guard = LocalAudioPlaybackGuard()
    private var plays = 0
    private var available = true
    private val coordinator = ArrivalMeowCoordinator(
        playMeow = { plays += 1; available },
        playbackGuard = guard,
    )

    @Test
    fun initialActiveMoodDoesNotGreet() {
        assertEquals(ArrivalMeowResult.NONE, active(0L))
        assertEquals(0, plays)
    }

    @Test
    fun returnJustBeforeThreeMinutesIsSilent() {
        rest(0L)
        assertEquals(ArrivalMeowResult.SHORT_REST, active(ARRIVAL_MIN_REST_NANOS - 1L))
        assertEquals(0, plays)
    }

    @Test
    fun returnAtThreeMinutesPlaysOnceAndGuardsAudio() {
        rest(0L)
        rest(60_000_000_000L)
        assertEquals(ArrivalMeowResult.PLAYED, active(ARRIVAL_MIN_REST_NANOS))
        assertFalse(guard.shouldProcessCapturedAudio(ARRIVAL_MIN_REST_NANOS))
        assertTrue(
            guard.shouldProcessCapturedAudio(
                ARRIVAL_MIN_REST_NANOS + LOCAL_MEOW_ECHO_GUARD_NANOS,
            ),
        )
        assertEquals(ArrivalMeowResult.NONE, active(ARRIVAL_MIN_REST_NANOS + 1L))
        assertEquals(
            ArrivalMeowResult.NONE,
            coordinator.onMood(PetFaceMood.BREAK_READY, ARRIVAL_MIN_REST_NANOS + 2L),
        )
        assertEquals(1, plays)
    }

    @Test
    fun interruptedRestDoesNotAccumulate() {
        rest(0L)
        active(120_000_000_000L)
        rest(121_000_000_000L)
        assertEquals(ArrivalMeowResult.SHORT_REST, active(240_000_000_000L))
        assertEquals(0, plays)
    }

    @Test
    fun anotherFullRestAllowsAnotherGreeting() {
        rest(0L)
        active(ARRIVAL_MIN_REST_NANOS)
        rest(ARRIVAL_MIN_REST_NANOS + 10_000_000_000L)
        assertEquals(
            ArrivalMeowResult.PLAYED,
            active(2 * ARRIVAL_MIN_REST_NANOS + 10_000_000_000L),
        )
        assertEquals(2, plays)
    }

    @Test
    fun unavailablePlaybackIsNotRetriedAndDoesNotGuardAudio() {
        available = false
        rest(0L)
        assertEquals(ArrivalMeowResult.UNAVAILABLE, active(ARRIVAL_MIN_REST_NANOS))
        assertTrue(guard.shouldProcessCapturedAudio(ARRIVAL_MIN_REST_NANOS))
        available = true
        assertEquals(ArrivalMeowResult.NONE, active(ARRIVAL_MIN_REST_NANOS + 1L))
        assertEquals(1, plays)
    }

    @Test
    fun existingLocalSoundSkipsGreetingWithoutRetryOrGuardExtension() {
        rest(0L)
        guard.suppressFor(ARRIVAL_MIN_REST_NANOS - 1L, 100L)
        assertEquals(ArrivalMeowResult.BUSY, active(ARRIVAL_MIN_REST_NANOS))
        assertEquals(ArrivalMeowResult.NONE, active(ARRIVAL_MIN_REST_NANOS + 100L))
        assertTrue(guard.shouldProcessCapturedAudio(ARRIVAL_MIN_REST_NANOS + 100L))
        assertEquals(0, plays)
    }

    @Test
    fun lifecycleResetDiscardsRestTime() {
        rest(0L)
        coordinator.reset()
        assertEquals(ArrivalMeowResult.NONE, active(ARRIVAL_MIN_REST_NANOS))
        rest(ARRIVAL_MIN_REST_NANOS + 1L)
        assertEquals(
            ArrivalMeowResult.SHORT_REST,
            active(2 * ARRIVAL_MIN_REST_NANOS),
        )
        assertEquals(0, plays)
    }

    @Test
    fun lifecycleResetPreservesPlaybackGuard() {
        rest(0L)
        active(ARRIVAL_MIN_REST_NANOS)
        coordinator.reset()
        assertFalse(guard.shouldProcessCapturedAudio(ARRIVAL_MIN_REST_NANOS + 1L))
    }

    @Test
    fun delayedDeliveryDoesNotTurnShortRestIntoThreeMinutes() {
        coordinator.onMood(PetFaceMood.RESTING, 0L, 1_000_000_000L)
        assertEquals(
            ArrivalMeowResult.SHORT_REST,
            coordinator.onMood(
                PetFaceMood.ATTENTIVE,
                ARRIVAL_MIN_REST_NANOS - 1L,
                ARRIVAL_MIN_REST_NANOS + 5_000_000_000L,
            ),
        )
        assertEquals(0, plays)
    }

    @Test
    fun delayedGreetingGuardsAudioFromActualPlaybackTime() {
        rest(0L)
        val playedAt = ARRIVAL_MIN_REST_NANOS + 5_000_000_000L
        assertEquals(
            ArrivalMeowResult.PLAYED,
            coordinator.onMood(PetFaceMood.ATTENTIVE, ARRIVAL_MIN_REST_NANOS, playedAt),
        )
        assertFalse(guard.shouldProcessCapturedAudio(playedAt))
        assertFalse(
            guard.shouldProcessCapturedAudio(playedAt + LOCAL_MEOW_ECHO_GUARD_NANOS - 1L),
        )
        assertTrue(guard.shouldProcessCapturedAudio(playedAt + LOCAL_MEOW_ECHO_GUARD_NANOS))
    }

    @Test
    fun busyCheckUsesPlaybackTimeNotOldObservation() {
        rest(0L)
        guard.suppressFor(ARRIVAL_MIN_REST_NANOS, LOCAL_MEOW_ECHO_GUARD_NANOS)
        assertEquals(
            ArrivalMeowResult.PLAYED,
            coordinator.onMood(
                PetFaceMood.ATTENTIVE,
                ARRIVAL_MIN_REST_NANOS,
                ARRIVAL_MIN_REST_NANOS + 5_000_000_000L,
            ),
        )
        assertEquals(1, plays)
    }

    private fun rest(at: Long) = coordinator.onMood(PetFaceMood.RESTING, at)

    private fun active(at: Long) = coordinator.onMood(PetFaceMood.ATTENTIVE, at)
}
