package com.taki.petrobot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeGestureDetectorTest {
    @Test
    fun movingForTwoSecondsTriggersOnceAndNeverRepeatsWithinDrag() {
        val detector = StrokeGestureDetector()
        assertFalse(detector.sample(0f, 0f, 0L))
        for (i in 1..19) assertFalse(detector.sample(i * 10f, 0f, i * 100L))
        assertTrue(detector.sample(200f, 0f, 2_000L))
        for (i in 21..100) assertFalse(detector.sample(i * 10f, 0f, i * 100L))
    }

    @Test
    fun stationaryHoldAndSmallJitterNeverTrigger() {
        val detector = StrokeGestureDetector()
        detector.sample(0f, 0f, 0L)
        for (i in 1..100) assertFalse(detector.sample((i % 2).toFloat(), 0f, i * 100L))
        assertFalse(detector.sample(10f, 0f, 10_100L))
    }

    @Test
    fun pauseResetsMovingDurationInsteadOfCountingHeldTime() {
        val detector = StrokeGestureDetector()
        detector.sample(0f, 0f, 0L)
        for (i in 1..15) assertFalse(detector.sample(i * 10f, 0f, i * 100L))
        assertFalse(detector.sample(160f, 0f, 3_000L))
        for (i in 1..19) assertFalse(detector.sample(160f + i * 10f, 0f, 3_000L + i * 100L))
        assertTrue(detector.sample(360f, 0f, 5_000L))
    }

    @Test
    fun shortSwipeDoesNotTriggerAndSeparateDragStartsFresh() {
        val first = StrokeGestureDetector()
        first.sample(0f, 0f, 0L)
        for (i in 1..15) assertFalse(first.sample(i * 10f, 0f, i * 100L))
        val second = StrokeGestureDetector()
        second.sample(0f, 0f, 2_000L)
        for (i in 1..10) assertFalse(second.sample(0f, i * 10f, 2_000L + i * 100L))
    }

    @Test
    fun reversingDirectionCountsMovement() {
        val detector = StrokeGestureDetector()
        detector.sample(0f, 0f, 0L)
        for (i in 1..19) assertFalse(detector.sample(if (i % 2 == 0) 0f else 20f, 0f, i * 100L))
        assertTrue(detector.sample(0f, 0f, 2_000L))
    }

    @Test
    fun gapBoundaryAndBackwardTimestampAreHandled() {
        val atBoundary = StrokeGestureDetector(durationMillis = 350L)
        atBoundary.sample(0f, 0f, 0L)
        assertTrue(atBoundary.sample(10f, 0f, 350L))
        val overBoundary = StrokeGestureDetector(durationMillis = 350L)
        overBoundary.sample(0f, 0f, 100L)
        assertFalse(overBoundary.sample(10f, 0f, 451L))
        assertFalse(overBoundary.sample(20f, 0f, 1L))
        assertTrue(overBoundary.sample(30f, 0f, 351L))
    }
}
