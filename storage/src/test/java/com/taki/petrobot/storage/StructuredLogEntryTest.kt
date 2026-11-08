package com.taki.petrobot.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class StructuredLogEntryTest {
    @Test
    fun entityRoundTripPreservesEveryScalarField() {
        val entry = StructuredLogEntry(
            id = 7L,
            elapsedRealtimeNanos = 123L,
            wallTimeEpochMillis = 456L,
            eventType = StructuredEventType.TIMING_EVALUATION,
            visibleFaceCount = 2,
            meanAbsoluteYDifference = 0.25,
            audioRms = 0.1,
            audioDbfs = -20.0,
            audioPeak = 0.4,
            voiceCandidate = true,
            presenceState = "PRESENT",
            breakState = "CANDIDATE",
            activityDetected = false,
            thermalSeverity = "MODERATE",
            sensingLoadProfile = "REDUCED",
            cameraAnalysisStride = 2,
            timingRating = "JUST_RIGHT",
            timingOffsetMillis = 1_250L,
        )

        assertEquals(entry, entry.toEntity().toEntry())
    }

    @Test
    fun entryWithoutOptionalEvidenceIsValid() {
        val entry = StructuredLogEntry(
            elapsedRealtimeNanos = 0L,
            wallTimeEpochMillis = 0L,
            eventType = StructuredEventType.THERMAL_PROFILE,
        )

        assertEquals(StructuredEventType.THERMAL_PROFILE, entry.eventType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeTimestampIsRejected() {
        StructuredLogEntry(
            elapsedRealtimeNanos = -1L,
            wallTimeEpochMillis = 0L,
            eventType = StructuredEventType.CAMERA_SAMPLE,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDifferenceIsRejected() {
        StructuredLogEntry(
            elapsedRealtimeNanos = 0L,
            wallTimeEpochMillis = 0L,
            eventType = StructuredEventType.CAMERA_SAMPLE,
            meanAbsoluteYDifference = 1.1,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidStrideIsRejected() {
        StructuredLogEntry(
            elapsedRealtimeNanos = 0L,
            wallTimeEpochMillis = 0L,
            eventType = StructuredEventType.THERMAL_PROFILE,
            cameraAnalysisStride = 0,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeTimingOffsetIsRejected() {
        StructuredLogEntry(
            elapsedRealtimeNanos = 0L,
            wallTimeEpochMillis = 0L,
            eventType = StructuredEventType.TIMING_EVALUATION,
            timingRating = "TOO_LATE",
            timingOffsetMillis = -1L,
        )
    }
}
