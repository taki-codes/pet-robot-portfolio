package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFaceTouchReactionTest {
    @Test
    fun settledReactionUsesNeutralVisuals() {
        assertEquals(
            PetFaceTouchVisual(
                eyeScaleMultiplier = 1f,
                cheekAlpha = 0f,
            ),
            petFaceTouchVisual(progress = 0f),
        )
        assertEquals(
            PetFaceTouchVisual(
                eyeScaleMultiplier = 1f,
                cheekAlpha = 0f,
            ),
            petFaceTouchVisual(progress = 1f),
        )
    }

    @Test
    fun earlyReactionGrowsEyesAndShowsCheeks() {
        val visual = petFaceTouchVisual(progress = 0.18f)

        assertEquals(1.18f, visual.eyeScaleMultiplier, 0.0001f)
        assertEquals(1f, visual.cheekAlpha, 0.0001f)
    }

    @Test
    fun lateReactionReturnsSmoothlyTowardNeutral() {
        val visual = petFaceTouchVisual(progress = 0.75f)

        assertTrue(visual.eyeScaleMultiplier in 1f..1.18f)
        assertTrue(visual.cheekAlpha in 0f..1f)
    }

    @Test
    fun progressIsClampedToValidRange() {
        assertEquals(
            petFaceTouchVisual(progress = 0f),
            petFaceTouchVisual(progress = -2f),
        )
        assertEquals(
            petFaceTouchVisual(progress = 1f),
            petFaceTouchVisual(progress = 3f),
        )
    }
}
