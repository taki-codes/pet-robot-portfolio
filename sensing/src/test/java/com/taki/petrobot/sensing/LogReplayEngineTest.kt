package com.taki.petrobot.sensing

import org.junit.Assert.assertEquals
import org.junit.Test

class LogReplayEngineTest {
    @Test
    fun alternativeQuietThresholdsCanProduceDifferentCandidateCounts() {
        val engine = LogReplayEngine(defaultReplayProfiles())
        listOf(
            sample(0L, faceCount = 1, voice = true),
            sample(2_000_000_000L, faceCount = 1),
            sample(2_200_000_000L, faceCount = 1, voice = true),
            sample(4_300_000_000L, faceCount = 1),
            sample(5_300_000_000L, faceCount = 1),
        ).forEach(engine::accept)

        val results = engine.results().associateBy { it.profile.name }

        assertEquals(2L, results.getValue("CURRENT_1_5S").breakCandidateCount)
        assertEquals(2L, results.getValue("QUIET_2_0S").breakCandidateCount)
        assertEquals(1L, results.getValue("QUIET_2_5S").breakCandidateCount)
        assertEquals(1L, results.getValue("QUIET_3_0S").breakCandidateCount)
        results.values.forEach {
            assertEquals(5L, it.sampleCount)
            assertEquals(1L, it.sessionCount)
            assertEquals(1L, it.presentTransitionCount)
        }
    }

    @Test
    fun longGapStartsNewSessionInsteadOfCreatingFalseCandidate() {
        val engine = LogReplayEngine(
            profiles = listOf(
                ReplayThresholdProfile(
                    name = "TEST",
                    quietPeriodNanos = 1_500_000_000L,
                ),
            ),
        )
        engine.accept(sample(0L, faceCount = 1, voice = true))
        engine.accept(sample(20_000_000_000L, faceCount = 1))

        val result = engine.results().single()

        assertEquals(2L, result.sessionCount)
        assertEquals(0L, result.breakCandidateCount)
        assertEquals(2L, result.presentTransitionCount)
    }

    @Test
    fun emptyReplayHasZeroCounts() {
        val result = LogReplayEngine(defaultReplayProfiles()).results().first()

        assertEquals(0L, result.sampleCount)
        assertEquals(0L, result.sessionCount)
        assertEquals(0L, result.presentTransitionCount)
        assertEquals(0L, result.breakCandidateCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfOrderSampleIsRejected() {
        val engine = LogReplayEngine(defaultReplayProfiles())
        engine.accept(sample(2L))
        engine.accept(sample(1L))
    }

    private fun sample(
        timestampNanos: Long,
        faceCount: Int = 0,
        difference: Double? = null,
        voice: Boolean = false,
    ) = ReplayScalarSample(
        timestampNanos = timestampNanos,
        visibleFaceCount = faceCount,
        meanAbsoluteYDifference = difference,
        voiceCandidate = voice,
    )
}
