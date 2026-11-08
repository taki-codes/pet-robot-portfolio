package com.taki.petrobot.sensing

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import kotlin.concurrent.thread

/**
 * Reads short microphone windows and immediately reduces them to scalar measurements.
 *
 * The PCM buffer is reused inside the capture thread and is never exposed or persisted.
 * Callers must hold RECORD_AUDIO before calling [start].
 */
class AudioLevelSource(
    private val onSample: (AudioEnvelopeSample) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onPcmWindow: (ShortArray, Int) -> Unit = { _, _ -> },
    private val analyzer: AudioEnvelopeAnalyzer = AudioEnvelopeAnalyzer(),
) : AutoCloseable {
    @Volatile
    private var running = false
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start() {
        if (running) return

        try {
            val minimumBufferBytes = AudioRecord.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            check(minimumBufferBytes > 0) {
                "AudioRecord returned invalid minimum buffer size: $minimumBufferBytes"
            }

            val record = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minimumBufferBytes, WINDOW_SAMPLES * BYTES_PER_SAMPLE))
                .build()

            check(record.state == AudioRecord.STATE_INITIALIZED) {
                record.release()
                "AudioRecord failed to initialize"
            }

            audioRecord = record
            running = true
            record.startRecording()
            captureThread = thread(
                start = true,
                name = "PetRobotAudioCapture",
            ) {
                capture(record)
            }
        } catch (error: Throwable) {
            running = false
            audioRecord?.release()
            audioRecord = null
            captureThread = null
            onError(error)
        }
    }

    private fun capture(record: AudioRecord) {
        val pcm = ShortArray(WINDOW_SAMPLES)
        try {
            while (running) {
                val samplesRead = record.read(
                    pcm,
                    0,
                    pcm.size,
                    AudioRecord.READ_BLOCKING,
                )
                when {
                    samplesRead > 0 -> {
                        // The callback is synchronous. The reusable PCM array must not be retained.
                        onPcmWindow(pcm, samplesRead)
                        onSample(
                            analyzer.analyze(
                                timestampNanos = SystemClock.elapsedRealtimeNanos(),
                                pcm = pcm,
                                sampleCount = samplesRead,
                            ),
                        )
                    }
                    samplesRead < 0 -> error("AudioRecord read failed: $samplesRead")
                }
            }
        } catch (error: Throwable) {
            if (running) {
                onError(error)
            }
        }
    }

    override fun close() {
        val record: AudioRecord
        val worker: Thread?
        synchronized(this) {
            running = false
            record = audioRecord ?: return
            audioRecord = null
            worker = captureThread
            captureThread = null
        }

        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        }
        worker?.join(CAPTURE_THREAD_JOIN_MILLIS)
        record.release()
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val WINDOW_SAMPLES = 1_600

        private const val BYTES_PER_SAMPLE = 2
        private const val CAPTURE_THREAD_JOIN_MILLIS = 1_000L
    }
}
