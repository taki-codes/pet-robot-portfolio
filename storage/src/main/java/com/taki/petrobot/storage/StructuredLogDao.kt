package com.taki.petrobot.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface StructuredLogDao {
    @Insert
    fun insert(entry: StructuredLogEntity): Long

    @Query("SELECT COUNT(*) FROM structured_logs")
    fun count(): Long

    @Query(
        """
        SELECT * FROM (
            SELECT * FROM structured_logs
            ORDER BY id DESC
            LIMIT :limit
        )
        ORDER BY id ASC
        """,
    )
    fun readRecent(limit: Int): List<StructuredLogEntity>

    @Query(
        """
        SELECT
            COUNT(*) AS totalEventCount,
            COALESCE(SUM(CASE WHEN eventType = 'CAMERA_SAMPLE' THEN 1 ELSE 0 END), 0)
                AS cameraSampleCount,
            COALESCE(SUM(CASE
                WHEN eventType = 'CAMERA_SAMPLE' AND visibleFaceCount > 0 THEN 1 ELSE 0
            END), 0) AS faceObservedSampleCount,
            COALESCE(SUM(CASE WHEN eventType = 'AUDIO_SAMPLE' THEN 1 ELSE 0 END), 0)
                AS audioSampleCount,
            COALESCE(SUM(CASE
                WHEN eventType = 'AUDIO_SAMPLE' AND voiceCandidate = 1 THEN 1 ELSE 0
            END), 0) AS voiceCandidateSampleCount,
            COALESCE(SUM(CASE
                WHEN eventType = 'PRESENCE_STATE' AND presenceState = 'PRESENT' THEN 1 ELSE 0
            END), 0) AS presentTransitionCount,
            COALESCE(SUM(CASE
                WHEN eventType = 'BREAK_STATE' AND breakState = 'CANDIDATE' THEN 1 ELSE 0
            END), 0) AS breakCandidateTransitionCount,
            COALESCE(SUM(CASE WHEN eventType = 'TIMING_EVALUATION' THEN 1 ELSE 0 END), 0)
                AS timingEvaluationCount,
            COALESCE(SUM(CASE
                WHEN eventType = 'TIMING_EVALUATION' AND timingRating = 'TOO_EARLY'
                THEN 1 ELSE 0
            END), 0) AS tooEarlyCount,
            COALESCE(SUM(CASE
                WHEN eventType = 'TIMING_EVALUATION' AND timingRating = 'JUST_RIGHT'
                THEN 1 ELSE 0
            END), 0) AS justRightCount,
            COALESCE(SUM(CASE
                WHEN eventType = 'TIMING_EVALUATION' AND timingRating = 'TOO_LATE'
                THEN 1 ELSE 0
            END), 0) AS tooLateCount
        FROM structured_logs
        WHERE wallTimeEpochMillis BETWEEN :startEpochMillis AND :endInclusiveEpochMillis
        """,
    )
    fun readSummaryCounts(
        startEpochMillis: Long,
        endInclusiveEpochMillis: Long,
    ): StructuredLogSummaryCounts

    @Query(
        """
        SELECT * FROM structured_logs
        WHERE wallTimeEpochMillis BETWEEN :startEpochMillis AND :endInclusiveEpochMillis
          AND (
              wallTimeEpochMillis > :afterWallTimeEpochMillis
              OR (wallTimeEpochMillis = :afterWallTimeEpochMillis AND id > :afterId)
          )
          AND eventType IN ('CAMERA_SAMPLE', 'AUDIO_SAMPLE', 'BREAK_STATE')
        ORDER BY wallTimeEpochMillis ASC, id ASC
        LIMIT :limit
        """,
    )
    fun readReplayPage(
        startEpochMillis: Long,
        endInclusiveEpochMillis: Long,
        afterWallTimeEpochMillis: Long,
        afterId: Long,
        limit: Int,
    ): List<StructuredLogEntity>

    @Query("DELETE FROM structured_logs WHERE wallTimeEpochMillis < :cutoffEpochMillis")
    fun deleteBefore(cutoffEpochMillis: Long): Int
}
