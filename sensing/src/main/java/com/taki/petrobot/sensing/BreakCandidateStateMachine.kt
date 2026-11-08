package com.taki.petrobot.sensing

enum class BreakCandidateState {
    IDLE,
    ACTIVE,
    CANDIDATE,
}

/**
 * Timestamped scalar inputs used to locate a possible interaction boundary.
 *
 * A candidate is only meaningful while presence is confirmed. No raw camera or microphone data is
 * included.
 */
data class BreakCandidateObservation(
    val timestampNanos: Long,
    val presenceState: PresenceState,
    val meanAbsoluteYDifference: Double?,
    val voiceCandidate: Boolean,
) {
    init {
        require(timestampNanos >= 0L)
        require(
            meanAbsoluteYDifference == null ||
                meanAbsoluteYDifference in 0.0..1.0,
        )
    }
}

data class BreakCandidateUpdate(
    val timestampNanos: Long,
    val previousState: BreakCandidateState,
    val state: BreakCandidateState,
    val activityDetected: Boolean,
) {
    val stateChanged: Boolean
        get() = previousState != state

    val becameCandidate: Boolean
        get() = stateChanged && state == BreakCandidateState.CANDIDATE
}

/**
 * Pure state machine that emits a candidate after activity ends while presence remains confirmed.
 *
 * Voice or motion arms [BreakCandidateState.ACTIVE]. A continuous quiet period of
 * [quietPeriodNanos] then produces one [BreakCandidateState.CANDIDATE]. New activity rearms the
 * machine, and loss of confirmed presence resets it.
 */
class BreakCandidateStateMachine(
    private val motionThreshold: Double = DEFAULT_MOTION_THRESHOLD,
    private val quietPeriodNanos: Long = DEFAULT_QUIET_PERIOD_NANOS,
) {
    var state: BreakCandidateState = BreakCandidateState.IDLE
        private set

    private var lastTimestampNanos: Long? = null
    private var lastActivityNanos: Long? = null

    init {
        require(motionThreshold in 0.0..1.0)
        require(quietPeriodNanos > 0L)
    }

    fun update(observation: BreakCandidateObservation): BreakCandidateUpdate {
        val previousTimestamp = lastTimestampNanos
        require(previousTimestamp == null || observation.timestampNanos >= previousTimestamp) {
            "Break-candidate observations must be timestamp ordered"
        }
        lastTimestampNanos = observation.timestampNanos

        val previousState = state
        val activityDetected = observation.voiceCandidate ||
            (observation.meanAbsoluteYDifference != null &&
                observation.meanAbsoluteYDifference >= motionThreshold)

        if (observation.presenceState != PresenceState.PRESENT) {
            reset()
        } else if (activityDetected) {
            state = BreakCandidateState.ACTIVE
            lastActivityNanos = observation.timestampNanos
        } else if (state == BreakCandidateState.ACTIVE) {
            val lastActivity = checkNotNull(lastActivityNanos)
            if (observation.timestampNanos - lastActivity >= quietPeriodNanos) {
                state = BreakCandidateState.CANDIDATE
            }
        }

        return BreakCandidateUpdate(
            timestampNanos = observation.timestampNanos,
            previousState = previousState,
            state = state,
            activityDetected = activityDetected,
        )
    }

    private fun reset() {
        state = BreakCandidateState.IDLE
        lastActivityNanos = null
    }

    companion object {
        const val DEFAULT_MOTION_THRESHOLD = 0.02
        const val DEFAULT_QUIET_PERIOD_NANOS = 1_500_000_000L
    }
}
