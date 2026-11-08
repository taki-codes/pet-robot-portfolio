package com.taki.petrobot.storage

enum class StructuredEventType {
    CAMERA_SAMPLE,
    AUDIO_SAMPLE,
    PRESENCE_STATE,
    BREAK_STATE,
    THERMAL_PROFILE,
    TIMING_EVALUATION,
}

/**
 * Structured scalar evidence persisted for reliability analysis.
 *
 * This model deliberately has no field capable of carrying camera bytes, audio samples, face
 * features, transcripts, or other raw media.
 */
data class StructuredLogEntry(
    val id: Long = 0L,
    val elapsedRealtimeNanos: Long,
    val wallTimeEpochMillis: Long,
    val eventType: StructuredEventType,
    val visibleFaceCount: Int? = null,
    val meanAbsoluteYDifference: Double? = null,
    val audioRms: Double? = null,
    val audioDbfs: Double? = null,
    val audioPeak: Double? = null,
    val voiceCandidate: Boolean? = null,
    val presenceState: String? = null,
    val breakState: String? = null,
    val activityDetected: Boolean? = null,
    val thermalSeverity: String? = null,
    val sensingLoadProfile: String? = null,
    val cameraAnalysisStride: Int? = null,
    val timingRating: String? = null,
    val timingOffsetMillis: Long? = null,
) {
    init {
        require(id >= 0L)
        require(elapsedRealtimeNanos >= 0L)
        require(wallTimeEpochMillis >= 0L)
        require(visibleFaceCount == null || visibleFaceCount >= 0)
        require(
            meanAbsoluteYDifference == null ||
                (meanAbsoluteYDifference.isFinite() &&
                    meanAbsoluteYDifference in 0.0..1.0),
        )
        require(audioRms == null || (audioRms.isFinite() && audioRms in 0.0..1.0))
        require(audioDbfs == null || (audioDbfs.isFinite() && audioDbfs <= 0.0))
        require(audioPeak == null || (audioPeak.isFinite() && audioPeak in 0.0..1.0))
        require(cameraAnalysisStride == null || cameraAnalysisStride >= 1)
        require(timingOffsetMillis == null || timingOffsetMillis >= 0L)
    }
}
