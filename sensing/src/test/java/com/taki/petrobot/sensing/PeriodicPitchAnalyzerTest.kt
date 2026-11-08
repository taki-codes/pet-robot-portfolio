package com.taki.petrobot.sensing

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodicPitchAnalyzerTest {
    private val analyzer = PeriodicPitchAnalyzer()

    @Test fun estimatesSustainedTonesAcrossVoiceRange() {
        for (hz in listOf(80.0, 110.0, 220.0, 440.0, 800.0)) {
            val pitch = analyzer.estimateHz(tone(hz))
            assertNotNull("frequency=$hz", pitch)
            assertEquals(hz, pitch!!, hz * 0.03)
        }
    }

    @Test fun silenceConstantOffsetAndQuietToneDoNotProducePitch() {
        assertNull(analyzer.estimateHz(ShortArray(1600)))
        assertNull(analyzer.estimateHz(ShortArray(1600) { 8000 }))
        assertNull(analyzer.estimateHz(tone(220.0, amplitude = 100.0)))
    }

    @Test fun seededBroadbandNoiseDoesNotProducePitch() {
        val random = Random(36)
        repeat(20) {
            assertNull(analyzer.estimateHz(ShortArray(1600) { random.nextInt(-8000, 8001).toShort() }))
        }
    }

    @Test fun findsFundamentalWithHarmonicsAndDcOffset() {
        val pcm = ShortArray(1600) { i ->
            val phase = 2 * PI * 160 * i / 16000
            (2000 + 4000 * sin(phase) + 3000 * sin(2 * phase) + 2000 * sin(3 * phase)).toInt().toShort()
        }
        assertEquals(160.0, analyzer.estimateHz(pcm)!!, 5.0)
    }

    @Test fun respectsValidSampleCountAndDoesNotReusePriorPcm() {
        val pcm = tone(220.0)
        assertNotNull(analyzer.estimateHz(pcm))
        pcm.fill(0, 0, 1100)
        assertNull(analyzer.estimateHz(pcm, 1100)) // tail still contains a tone
        assertNull(analyzer.estimateHz(pcm, 500))
        assertNull(analyzer.estimateHz(pcm, 0))
    }

    private fun tone(hz: Double, amplitude: Double = 8000.0) = ShortArray(1600) { i ->
        (amplitude * sin(2 * PI * hz * i / 16000)).toInt().toShort()
    }
}
