package com.taki.petrobot

import com.taki.petrobot.sensing.PeriodicPitchAnalyzer
import com.taki.petrobot.sensing.SingingCandidateDetector

/** Opt-in diagnostic only. No callback capable of changing the pet or playing sound. */
class SingingProbe(
    private val playbackGuard: LocalAudioPlaybackGuard,
    private val report: (String) -> Unit,
) {
    private val analyzer = PeriodicPitchAnalyzer()
    private val detector = SingingCandidateDetector()
    private var active = false
    private var nextReportNanos = 0L
    private var previousTimestampNanos: Long? = null
    private var lastState: String? = null

    // Serialize lifecycle changes with the bounded capture-thread calculation. This prevents
    // old capture callbacks from producing a candidate after pause/reset has returned.
    @Synchronized
    fun setActive(value: Boolean) {
        if (active == value) return
        active = value
        detector.reset()
        previousTimestampNanos = null
        nextReportNanos = 0L
        lastState = null
        report(if (value) "probe=active mode=experimental_pitch" else "probe=paused")
    }

    @Synchronized
    fun acceptPcm(
        pcm: ShortArray,
        sampleCount: Int,
        timestampNanos: Long,
        interactive: Boolean,
    ) {
        if (!active) return
        require(timestampNanos >= 0L)
        require(sampleCount in 0..pcm.size)
        val previous = previousTimestampNanos
        if (previous != null && timestampNanos <= previous) {
            detector.reset()
            nextReportNanos = 0L
        }
        previousTimestampNanos = timestampNanos
        val windowStartNanos = (timestampNanos -
            sampleCount.toLong() * 1_000_000_000L / PeriodicPitchAnalyzer.SAMPLE_RATE_HZ)
            .coerceAtLeast(0L)
        val suppressed = !interactive || !playbackGuard.shouldProcessCapturedAudio(windowStartNanos)
        if (suppressed) {
            detector.reset()
            emit(timestampNanos, "suppressed", "candidate=false reason=" +
                if (!interactive) "screen_off" else "local_playback")
            return
        }
        if (sampleCount < PeriodicPitchAnalyzer.REQUIRED_SAMPLES) {
            detector.reset()
            emit(timestampNanos, "short_window", "candidate=false reason=short_window")
            return
        }
        val pitch = analyzer.estimateHz(pcm, sampleCount)
        // A local sound may have started on the UI thread while estimating pitch.
        if (!playbackGuard.shouldProcessCapturedAudio(windowStartNanos)) {
            detector.reset()
            emit(timestampNanos, "suppressed", "candidate=false reason=local_playback")
            return
        }
        val status = detector.accept(timestampNanos, pitch)
        val state = if (!status.ready) "warming" else if (status.candidate) "candidate" else "other"
        emit(timestampNanos, state,
            "candidate=${status.candidate} ready=${status.ready} " +
                "voicedWindows=${status.voicedWindows}/${status.observedWindows} " +
                "stablePairs=${status.stablePairs}")
    }

    private fun emit(timestampNanos: Long, state: String, detail: String) {
        if (state != lastState || timestampNanos >= nextReportNanos) {
            report("probe=$state $detail")
            lastState = state
            nextReportNanos = timestampNanos + 1_000_000_000L
        }
    }
}
