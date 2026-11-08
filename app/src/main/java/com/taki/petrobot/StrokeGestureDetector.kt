package com.taki.petrobot

import kotlin.math.hypot

/** One drag only. Positions are dp, timestamps are monotonic pointer-event milliseconds. */
class StrokeGestureDetector(
    private val durationMillis: Long = 2_000L,
    private val maxMotionGapMillis: Long = 350L,
    private val movementStepDp: Float = 4f,
) {
    private var anchorX = 0f
    private var anchorY = 0f
    private var lastMotionMillis: Long? = null
    private var movingMillis = 0L
    private var triggered = false

    fun sample(xDp: Float, yDp: Float, timestampMillis: Long): Boolean {
        if (triggered) return false
        val previous = lastMotionMillis
        if (previous == null || timestampMillis < previous) {
            anchorX = xDp
            anchorY = yDp
            lastMotionMillis = timestampMillis
            movingMillis = 0L
            return false
        }
        val gap = timestampMillis - previous
        if (gap > maxMotionGapMillis) movingMillis = 0L
        if (hypot(xDp - anchorX, yDp - anchorY) < movementStepDp) return false

        anchorX = xDp
        anchorY = yDp
        lastMotionMillis = timestampMillis
        if (gap <= maxMotionGapMillis) movingMillis += gap
        if (movingMillis < durationMillis) return false
        triggered = true
        return true
    }
}
