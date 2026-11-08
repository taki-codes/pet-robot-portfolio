package com.taki.petrobot.sensing

/**
 * Non-owning metadata and scalar sensing results for one acquired YUV frame.
 *
 * No camera pixels, face features, or identifiers leave the analyzer, and no raw media is
 * persisted.
 */
data class CameraFrameSample(
    val timestampNanos: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val yRowStride: Int,
    val yPixelStride: Int,
    val yBufferBytes: Int,
    val sampledYPixelCount: Int,
    val meanAbsoluteYDifference: Double?,
    val visibleFaceCount: Int,
) {
    init {
        require(timestampNanos >= 0L)
        require(width > 0)
        require(height > 0)
        require(rotationDegrees == 0 || rotationDegrees == 90 ||
            rotationDegrees == 180 || rotationDegrees == 270)
        require(yRowStride > 0)
        require(yPixelStride > 0)
        require(yBufferBytes > 0)
        require(sampledYPixelCount > 0)
        require(meanAbsoluteYDifference == null ||
            meanAbsoluteYDifference in 0.0..1.0)
        require(visibleFaceCount >= 0)
    }
}
