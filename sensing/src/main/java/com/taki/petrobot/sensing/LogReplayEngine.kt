package com.taki.petrobot.sensing

data class ReplayThresholdProfile(
    val name: String,
    val motionThreshold: Double = PresenceStateMachine.DEFAULT_MOTION_THRESHOLD,
    val activityConfirmationNanos: Long =
        PresenceStateMachine.DEFAULT_ACTIVITY_CONFIRMATION_NANOS,
    val candidateGapTimeoutNanos: Long =
        PresenceStateMachine.DEFAULT_CANDIDATE_GAP_TIMEOUT_NANOS,
    val absenceTimeoutNanos: Long = PresenceStateMachine.DEFAULT_ABSENCE_TIMEOUT_NANOS,
    val quietPeriodNanos: Long,
) {
    init {
        require(name.isNotBlank())
        require(motionThreshold in 0.0..1.0)
        require(activityConfirmationNanos > 0L)
        require(candidateGapTimeoutNanos > 0L)
        require(absenceTimeoutNanos > candidateGapTimeoutNanos)
        require(quietPeriodNanos > 0L)
    }
}

data class ReplayScalarSample(
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

data class LogReplayResult(
    val profile: ReplayThresholdProfile,
    val sampleCount: Long,
    val sessionCount: Long,
    val presentTransitionCount: Long,
    val breakCandidateCount: Long,
)

/**
 * Replays timestamped scalar evidence through the existing pure state machines.
 *
 * A long gap starts a new session so an app stop or device restart cannot create a false quiet
 * candidate. No Android API, persistence API, raw camera frame, or audio sample is used here.
 */
class LogReplayEngine(
    profiles: List<ReplayThresholdProfile>,
    private val sessionGapNanos: Long = DEFAULT_SESSION_GAP_NANOS,
) {
    private val runtimes: List<ProfileRuntime>

    private var lastTimestampNanos: Long? = null

    init {
        require(profiles.isNotEmpty())
        require(profiles.map(ReplayThresholdProfile::name).distinct().size == profiles.size)
        require(sessionGapNanos > 0L)
        runtimes = profiles.map(::ProfileRuntime)
    }

    fun accept(sample: ReplayScalarSample) {
        val previousTimestamp = lastTimestampNanos
        require(previousTimestamp == null || sample.timestampNanos >= previousTimestamp) {
            "Replay samples must be timestamp ordered"
        }

        if (
            previousTimestamp == null ||
            sample.timestampNanos - previousTimestamp > sessionGapNanos
        ) {
            runtimes.forEach(ProfileRuntime::startSession)
        }
        lastTimestampNanos = sample.timestampNanos

        runtimes.forEach { runtime ->
            val presenceUpdate = runtime.presence.update(
                PresenceObservation(
                    timestampNanos = sample.timestampNanos,
                    visibleFaceCount = sample.visibleFaceCount,
                    meanAbsoluteYDifference = sample.meanAbsoluteYDifference,
                    voiceCandidate = sample.voiceCandidate,
                ),
            )
            if (presenceUpdate.stateChanged && presenceUpdate.state == PresenceState.PRESENT) {
                runtime.presentTransitionCount += 1L
            }

            val breakUpdate = runtime.breakCandidate.update(
                BreakCandidateObservation(
                    timestampNanos = sample.timestampNanos,
                    presenceState = presenceUpdate.state,
                    meanAbsoluteYDifference = sample.meanAbsoluteYDifference,
                    voiceCandidate = sample.voiceCandidate,
                ),
            )
            if (breakUpdate.becameCandidate) {
                runtime.breakCandidateCount += 1L
            }
            runtime.sampleCount += 1L
        }
    }

    fun results(): List<LogReplayResult> = runtimes.map { runtime ->
        LogReplayResult(
            profile = runtime.profile,
            sampleCount = runtime.sampleCount,
            sessionCount = runtime.sessionCount,
            presentTransitionCount = runtime.presentTransitionCount,
            breakCandidateCount = runtime.breakCandidateCount,
        )
    }

    private class ProfileRuntime(
        val profile: ReplayThresholdProfile,
    ) {
        var presence = newPresenceMachine()
        var breakCandidate = newBreakCandidateMachine()
        var sampleCount = 0L
        var sessionCount = 0L
        var presentTransitionCount = 0L
        var breakCandidateCount = 0L

        fun startSession() {
            presence = newPresenceMachine()
            breakCandidate = newBreakCandidateMachine()
            sessionCount += 1L
        }

        private fun newPresenceMachine() = PresenceStateMachine(
            motionThreshold = profile.motionThreshold,
            activityConfirmationNanos = profile.activityConfirmationNanos,
            candidateGapTimeoutNanos = profile.candidateGapTimeoutNanos,
            absenceTimeoutNanos = profile.absenceTimeoutNanos,
        )

        private fun newBreakCandidateMachine() = BreakCandidateStateMachine(
            motionThreshold = profile.motionThreshold,
            quietPeriodNanos = profile.quietPeriodNanos,
        )
    }

    companion object {
        const val DEFAULT_SESSION_GAP_NANOS = 10_000_000_000L
    }
}

fun defaultReplayProfiles(): List<ReplayThresholdProfile> = listOf(
    ReplayThresholdProfile(
        name = "CURRENT_1_5S",
        quietPeriodNanos = 1_500_000_000L,
    ),
    ReplayThresholdProfile(
        name = "QUIET_2_0S",
        quietPeriodNanos = 2_000_000_000L,
    ),
    ReplayThresholdProfile(
        name = "QUIET_2_5S",
        quietPeriodNanos = 2_500_000_000L,
    ),
    ReplayThresholdProfile(
        name = "QUIET_3_0S",
        quietPeriodNanos = 3_000_000_000L,
    ),
)
