package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimingEvaluationTest {
    @Test
    fun evaluationComputesMillisecondsSinceBreakStateChange() {
        val record = createTimingEvaluationRecord(
            rating = TimingRating.JUST_RIGHT,
            pressedAtNanos = 5_250_000_000L,
            referenceBreakState = "CANDIDATE",
            breakStateChangedAtNanos = 4_000_000_000L,
        )

        assertEquals(TimingRating.JUST_RIGHT, record.rating)
        assertEquals("CANDIDATE", record.referenceBreakState)
        assertEquals(1_250L, record.millisSinceBreakStateChange)
    }

    @Test
    fun evaluationBeforeFirstBreakObservationHasNoOffset() {
        val record = createTimingEvaluationRecord(
            rating = TimingRating.TOO_EARLY,
            pressedAtNanos = 10L,
            referenceBreakState = "UNKNOWN",
            breakStateChangedAtNanos = 0L,
        )

        assertNull(record.millisSinceBreakStateChange)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativePressTimestampIsRejected() {
        createTimingEvaluationRecord(
            rating = TimingRating.TOO_LATE,
            pressedAtNanos = -1L,
            referenceBreakState = "IDLE",
            breakStateChangedAtNanos = 0L,
        )
    }
}
