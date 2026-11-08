package com.taki.petrobot

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.taki.petrobot.sensing.AudioLevelSource
import com.taki.petrobot.sensing.containsMiKeyword
import java.io.OutputStream

/**
 * Streams the existing in-memory PCM to Android's on-device Japanese recognizer.
 * Hypotheses are reduced to a keyword boolean and are never logged, persisted, or exposed.
 */
class OnDeviceMiKeywordRecognizer(
    private val context: Context,
    private val onKeywordDetected: () -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (Int) -> Unit,
) : AutoCloseable {
    private var speechRecognizer: SpeechRecognizer? = null
    private var readDescriptor: ParcelFileDescriptor? = null
    @Volatile private var pcmOutput: OutputStream? = null
    private var keywordSeenInUtterance = false
    @Volatile private var acceptingPcm = false

    fun start() {
        if (speechRecognizer != null || acceptingPcm) return
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            onStatus(STATUS_UNAVAILABLE)
            return
        }

        val pipe = ParcelFileDescriptor.createPipe()
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        readDescriptor = pipe[0]
        pcmOutput = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        speechRecognizer = recognizer
        acceptingPcm = true

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onStatus(STATUS_READY)
            }

            override fun onBeginningOfSpeech() {
                keywordSeenInUtterance = false
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                stopPcmInput()
                onError(error)
            }

            override fun onResults(results: Bundle?) {
                inspect(results)
                keywordSeenInUtterance = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                inspect(partialResults)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onSegmentResults(segmentResults: Bundle) {
                inspect(segmentResults)
                keywordSeenInUtterance = false
            }

            override fun onEndOfSegmentedSession() {
                stopPcmInput()
                onStatus(STATUS_ENDED)
            }

            private fun inspect(results: Bundle?) {
                if (keywordSeenInUtterance) return
                val matched = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                    .any(::containsMiKeyword)
                if (matched) {
                    keywordSeenInUtterance = true
                    onKeywordDetected()
                }
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, JAPANESE_LANGUAGE_TAG)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            .putStringArrayListExtra(
                RecognizerIntent.EXTRA_BIASING_STRINGS,
                arrayListOf(MI_KEYWORD, MI_SHORT_KANA, MI_SHORT_LONG_KANA, MIINYA_KEYWORD),
            )
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, CHANNEL_COUNT)
            .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            .putExtra(
                RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE,
                AudioLevelSource.SAMPLE_RATE_HZ,
            )
            .putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
        recognizer.startListening(intent)
    }

    fun acceptPcm(samples: ShortArray, sampleCount: Int) {
        if (!acceptingPcm) return
        val output = pcmOutput ?: return
        val boundedCount = sampleCount.coerceIn(0, samples.size)
        val bytes = ByteArray(boundedCount * BYTES_PER_SAMPLE)
        var byteIndex = 0
        repeat(boundedCount) { sampleIndex ->
            val sample = samples[sampleIndex].toInt()
            bytes[byteIndex++] = sample.toByte()
            bytes[byteIndex++] = (sample ushr 8).toByte()
        }
        runCatching { output.write(bytes) }
            .onFailure { stopPcmInput() }
    }

    private fun stopPcmInput() {
        acceptingPcm = false
        val output = pcmOutput
        pcmOutput = null
        runCatching { output?.close() }
    }

    override fun close() {
        stopPcmInput()
        runCatching { readDescriptor?.close() }
        readDescriptor = null
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        keywordSeenInUtterance = false
    }

    private companion object {
        const val MI_KEYWORD = "ミー"
        const val MI_SHORT_KANA = "ミ"
        const val MI_SHORT_LONG_KANA = "ミイ"
        const val MIINYA_KEYWORD = "ミーニャ"
        const val JAPANESE_LANGUAGE_TAG = "ja-JP"
        const val MAX_RESULTS = 5
        const val CHANNEL_COUNT = 1
        const val BYTES_PER_SAMPLE = 2
        const val STATUS_UNAVAILABLE = "unavailable"
        const val STATUS_READY = "ready"
        const val STATUS_ENDED = "ended"
    }
}
