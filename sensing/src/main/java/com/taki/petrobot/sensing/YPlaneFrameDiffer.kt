package com.taki.petrobot.sensing

import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Computes a normalized mean absolute difference from sparse Y-plane samples.
 *
 * Only the sampled luminance values from the previous frame are kept in memory. Full frames are
 * neither copied nor persisted. The first observation after creation, reset, or a size change is
 * a baseline and therefore has no difference value.
 */
class YPlaneFrameDiffer(
    private val sampleStepPixels: Int = DEFAULT_SAMPLE_STEP_PIXELS,
) {
    private var previousSamples: IntArray? = null
    private var previousWidth = 0
    private var previousHeight = 0

    init {
        require(sampleStepPixels > 0)
    }

    fun observe(
        timestampNanos: Long,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        buffer: ByteBuffer,
    ): YPlaneDifference {
        require(timestampNanos >= 0L)
        require(width > 0)
        require(height > 0)
        require(rowStride > 0)
        require(pixelStride > 0)

        val samples = sample(
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = pixelStride,
            buffer = buffer,
        )
        val previous = previousSamples
        val difference = if (
            previous != null &&
            previousWidth == width &&
            previousHeight == height &&
            previous.size == samples.size
        ) {
            var totalDifference = 0L
            for (index in samples.indices) {
                totalDifference += abs(samples[index] - previous[index]).toLong()
            }
            totalDifference / (samples.size * MAX_LUMA.toDouble())
        } else {
            null
        }

        previousSamples = samples
        previousWidth = width
        previousHeight = height

        return YPlaneDifference(
            timestampNanos = timestampNanos,
            sampledPixelCount = samples.size,
            meanAbsoluteDifference = difference,
        )
    }

    fun reset() {
        previousSamples = null
        previousWidth = 0
        previousHeight = 0
    }

    private fun sample(
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        buffer: ByteBuffer,
    ): IntArray {
        val columns = (width + sampleStepPixels - 1) / sampleStepPixels
        val rows = (height + sampleStepPixels - 1) / sampleStepPixels
        val samples = IntArray(columns * rows)
        val bufferStart = buffer.position()
        val bufferEnd = buffer.limit()
        var sampleIndex = 0

        for (y in 0 until height step sampleStepPixels) {
            for (x in 0 until width step sampleStepPixels) {
                val bufferIndex = bufferStart + y * rowStride + x * pixelStride
                require(bufferIndex in bufferStart until bufferEnd) {
                    "Y-plane buffer is smaller than its dimensions and strides require"
                }
                samples[sampleIndex++] = buffer.get(bufferIndex).toInt() and MAX_LUMA
            }
        }

        return samples
    }

    private companion object {
        const val DEFAULT_SAMPLE_STEP_PIXELS = 8
        const val MAX_LUMA = 255
    }
}

data class YPlaneDifference(
    val timestampNanos: Long,
    val sampledPixelCount: Int,
    val meanAbsoluteDifference: Double?,
) {
    init {
        require(timestampNanos >= 0L)
        require(sampledPixelCount > 0)
        require(meanAbsoluteDifference == null ||
            meanAbsoluteDifference in 0.0..1.0)
    }
}
