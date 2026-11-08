package com.taki.petrobot.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEnvelopeAnalyzerTest {
    @Test
    fun silenceUsesFloorAndIsNotVoiceCandidate() {
        val result = AudioEnvelopeAnalyzer().analyze(
            timestampNanos = 10L,
            pcm = ShortArray(160),
        )

        assertEquals(160, result.sampleCount)
        assertEquals(0.0, result.rms, 0.0)
        assertEquals(AudioEnvelopeAnalyzer.MIN_DBFS, result.dbfs, 0.0)
        assertEquals(0.0, result.peak, 0.0)
        assertFalse(result.voiceCandidate)
    }

    @Test
    fun constantHalfScaleSignalProducesExpectedLevel() {
        val result = AudioEnvelopeAnalyzer().analyze(
            timestampNanos = 20L,
            pcm = ShortArray(160) { 16_384 },
        )

        assertEquals(0.5, result.rms, 1e-9)
        assertEquals(-6.0206, result.dbfs, 1e-4)
        assertEquals(0.5, result.peak, 1e-9)
        assertTrue(result.voiceCandidate)
    }

    @Test
    fun thresholdSeparatesQuietAndLoudWindows() {
        val analyzer = AudioEnvelopeAnalyzer(voiceThresholdDbfs = -42.0)

        val quiet = analyzer.analyze(1L, ShortArray(10) { 200 })
        val loud = analyzer.analyze(2L, ShortArray(10) { 400 })

        assertFalse(quiet.voiceCandidate)
        assertTrue(loud.voiceCandidate)
    }

    @Test
    fun analysisUsesOnlyValidSamples() {
        val result = AudioEnvelopeAnalyzer().analyze(
            timestampNanos = 30L,
            pcm = shortArrayOf(0, 0, Short.MAX_VALUE),
            sampleCount = 2,
        )

        assertEquals(2, result.sampleCount)
        assertEquals(0.0, result.rms, 0.0)
        assertEquals(0.0, result.peak, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyWindowIsRejected() {
        AudioEnvelopeAnalyzer().analyze(
            timestampNanos = 40L,
            pcm = ShortArray(0),
        )
    }
}
