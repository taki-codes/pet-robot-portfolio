package com.taki.petrobot.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "structured_logs",
    indices = [
        Index(value = ["elapsedRealtimeNanos"]),
        Index(value = ["wallTimeEpochMillis"]),
        Index(value = ["eventType"]),
    ],
)
internal data class StructuredLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val elapsedRealtimeNanos: Long,
    val wallTimeEpochMillis: Long,
    val eventType: String,
    val visibleFaceCount: Int?,
    val meanAbsoluteYDifference: Double?,
    val audioRms: Double?,
    val audioDbfs: Double?,
    val audioPeak: Double?,
    val voiceCandidate: Boolean?,
    val presenceState: String?,
    val breakState: String?,
    val activityDetected: Boolean?,
    val thermalSeverity: String?,
    val sensingLoadProfile: String?,
    val cameraAnalysisStride: Int?,
    val timingRating: String?,
    val timingOffsetMillis: Long?,
)

internal fun StructuredLogEntry.toEntity() = StructuredLogEntity(
    id = id,
    elapsedRealtimeNanos = elapsedRealtimeNanos,
    wallTimeEpochMillis = wallTimeEpochMillis,
    eventType = eventType.name,
    visibleFaceCount = visibleFaceCount,
    meanAbsoluteYDifference = meanAbsoluteYDifference,
    audioRms = audioRms,
    audioDbfs = audioDbfs,
    audioPeak = audioPeak,
    voiceCandidate = voiceCandidate,
    presenceState = presenceState,
    breakState = breakState,
    activityDetected = activityDetected,
    thermalSeverity = thermalSeverity,
    sensingLoadProfile = sensingLoadProfile,
    cameraAnalysisStride = cameraAnalysisStride,
    timingRating = timingRating,
    timingOffsetMillis = timingOffsetMillis,
)

internal fun StructuredLogEntity.toEntry() = StructuredLogEntry(
    id = id,
    elapsedRealtimeNanos = elapsedRealtimeNanos,
    wallTimeEpochMillis = wallTimeEpochMillis,
    eventType = StructuredEventType.valueOf(eventType),
    visibleFaceCount = visibleFaceCount,
    meanAbsoluteYDifference = meanAbsoluteYDifference,
    audioRms = audioRms,
    audioDbfs = audioDbfs,
    audioPeak = audioPeak,
    voiceCandidate = voiceCandidate,
    presenceState = presenceState,
    breakState = breakState,
    activityDetected = activityDetected,
    thermalSeverity = thermalSeverity,
    sensingLoadProfile = sensingLoadProfile,
    cameraAnalysisStride = cameraAnalysisStride,
    timingRating = timingRating,
    timingOffsetMillis = timingOffsetMillis,
)
