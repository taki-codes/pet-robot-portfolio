package com.taki.petrobot.sensing

import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YPlaneFrameDifferTest {
    @Test
    fun firstFrameCreatesBaselineAndIdenticalFrameHasZeroDifference() {
        val differ = YPlaneFrameDiffer(sampleStepPixels = 1)
        val frame = ByteBuffer.wrap(byteArrayOf(10, 20, 30, 40))

        val baseline = differ.observe(
            timestampNanos = 1L,
            width = 2,
            height = 2,
            rowStride = 2,
            pixelStride = 1,
            buffer = frame,
        )
        val identical = differ.observe(
            timestampNanos = 2L,
            width = 2,
            height = 2,
            rowStride = 2,
            pixelStride = 1,
            buffer = frame,
        )

        assertNull(baseline.meanAbsoluteDifference)
        assertEquals(4, baseline.sampledPixelCount)
        assertEquals(0.0, identical.meanAbsoluteDifference!!, 0.0)
    }

    @Test
    fun maximumLuminanceChangeHasOneDifference() {
        val differ = YPlaneFrameDiffer(sampleStepPixels = 1)

        differ.observe(
            timestampNanos = 1L,
            width = 2,
            height = 2,
            rowStride = 2,
            pixelStride = 1,
            buffer = ByteBuffer.wrap(ByteArray(4)),
        )
        val changed = differ.observe(
            timestampNanos = 2L,
            width = 2,
            height = 2,
            rowStride = 2,
            pixelStride = 1,
            buffer = ByteBuffer.wrap(ByteArray(4) { 255.toByte() }),
        )

        assertEquals(1.0, changed.meanAbsoluteDifference!!, 0.0)
    }

    @Test
    fun respectsRowAndPixelStrides() {
        val differ = YPlaneFrameDiffer(sampleStepPixels = 1)
        val first = ByteBuffer.wrap(
            byteArrayOf(
                10, 0, 20, 0, 99, 99,
                30, 0, 40, 0, 99, 99,
            )
        )
        val second = ByteBuffer.wrap(
            byteArrayOf(
                10, 0, 20, 0, 99, 99,
                30, 0, 50, 0, 99, 99,
            )
        )

        differ.observe(
            timestampNanos = 1L,
            width = 2,
            height = 2,
            rowStride = 6,
            pixelStride = 2,
            buffer = first,
        )
        val changed = differ.observe(
            timestampNanos = 2L,
            width = 2,
            height = 2,
            rowStride = 6,
            pixelStride = 2,
            buffer = second,
        )

        assertEquals(10.0 / (4 * 255), changed.meanAbsoluteDifference!!, 0.000_000_1)
    }

    @Test
    fun resetAndDimensionChangeCreateNewBaselines() {
        val differ = YPlaneFrameDiffer(sampleStepPixels = 1)
        differ.observe(
            timestampNanos = 1L,
            width = 2,
            height = 2,
            rowStride = 2,
            pixelStride = 1,
            buffer = ByteBuffer.wrap(ByteArray(4)),
        )

        val resized = differ.observe(
            timestampNanos = 2L,
            width = 1,
            height = 2,
            rowStride = 1,
            pixelStride = 1,
            buffer = ByteBuffer.wrap(ByteArray(2)),
        )
        differ.reset()
        val reset = differ.observe(
            timestampNanos = 3L,
            width = 1,
            height = 2,
            rowStride = 1,
            pixelStride = 1,
            buffer = ByteBuffer.wrap(ByteArray(2)),
        )

        assertNull(resized.meanAbsoluteDifference)
        assertNull(reset.meanAbsoluteDifference)
    }
}
