package com.taki.petrobot.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalLoadControllerTest {
    @Test
    fun everyThermalSeverityMapsToExpectedProfile() {
        val expectedProfiles = listOf(
            ThermalSeverity.NONE to SensingLoadProfile.FULL,
            ThermalSeverity.LIGHT to SensingLoadProfile.FULL,
            ThermalSeverity.MODERATE to SensingLoadProfile.REDUCED,
            ThermalSeverity.SEVERE to SensingLoadProfile.LOW,
            ThermalSeverity.CRITICAL to SensingLoadProfile.MINIMAL,
            ThermalSeverity.EMERGENCY to SensingLoadProfile.MINIMAL,
            ThermalSeverity.SHUTDOWN to SensingLoadProfile.MINIMAL,
        )

        expectedProfiles.forEachIndexed { index, (severity, expectedProfile) ->
            val update = ThermalLoadController().update(
                ThermalObservation(
                    timestampNanos = index.toLong(),
                    severity = severity,
                ),
            )

            assertEquals(expectedProfile, update.profile)
        }
    }

    @Test
    fun elevatedStatusReducesLoadAndCoolStatusRestoresFullRate() {
        val controller = ThermalLoadController()

        val elevated = controller.update(
            ThermalObservation(1L, ThermalSeverity.SEVERE),
        )
        val recovered = controller.update(
            ThermalObservation(2L, ThermalSeverity.NONE),
        )

        assertEquals(SensingLoadProfile.LOW, elevated.profile)
        assertTrue(elevated.profileChanged)
        assertEquals(SensingLoadProfile.FULL, recovered.profile)
        assertTrue(recovered.profileChanged)
    }

    @Test
    fun equivalentStatusesDoNotReportProfileChange() {
        val controller = ThermalLoadController()
        controller.update(ThermalObservation(1L, ThermalSeverity.NONE))

        val update = controller.update(
            ThermalObservation(2L, ThermalSeverity.LIGHT),
        )

        assertEquals(SensingLoadProfile.FULL, update.profile)
        assertFalse(update.profileChanged)
    }

    @Test
    fun analysisStridesIncreaseWithThermalLoad() {
        assertEquals(1, SensingLoadProfile.FULL.cameraAnalysisStride)
        assertEquals(2, SensingLoadProfile.REDUCED.cameraAnalysisStride)
        assertEquals(5, SensingLoadProfile.LOW.cameraAnalysisStride)
        assertEquals(15, SensingLoadProfile.MINIMAL.cameraAnalysisStride)
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfOrderObservationIsRejected() {
        val controller = ThermalLoadController()
        controller.update(ThermalObservation(2L, ThermalSeverity.NONE))
        controller.update(ThermalObservation(1L, ThermalSeverity.MODERATE))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeTimestampIsRejected() {
        ThermalObservation(-1L, ThermalSeverity.NONE)
    }
}
