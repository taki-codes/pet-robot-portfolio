package com.taki.petrobot.sensing

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Scalar audio measurements derived from one in-memory PCM window.
 *
 * Raw audio is neither retained by this value nor persisted. [voiceCandidate] is only a
 * threshold-based VAD-lite signal; it does not recognize or transcribe speech.
 */
data class AudioEnvelopeSample(
    val timestampNanos: Long,
    val sampleCount: Int,
    val rms: Double,
    val dbfs: Double,
    val peak: Double,
    val voiceCandidate: Boolean,
) {
    init {
        require(timestampNanos >= 0L)
        require(sampleCount > 0)
        require(rms in 0.0..1.0)
        require(dbfs in AudioEnvelopeAnalyzer.MIN_DBFS..0.0)
        require(peak in 0.0..1.0)
    }
}

/**
 * Converts a short PCM window into an RMS envelope and a fixed-threshold VAD-lite result.
 */
class AudioEnvelopeAnalyzer(
    private val voiceThresholdDbfs: Double = DEFAULT_VOICE_THRESHOLD_DBFS,
) {
    init {
        require(voiceThresholdDbfs in MIN_DBFS..0.0)
    }

    fun analyze(
        timestampNanos: Long,
        pcm: ShortArray,
        sampleCount: Int = pcm.size,
    ): AudioEnvelopeSample {
        require(timestampNanos >= 0L)
        require(sampleCount in 1..pcm.size)

        var squareSum = 0.0
        var peakMagnitude = 0
        for (index in 0 until sampleCount) {
            val magnitude = abs(pcm[index].toInt())
            val normalized = magnitude / PCM_FULL_SCALE
            squareSum += normalized * normalized
            peakMagnitude = max(peakMagnitude, magnitude)
        }

        val rms = sqrt(squareSum / sampleCount)
        val dbfs = if (rms == 0.0) {
            MIN_DBFS
        } else {
            max(MIN_DBFS, 20.0 * log10(rms))
        }

        return AudioEnvelopeSample(
            timestampNanos = timestampNanos,
            sampleCount = sampleCount,
            rms = rms,
            dbfs = dbfs,
            peak = peakMagnitude / PCM_FULL_SCALE,
            voiceCandidate = dbfs >= voiceThresholdDbfs,
        )
    }

    companion object {
        const val MIN_DBFS = -120.0
        const val DEFAULT_VOICE_THRESHOLD_DBFS = -42.0
        private const val PCM_FULL_SCALE = 32768.0
    }
}
