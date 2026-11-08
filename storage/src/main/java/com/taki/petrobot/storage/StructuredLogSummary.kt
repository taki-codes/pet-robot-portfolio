package com.taki.petrobot.storage

import java.time.Instant
import java.time.ZoneId

enum class SummaryPeriod {
    DAILY,
    SEVEN_DAYS,
}

data class StructuredLogSummaryWindow(
    val period: SummaryPeriod,
    val startEpochMillis: Long,
    val endInclusiveEpochMillis: Long,
) {
    init {
        require(startEpochMillis >= 0L)
        require(endInclusiveEpochMillis >= startEpochMillis)
    }
}

fun currentSummaryWindows(
    nowEpochMillis: Long,
    zoneId: ZoneId,
): List<StructuredLogSummaryWindow> {
    require(nowEpochMillis >= 0L)
    val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
    val dailyStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val sevenDayStart = today.minusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return listOf(
        StructuredLogSummaryWindow(
            period = SummaryPeriod.DAILY,
            startEpochMillis = dailyStart,
            endInclusiveEpochMillis = nowEpochMillis,
        ),
        StructuredLogSummaryWindow(
            period = SummaryPeriod.SEVEN_DAYS,
            startEpochMillis = sevenDayStart,
            endInclusiveEpochMillis = nowEpochMillis,
        ),
    )
}

data class StructuredLogSummary(
    val window: StructuredLogSummaryWindow,
    val totalEventCount: Long,
    val cameraSampleCount: Long,
    val faceObservedSampleCount: Long,
    val audioSampleCount: Long,
    val voiceCandidateSampleCount: Long,
    val presentTransitionCount: Long,
    val breakCandidateTransitionCount: Long,
    val timingEvaluationCount: Long,
    val tooEarlyCount: Long,
    val justRightCount: Long,
    val tooLateCount: Long,
)

internal data class StructuredLogSummaryCounts(
    val totalEventCount: Long,
    val cameraSampleCount: Long,
    val faceObservedSampleCount: Long,
    val audioSampleCount: Long,
    val voiceCandidateSampleCount: Long,
    val presentTransitionCount: Long,
    val breakCandidateTransitionCount: Long,
    val timingEvaluationCount: Long,
    val tooEarlyCount: Long,
    val justRightCount: Long,
    val tooLateCount: Long,
) {
    fun withWindow(window: StructuredLogSummaryWindow) = StructuredLogSummary(
        window = window,
        totalEventCount = totalEventCount,
        cameraSampleCount = cameraSampleCount,
        faceObservedSampleCount = faceObservedSampleCount,
        audioSampleCount = audioSampleCount,
        voiceCandidateSampleCount = voiceCandidateSampleCount,
        presentTransitionCount = presentTransitionCount,
        breakCandidateTransitionCount = breakCandidateTransitionCount,
        timingEvaluationCount = timingEvaluationCount,
        tooEarlyCount = tooEarlyCount,
        justRightCount = justRightCount,
        tooLateCount = tooLateCount,
    )
}

object StructuredLogSummarizer {
    fun summarize(
        window: StructuredLogSummaryWindow,
        entries: List<StructuredLogEntry>,
    ): StructuredLogSummary {
        val inWindow = entries.filter {
            it.wallTimeEpochMillis in window.startEpochMillis..window.endInclusiveEpochMillis
        }
        return StructuredLogSummary(
            window = window,
            totalEventCount = inWindow.size.toLong(),
            cameraSampleCount = inWindow.countEvent(StructuredEventType.CAMERA_SAMPLE),
            faceObservedSampleCount = inWindow.count {
                it.eventType == StructuredEventType.CAMERA_SAMPLE &&
                    (it.visibleFaceCount ?: 0) > 0
            }.toLong(),
            audioSampleCount = inWindow.countEvent(StructuredEventType.AUDIO_SAMPLE),
            voiceCandidateSampleCount = inWindow.count {
                it.eventType == StructuredEventType.AUDIO_SAMPLE && it.voiceCandidate == true
            }.toLong(),
            presentTransitionCount = inWindow.count {
                it.eventType == StructuredEventType.PRESENCE_STATE &&
                    it.presenceState == PRESENT_STATE
            }.toLong(),
            breakCandidateTransitionCount = inWindow.count {
                it.eventType == StructuredEventType.BREAK_STATE &&
                    it.breakState == CANDIDATE_STATE
            }.toLong(),
            timingEvaluationCount = inWindow.countEvent(StructuredEventType.TIMING_EVALUATION),
            tooEarlyCount = inWindow.countRating(TOO_EARLY_RATING),
            justRightCount = inWindow.countRating(JUST_RIGHT_RATING),
            tooLateCount = inWindow.countRating(TOO_LATE_RATING),
        )
    }

    private fun List<StructuredLogEntry>.countEvent(eventType: StructuredEventType): Long =
        count { it.eventType == eventType }.toLong()

    private fun List<StructuredLogEntry>.countRating(rating: String): Long =
        count {
            it.eventType == StructuredEventType.TIMING_EVALUATION &&
                it.timingRating == rating
        }.toLong()

    private const val PRESENT_STATE = "PRESENT"
    private const val CANDIDATE_STATE = "CANDIDATE"
    private const val TOO_EARLY_RATING = "TOO_EARLY"
    private const val JUST_RIGHT_RATING = "JUST_RIGHT"
    private const val TOO_LATE_RATING = "TOO_LATE"
}
