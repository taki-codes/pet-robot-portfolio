package com.taki.petrobot.storage

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StructuredLogSummaryTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")
    private val now = Instant.parse("2026-09-17T12:34:56Z").toEpochMilli()

    @Test
    fun currentWindowsUseLocalCalendarDayBoundaries() {
        val windows = currentSummaryWindows(now, tokyo)

        assertEquals(SummaryPeriod.DAILY, windows[0].period)
        assertEquals(
            Instant.parse("2026-09-16T15:00:00Z").toEpochMilli(),
            windows[0].startEpochMillis,
        )
        assertEquals(now, windows[0].endInclusiveEpochMillis)
        assertEquals(SummaryPeriod.SEVEN_DAYS, windows[1].period)
        assertEquals(
            Instant.parse("2026-09-10T15:00:00Z").toEpochMilli(),
            windows[1].startEpochMillis,
        )
        assertEquals(now, windows[1].endInclusiveEpochMillis)
    }

    @Test
    fun summarizeCountsOnlyScalarEvidenceInsideWindow() {
        val window = currentSummaryWindows(now, tokyo).first()
        val atStart = window.startEpochMillis
        val entries = listOf(
            entry(atStart - 1L, StructuredEventType.CAMERA_SAMPLE, faces = 1),
            entry(atStart, StructuredEventType.CAMERA_SAMPLE, faces = 0),
            entry(atStart + 1L, StructuredEventType.CAMERA_SAMPLE, faces = 2),
            entry(atStart + 2L, StructuredEventType.AUDIO_SAMPLE, voice = false),
            entry(atStart + 3L, StructuredEventType.AUDIO_SAMPLE, voice = true),
            entry(atStart + 4L, StructuredEventType.PRESENCE_STATE, presence = "PRESENT"),
            entry(atStart + 5L, StructuredEventType.BREAK_STATE, breakState = "CANDIDATE"),
            entry(atStart + 6L, StructuredEventType.TIMING_EVALUATION, rating = "TOO_EARLY"),
            entry(atStart + 7L, StructuredEventType.TIMING_EVALUATION, rating = "JUST_RIGHT"),
            entry(now, StructuredEventType.TIMING_EVALUATION, rating = "TOO_LATE"),
            entry(now + 1L, StructuredEventType.AUDIO_SAMPLE, voice = true),
        )

        val summary = StructuredLogSummarizer.summarize(window, entries)

        assertEquals(9L, summary.totalEventCount)
        assertEquals(2L, summary.cameraSampleCount)
        assertEquals(1L, summary.faceObservedSampleCount)
        assertEquals(2L, summary.audioSampleCount)
        assertEquals(1L, summary.voiceCandidateSampleCount)
        assertEquals(1L, summary.presentTransitionCount)
        assertEquals(1L, summary.breakCandidateTransitionCount)
        assertEquals(3L, summary.timingEvaluationCount)
        assertEquals(1L, summary.tooEarlyCount)
        assertEquals(1L, summary.justRightCount)
        assertEquals(1L, summary.tooLateCount)
    }

    @Test
    fun emptyWindowProducesZeroCounts() {
        val summary = StructuredLogSummarizer.summarize(
            currentSummaryWindows(now, tokyo).first(),
            emptyList(),
        )

        assertEquals(0L, summary.totalEventCount)
        assertEquals(0L, summary.timingEvaluationCount)
    }

    private fun entry(
        wallTime: Long,
        type: StructuredEventType,
        faces: Int? = null,
        voice: Boolean? = null,
        presence: String? = null,
        breakState: String? = null,
        rating: String? = null,
    ) = StructuredLogEntry(
        elapsedRealtimeNanos = 1L,
        wallTimeEpochMillis = wallTime,
        eventType = type,
        visibleFaceCount = faces,
        voiceCandidate = voice,
        presenceState = presence,
        breakState = breakState,
        timingRating = rating,
    )
}
