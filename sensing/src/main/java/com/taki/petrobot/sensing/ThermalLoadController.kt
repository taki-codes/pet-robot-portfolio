package com.taki.petrobot.sensing

enum class ThermalSeverity {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
}

enum class SensingLoadProfile(
    val cameraAnalysisStride: Int,
) {
    FULL(cameraAnalysisStride = 1),
    REDUCED(cameraAnalysisStride = 2),
    LOW(cameraAnalysisStride = 5),
    MINIMAL(cameraAnalysisStride = 15),
}

data class ThermalObservation(
    val timestampNanos: Long,
    val severity: ThermalSeverity,
) {
    init {
        require(timestampNanos >= 0L)
    }
}

data class ThermalLoadUpdate(
    val timestampNanos: Long,
    val severity: ThermalSeverity,
    val previousProfile: SensingLoadProfile,
    val profile: SensingLoadProfile,
) {
    val profileChanged: Boolean
        get() = previousProfile != profile
}

/**
 * Pure policy that maps Android thermal severity to a scalar sensing-load profile.
 *
 * Elevated profiles reduce how often the camera pipeline performs Y-plane comparison and face
 * detection. Returning to a cool status restores full-rate analysis.
 */
class ThermalLoadController {
    var profile: SensingLoadProfile = SensingLoadProfile.FULL
        private set

    private var lastTimestampNanos: Long? = null

    fun update(observation: ThermalObservation): ThermalLoadUpdate {
        val previousTimestamp = lastTimestampNanos
        require(previousTimestamp == null || observation.timestampNanos >= previousTimestamp) {
            "Thermal observations must be timestamp ordered"
        }
        lastTimestampNanos = observation.timestampNanos

        val previousProfile = profile
        profile = when (observation.severity) {
            ThermalSeverity.NONE,
            ThermalSeverity.LIGHT -> SensingLoadProfile.FULL

            ThermalSeverity.MODERATE -> SensingLoadProfile.REDUCED
            ThermalSeverity.SEVERE -> SensingLoadProfile.LOW
            ThermalSeverity.CRITICAL,
            ThermalSeverity.EMERGENCY,
            ThermalSeverity.SHUTDOWN -> SensingLoadProfile.MINIMAL
        }

        return ThermalLoadUpdate(
            timestampNanos = observation.timestampNanos,
            severity = observation.severity,
            previousProfile = previousProfile,
            profile = profile,
        )
    }
}
