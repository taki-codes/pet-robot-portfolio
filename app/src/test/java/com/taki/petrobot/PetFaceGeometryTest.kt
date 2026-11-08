package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Test

class PetFaceGeometryTest {
    @Test
    fun centerTapProducesNeutralGaze() {
        val target = normalizedGazeTarget(
            tapX = 500f,
            tapY = 300f,
            width = 1_000f,
            height = 600f,
        )

        assertEquals(0f, target.horizontal, 0.0001f)
        assertEquals(0f, target.vertical, 0.0001f)
    }

    @Test
    fun cornerTapProducesFullGaze() {
        val target = normalizedGazeTarget(
            tapX = 1_000f,
            tapY = 600f,
            width = 1_000f,
            height = 600f,
        )

        assertEquals(1f, target.horizontal, 0.0001f)
        assertEquals(1f, target.vertical, 0.0001f)
    }

    @Test
    fun outOfBoundsTapIsClamped() {
        val target = normalizedGazeTarget(
            tapX = -250f,
            tapY = 900f,
            width = 1_000f,
            height = 600f,
        )

        assertEquals(-1f, target.horizontal, 0.0001f)
        assertEquals(1f, target.vertical, 0.0001f)
    }

    @Test
    fun invalidCanvasSizeProducesNeutralGaze() {
        val target = normalizedGazeTarget(
            tapX = 10f,
            tapY = 10f,
            width = 0f,
            height = 0f,
        )

        assertEquals(GazeTarget(0f, 0f), target)
    }
}
