package com.taki.petrobot.sensing

enum class PresenceState {
    ABSENT,
    POSSIBLE,
    PRESENT,
}

enum class PresenceEvidence {
    NONE,
    ACTIVITY,
    FACE,
}

/**
 * One timestamped set of scalar presence signals.
 *
 * The observation contains no raw camera or microphone data.
 */
data class PresenceObservation(
    val timestampNanos: Long,
    val visibleFaceCount: Int,
    val meanAbsoluteYDifference: Double?,
    val voiceCandidate: Boolean,
) {
    init {
        require(timestampNanos >= 0L)
        require(visibleFaceCount >= 0)
        require(
            meanAbsoluteYDifference == null ||
                meanAbsoluteYDifference in 0.0..1.0,
        )
    }
}

data class PresenceUpdate(
    val timestampNanos: Long,
    val previousState: PresenceState,
    val state: PresenceState,
    val evidence: PresenceEvidence,
) {
    val stateChanged: Boolean
        get() = previousState != state
}

/**
 * Pure, timestamp-driven presence state machine with asymmetric confirmation and release.
 *
 * A visible face is strong evidence and confirms presence immediately. Motion or VAD-lite is
 * weaker evidence and must remain active for [activityConfirmationNanos]. Once present, the state
 * is retained through short detector gaps for [absenceTimeoutNanos].
 */
class PresenceStateMachine(
    private val motionThreshold: Double = DEFAULT_MOTION_THRESHOLD,
    private val activityConfirmationNanos: Long = DEFAULT_ACTIVITY_CONFIRMATION_NANOS,
    private val candidateGapTimeoutNanos: Long = DEFAULT_CANDIDATE_GAP_TIMEOUT_NANOS,
    private val absenceTimeoutNanos: Long = DEFAULT_ABSENCE_TIMEOUT_NANOS,
) {
    var state: PresenceState = PresenceState.ABSENT
        private set

    private var lastTimestampNanos: Long? = null
    private var candidateStartedNanos: Long? = null
    private var lastPositiveEvidenceNanos: Long? = null

    init {
        require(motionThreshold in 0.0..1.0)
        require(activityConfirmationNanos > 0L)
        require(candidateGapTimeoutNanos > 0L)
        require(absenceTimeoutNanos > candidateGapTimeoutNanos)
    }

    fun update(observation: PresenceObservation): PresenceUpdate {
        val previousTimestamp = lastTimestampNanos
        require(previousTimestamp == null || observation.timestampNanos >= previousTimestamp) {
            "Presence observations must be timestamp ordered"
        }
        lastTimestampNanos = observation.timestampNanos

        val evidence = observation.evidence()
        val previousState = state
        when (evidence) {
            PresenceEvidence.FACE -> confirmPresence(observation.timestampNanos)
            PresenceEvidence.ACTIVITY -> observeActivity(observation.timestampNanos)
            PresenceEvidence.NONE -> observeNoEvidence(observation.timestampNanos)
        }

        return PresenceUpdate(
            timestampNanos = observation.timestampNanos,
            previousState = previousState,
            state = state,
            evidence = evidence,
        )
    }

    private fun PresenceObservation.evidence(): PresenceEvidence =
        when {
            visibleFaceCount > 0 -> PresenceEvidence.FACE
            voiceCandidate ||
                (meanAbsoluteYDifference != null &&
                    meanAbsoluteYDifference >= motionThreshold) -> PresenceEvidence.ACTIVITY
            else -> PresenceEvidence.NONE
        }

    private fun confirmPresence(timestampNanos: Long) {
        state = PresenceState.PRESENT
        candidateStartedNanos = null
        lastPositiveEvidenceNanos = timestampNanos
    }

    private fun observeActivity(timestampNanos: Long) {
        lastPositiveEvidenceNanos = timestampNanos
        when (state) {
            PresenceState.ABSENT -> {
                state = PresenceState.POSSIBLE
                candidateStartedNanos = timestampNanos
            }
            PresenceState.POSSIBLE -> {
                val candidateStarted = checkNotNull(candidateStartedNanos)
                if (timestampNanos - candidateStarted >= activityConfirmationNanos) {
                    state = PresenceState.PRESENT
                    candidateStartedNanos = null
                }
            }
            PresenceState.PRESENT -> Unit
        }
    }

    private fun observeNoEvidence(timestampNanos: Long) {
        val lastPositive = lastPositiveEvidenceNanos ?: return
        when (state) {
            PresenceState.ABSENT -> Unit
            PresenceState.POSSIBLE -> {
                if (timestampNanos - lastPositive >= candidateGapTimeoutNanos) {
                    state = PresenceState.ABSENT
                    candidateStartedNanos = null
                    lastPositiveEvidenceNanos = null
                }
            }
            PresenceState.PRESENT -> {
                if (timestampNanos - lastPositive >= absenceTimeoutNanos) {
                    state = PresenceState.ABSENT
                    lastPositiveEvidenceNanos = null
                }
            }
        }
    }

    companion object {
        const val DEFAULT_MOTION_THRESHOLD = 0.02
        const val DEFAULT_ACTIVITY_CONFIRMATION_NANOS = 750_000_000L
        const val DEFAULT_CANDIDATE_GAP_TIMEOUT_NANOS = 500_000_000L
        const val DEFAULT_ABSENCE_TIMEOUT_NANOS = 3_000_000_000L
    }
}
