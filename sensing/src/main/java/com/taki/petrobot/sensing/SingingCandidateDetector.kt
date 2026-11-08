package com.taki.petrobot.sensing

import kotlin.math.abs
import kotlin.math.ln

data class SingingCandidateStatus(
    val candidate: Boolean,
    val ready: Boolean,
    val voicedWindows: Int,
    val stablePairs: Int,
    val observedWindows: Int,
)

/**
 * Experimental sustained-pitch heuristic. Speech, instruments and TV can also pass.
 * Keeps at most 20 scalar pitch observations; never receives or retains PCM.
 * Confined to the caller's thread (or externally synchronized).
 */
class SingingCandidateDetector {
    private data class Observation(val timestampNanos: Long, val pitchHz: Double?)
    private val history = ArrayDeque<Observation>()

    fun accept(timestampNanos: Long, pitchHz: Double?): SingingCandidateStatus {
        require(timestampNanos >= 0L)
        require(pitchHz == null || (pitchHz.isFinite() && pitchHz in 80.0..800.0))
        val previous = history.lastOrNull()
        if (previous != null && timestampNanos - previous.timestampNanos !in 1L..MAX_GAP_NANOS) {
            reset()
        }
        history.addLast(Observation(timestampNanos, pitchHz))
        while (history.size > WINDOW_COUNT ||
            timestampNanos - history.first().timestampNanos > HISTORY_NANOS
        ) {
            history.removeFirst()
        }
        var voiced = 0
        var stable = 0
        var previousPitch: Double? = null
        for (observation in history) {
            val pitch = observation.pitchHz
            if (pitch != null) {
                voiced += 1
                if (previousPitch != null &&
                    abs(12.0 * ln(pitch / previousPitch) / ln(2.0)) <= MAX_STEP_SEMITONES
                ) {
                    stable += 1
                }
            }
            previousPitch = pitch
        }
        val ready = history.size == WINDOW_COUNT &&
            timestampNanos - history.first().timestampNanos >= MIN_OBSERVATION_NANOS
        return SingingCandidateStatus(
            candidate = ready && pitchHz != null && voiced >= 14 && stable >= 9,
            ready = ready,
            voicedWindows = voiced,
            stablePairs = stable,
            observedWindows = history.size,
        )
    }

    fun reset() = history.clear()

    companion object {
        private const val WINDOW_COUNT = 20
        private const val HISTORY_NANOS = 2_500_000_000L
        private const val MIN_OBSERVATION_NANOS = 1_800_000_000L
        private const val MAX_GAP_NANOS = 250_000_000L
        private const val MAX_STEP_SEMITONES = 1.5
    }
}
