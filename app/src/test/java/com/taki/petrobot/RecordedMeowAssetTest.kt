package com.taki.petrobot

import java.io.File
import kotlin.math.ceil
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordedMeowAssetTest {
    @Test
    fun strokeAffectionateSoundFitsGuardAtSlowestRate() {
        AudioSystem.getAudioInputStream(File("src/main/res/raw/miina_affection.wav")).use { audio ->
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, audio.format.encoding)
            assertEquals(16, audio.format.sampleSizeInBits)
            assertEquals(2, audio.format.channels)
            assertEquals(44_100f, audio.format.frameRate, 0f)
            assertTrue(audio.frameLength > 0L)
            val duration = ceil(
                audio.frameLength.toDouble() / audio.format.frameRate /
                    CAT_VOICE_MIN_RATE * 1_000_000_000,
            ).toLong()
            assertTrue(duration + 300_000_000L <= STROKE_PURR_ECHO_GUARD_NANOS)
        }
    }

    @Test
    fun keywordAndTapUseTheSameSelectedRecording() {
        assertArrayEquals(
            File("src/main/res/raw/miina_purr.wav").readBytes(),
            File("src/main/res/raw/miina_meow.wav").readBytes(),
        )
    }

    @Test
    fun slowestRecordingFitsKeywordArrivalAndTouchGuardsWithTailMargin() {
        AudioSystem.getAudioInputStream(
            File("src/main/res/raw/miina_meow.wav"),
        ).use { audio ->
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, audio.format.encoding)
            assertEquals(16, audio.format.sampleSizeInBits)
            assertEquals(2, audio.format.channels)
            assertEquals(44_100f, audio.format.frameRate, 0f)
            assertTrue(audio.frameLength > 0L)
            val durationNanos = ceil(
                audio.frameLength.toDouble() / audio.format.frameRate /
                    CAT_VOICE_MIN_RATE * 1_000_000_000,
            ).toLong()
            assertTrue(
                "Recording must leave at least 300 ms for the playback tail",
                durationNanos + 300_000_000L <= LOCAL_MEOW_ECHO_GUARD_NANOS,
            )
            assertTrue(
                "Slowest tap sound must also leave at least 300 ms",
                durationNanos + 300_000_000L <= TOUCH_PURR_ECHO_GUARD_NANOS,
            )
        }
    }
}
