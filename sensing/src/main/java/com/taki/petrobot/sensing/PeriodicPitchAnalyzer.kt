package com.taki.petrobot.sensing

/**
 * A bounded, YIN-style pitch estimate, not a speech/singing classifier.
 * Uses a fixed integration width and the cumulative mean normalized difference.
 * No fallback pitch is returned when no sufficiently periodic minimum is found.
 * Reference: de Cheveigne & Kawahara (2002), DOI 10.1121/1.1458024, steps 2–4.
 * PCM is read synchronously; only the derived difference scratch array is reused.
 */
class PeriodicPitchAnalyzer {
    private val normalizedDifference = DoubleArray(MAX_LAG + 2)

    fun estimateHz(pcm: ShortArray, sampleCount: Int = pcm.size): Double? {
        require(sampleCount in 0..pcm.size)
        if (sampleCount < REQUIRED_SAMPLES) return null
        // Use the newest bounded frame, even if a caller supplies a larger window.
        val offset = sampleCount - REQUIRED_SAMPLES
        var sum = 0.0
        var sumSquares = 0.0
        for (i in offset until sampleCount) {
            val value = pcm[i].toDouble() / 32768.0
            sum += value
            sumSquares += value * value
        }
        val mean = sum / REQUIRED_SAMPLES
        val variance = (sumSquares / REQUIRED_SAMPLES - mean * mean).coerceAtLeast(0.0)
        if (variance < MIN_RMS * MIN_RMS) return null

        var cumulativeDifference = 0.0
        normalizedDifference[0] = 1.0
        for (lag in 1..MAX_LAG + 1) {
            var difference = 0.0
            for (i in 0 until INTEGRATION_SAMPLES) {
                val delta = pcm[offset + i].toDouble() - pcm[offset + i + lag].toDouble()
                difference += delta * delta
            }
            cumulativeDifference += difference
            normalizedDifference[lag] = if (cumulativeDifference > 0.0) {
                difference * lag / cumulativeDifference
            } else {
                1.0
            }
        }
        for (lag in MIN_LAG..MAX_LAG) {
            val value = normalizedDifference[lag]
            if (value < PERIODICITY_THRESHOLD &&
                value <= normalizedDifference[lag - 1] &&
                value <= normalizedDifference[lag + 1]
            ) {
                return SAMPLE_RATE_HZ.toDouble() / lag
            }
        }
        return null
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val REQUIRED_SAMPLES = 1_001
        private const val INTEGRATION_SAMPLES = 800
        private const val MIN_LAG = 20 // 800 Hz
        private const val MAX_LAG = 200 // 80 Hz
        private const val MIN_RMS = 0.01 // -40 dBFS, after removal of DC energy
        private const val PERIODICITY_THRESHOLD = 0.15
    }
}
