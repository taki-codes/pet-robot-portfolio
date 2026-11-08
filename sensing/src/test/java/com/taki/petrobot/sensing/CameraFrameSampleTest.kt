package com.taki.petrobot.sensing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CameraFrameSampleTest {
    @Test
    fun preservesFrameMetadataAndScalarResults() {
        val sample = validSample(visibleFaceCount = 2)

        assertEquals(123L, sample.timestampNanos)
        assertEquals(640, sample.width)
        assertEquals(480, sample.height)
        assertEquals(90, sample.rotationDegrees)
        assertEquals(4_800, sample.sampledYPixelCount)
        assertEquals(0.125, sample.meanAbsoluteYDifference!!, 0.0)
        assertEquals(2, sample.visibleFaceCount)
    }

    @Test
    fun rejectsInvalidDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            validSample(width = 0)
        }
    }

    @Test
    fun rejectsOutOfRangeDifference() {
        assertThrows(IllegalArgumentException::class.java) {
            validSample(meanAbsoluteYDifference = 1.01)
        }
    }

    @Test
    fun rejectsNegativeVisibleFaceCount() {
        assertThrows(IllegalArgumentException::class.java) {
            validSample(visibleFaceCount = -1)
        }
    }

    private fun validSample(
        width: Int = 640,
        meanAbsoluteYDifference: Double? = 0.125,
        visibleFaceCount: Int = 0,
    ) = CameraFrameSample(
        timestampNanos = 123L,
        width = width,
        height = 480,
        rotationDegrees = 90,
        yRowStride = 640,
        yPixelStride = 1,
        yBufferBytes = 307_200,
        sampledYPixelCount = 4_800,
        meanAbsoluteYDifference = meanAbsoluteYDifference,
        visibleFaceCount = visibleFaceCount,
    )
}
