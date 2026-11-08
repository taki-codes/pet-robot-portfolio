package com.taki.petrobot.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingingCandidateDetectorTest {
    private val detector = SingingCandidateDetector()
    private val step = 100_000_000L

    @Test fun sustainedPitchNeedsAboutTwoSecondsThenEndsOnSilence() {
        repeat(19) { assertFalse(detector.accept(it * step, 220.0).candidate) }
        assertTrue(detector.accept(19 * step, 220.0).candidate)
        assertFalse(detector.accept(20 * step, null).candidate)
    }

    @Test fun notesCanChangeWhileMostAdjacentFramesStayStable() {
        var status = detector.accept(0L, 220.0)
        repeat(29) { i ->
            status = detector.accept((i + 1) * step, if (i / 5 % 2 == 0) 220.0 else 330.0)
        }
        assertTrue(status.candidate)
    }

    @Test fun silenceAndRapidlyChangingPitchAreNotCandidates() {
        repeat(30) { assertFalse(detector.accept(it * step, null).candidate) }
        detector.reset()
        repeat(30) {
            assertFalse(detector.accept(it * step, if (it % 2 == 0) 110.0 else 220.0).candidate)
        }
    }

    @Test fun briefVoicedBurstsDoNotFillTheObservationWindow() {
        repeat(40) {
            assertFalse(detector.accept(it * step, if (it % 5 < 3) 220.0 else null).candidate)
        }
    }

    @Test fun gapBackwardTimeAndExplicitResetDiscardOldEvidence() {
        repeat(20) { detector.accept(it * step, 220.0) }
        assertFalse(detector.accept(30 * step, 220.0).ready)
        repeat(20) { detector.accept((31 + it) * step, 220.0) }
        assertFalse(detector.accept(step, 220.0).ready)
        repeat(20) { detector.accept((2 + it) * step, 220.0) }
        detector.reset()
        assertFalse(detector.accept(22 * step, 220.0).ready)
    }

    @Test fun relaxedBoundaryAcceptsFourteenVoicedWindowsAndNineStablePairs() {
        val status = analyzeRuns(listOf(3, 3, 3, 3, 2), leadingSilent = 2)
        assertTrue(status.ready)
        assertEquals(14, status.voicedWindows)
        assertEquals(9, status.stablePairs)
        assertTrue(status.candidate)
    }

    @Test fun thirteenVoicedWindowsStillFailEvenWithNineStablePairs() {
        val status = analyzeRuns(listOf(4, 3, 3, 3), leadingSilent = 4)
        assertTrue(status.ready)
        assertEquals(13, status.voicedWindows)
        assertEquals(9, status.stablePairs)
        assertFalse(status.candidate)
    }

    @Test fun eightStablePairsStillFailEvenWithFourteenVoicedWindows() {
        val status = analyzeRuns(listOf(3, 3, 2, 2, 2, 2), leadingSilent = 1)
        assertTrue(status.ready)
        assertEquals(14, status.voicedWindows)
        assertEquals(8, status.stablePairs)
        assertFalse(status.candidate)
    }

    private fun analyzeRuns(runs: List<Int>, leadingSilent: Int): SingingCandidateStatus {
        val pitches = mutableListOf<Double?>()
        repeat(leadingSilent) { pitches.add(null) }
        runs.forEachIndexed { index, count ->
            if (index > 0) pitches.add(null)
            repeat(count) { pitches.add(220.0) }
        }
        assertEquals(20, pitches.size)
        detector.reset()
        var result = detector.accept(0L, pitches.first())
        for (index in 1 until pitches.size) {
            result = detector.accept(index * step, pitches[index])
        }
        return result
    }

    @Test fun fastCallbacksCannotManufactureTwoSecondsOfEvidence() {
        repeat(100) { assertFalse(detector.accept(it * 1_000_000L, 220.0).ready) }
    }
}
