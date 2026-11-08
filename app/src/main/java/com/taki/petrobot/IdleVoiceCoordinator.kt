package com.taki.petrobot

import kotlin.random.Random

enum class IdleVoice { PURR, SAD }
enum class IdleVoiceResult { NONE, PURR_PLAYED, SAD_PLAYED, UNAVAILABLE }

/** Foreground session purr and interaction-based sad voice use independent clocks. */
class IdleVoiceCoordinator(
    private val play: (IdleVoice) -> Boolean,
    private val playbackGuard: LocalAudioPlaybackGuard,
    private val choosePurrDelayNanos: () -> Long = {
        Random.nextLong(600L, 1_801L) * 1_000_000_000L
    },
) {
    private var sessionStartedNanos: Long? = null
    private var lastInteractionNanos = 0L
    private var purrDelayNanos = 0L
    private var purrConsumed = false
    private var sadConsumed = false
    private var purrOutcome = "pending"
    private var sadOutcome = "pending"
    private var blockingUntilNanos = Long.MIN_VALUE

    @Synchronized
    fun startSession(timestampNanos: Long): Long {
        val delay = choosePurrDelayNanos()
        require(delay in RANDOM_PURR_MIN_NANOS..RANDOM_PURR_MAX_NANOS)
        sessionStartedNanos = timestampNanos
        lastInteractionNanos = timestampNanos
        purrDelayNanos = delay
        purrConsumed = false
        sadConsumed = false
        purrOutcome = "pending"
        sadOutcome = "pending"
        return delay
    }

    @Synchronized
    fun stopSession() {
        sessionStartedNanos = null
        // Do not remove an active echo guard when resetting timers.
    }

    @Synchronized
    fun onInteraction(timestampNanos: Long) {
        if (sessionStartedNanos == null) return
        lastInteractionNanos = timestampNanos
        sadConsumed = false
        sadOutcome = "pending"
        // Touch/name interactions never reschedule or rearm the session purr.
    }

    /** Aggregate in-memory status only; never includes captured media or speech. */
    @Synchronized
    fun status(timestampNanos: Long): String {
        val start = sessionStartedNanos ?: return "session=inactive"
        val purrRemaining = ((purrDelayNanos - (timestampNanos - start)) / 1_000_000_000L).coerceAtLeast(0L)
        val sadRemaining = ((IDLE_SAD_AFTER_NANOS - (timestampNanos - lastInteractionNanos)) /
            1_000_000_000L).coerceAtLeast(0L)
        return "session=active purr=$purrOutcome purrRemainingSeconds=$purrRemaining " +
            "sad=$sadOutcome sadRemainingSeconds=$sadRemaining"
    }

    @Synchronized
    fun isBlockingOtherSounds(timestampNanos: Long): Boolean =
        timestampNanos < blockingUntilNanos

    @Synchronized
    fun tick(timestampNanos: Long, sadEligible: Boolean): IdleVoiceResult {
        val start = sessionStartedNanos ?: return IdleVoiceResult.NONE
        if (timestampNanos < start || timestampNanos < lastInteractionNanos) {
            startSession(timestampNanos)
            return IdleVoiceResult.NONE
        }
        val voice = when {
            !purrConsumed && timestampNanos - start >= purrDelayNanos -> IdleVoice.PURR
            !sadConsumed && sadEligible &&
                timestampNanos - lastInteractionNanos >= IDLE_SAD_AFTER_NANOS -> IdleVoice.SAD
            else -> return IdleVoiceResult.NONE
        }
        if (!playbackGuard.shouldProcessCapturedAudio(timestampNanos)) return IdleVoiceResult.NONE
        if (voice == IdleVoice.PURR) purrConsumed = true else sadConsumed = true
        val played = play(voice)
        val outcome = if (played) "played" else "unavailable"
        if (voice == IdleVoice.PURR) purrOutcome = outcome else sadOutcome = outcome
        if (!played) return IdleVoiceResult.UNAVAILABLE
        val guard = if (voice == IdleVoice.PURR) IDLE_PURR_GUARD_NANOS else IDLE_SAD_GUARD_NANOS
        blockingUntilNanos = timestampNanos + guard
        playbackGuard.suppressFor(timestampNanos, guard)
        return if (voice == IdleVoice.PURR) IdleVoiceResult.PURR_PLAYED else IdleVoiceResult.SAD_PLAYED
    }
}

const val RANDOM_PURR_MIN_NANOS = 600_000_000_000L
const val RANDOM_PURR_MAX_NANOS = 1_800_000_000_000L
const val IDLE_SAD_AFTER_NANOS = 3_600_000_000_000L
const val IDLE_PURR_GUARD_NANOS = 1_700_000_000L
const val IDLE_SAD_GUARD_NANOS = 8_500_000_000L
