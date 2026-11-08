package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFaceArrivalReactionTest {
    @Test
    fun restingToCurious_triggersGreeting() {
        assertTrue(
            shouldTriggerArrivalReaction(
                previousMood = PetFaceMood.RESTING,
                currentMood = PetFaceMood.CURIOUS,
            ),
        )
    }

    @Test
    fun restingToAttentive_triggersGreeting() {
        assertTrue(
            shouldTriggerArrivalReaction(
                previousMood = PetFaceMood.RESTING,
                currentMood = PetFaceMood.ATTENTIVE,
            ),
        )
    }

    @Test
    fun nonRestingMoodChanges_doNotTriggerGreeting() {
        assertFalse(
            shouldTriggerArrivalReaction(
                previousMood = PetFaceMood.CURIOUS,
                currentMood = PetFaceMood.ATTENTIVE,
            ),
        )
        assertFalse(
            shouldTriggerArrivalReaction(
                previousMood = PetFaceMood.ENGAGED,
                currentMood = PetFaceMood.BREAK_READY,
            ),
        )
    }

    @Test
    fun stayingResting_doesNotTriggerGreeting() {
        assertFalse(
            shouldTriggerArrivalReaction(
                previousMood = PetFaceMood.RESTING,
                currentMood = PetFaceMood.RESTING,
            ),
        )
    }

    @Test
    fun settledProgress_usesNeutralVisuals() {
        val neutral = PetFaceArrivalVisual(
            eyeScaleMultiplier = 1f,
            cheekAlpha = 0f,
            glowAlphaBoost = 0f,
            smileAlpha = 0f,
        )

        assertEquals(neutral, petFaceArrivalVisual(progress = 0f))
        assertEquals(neutral, petFaceArrivalVisual(progress = 1f))
    }

    @Test
    fun peakProgress_brightensAndEnlargesEyes() {
        val visual = petFaceArrivalVisual(progress = 0.28f)

        assertEquals(1.14f, visual.eyeScaleMultiplier, 0.0001f)
        assertEquals(0.24f, visual.glowAlphaBoost, 0.0001f)
        assertEquals(1f, visual.smileAlpha, 0.0001f)
        assertTrue(visual.cheekAlpha > 0f)
    }

    @Test
    fun smileAndCheeksSwitchTogetherForFourSeconds() {
        assertEquals(4_000L, PET_FACE_ARRIVAL_DURATION_MILLIS)

        listOf(0.001f, 0.50f, 0.999f).forEach { progress ->
            val visual = petFaceArrivalVisual(progress)

            assertEquals(1f, visual.smileAlpha, 0.0001f)
            assertEquals(1f, visual.cheekAlpha, 0.0001f)
        }

        assertEquals(0f, petFaceArrivalVisual(progress = 0f).smileAlpha, 0.0001f)
        assertEquals(0f, petFaceArrivalVisual(progress = 0f).cheekAlpha, 0.0001f)
        assertEquals(0f, petFaceArrivalVisual(progress = 1f).smileAlpha, 0.0001f)
        assertEquals(0f, petFaceArrivalVisual(progress = 1f).cheekAlpha, 0.0001f)
    }

    @Test
    fun regularAndSmileEyesAreNeverDrawnTogether() {
        val neutralLayers = petFaceEyeLayerAlpha(smileAlpha = 0f)

        assertEquals(1f, neutralLayers.regularEyeAlpha, 0.0001f)
        assertEquals(0f, neutralLayers.smileEyeAlpha, 0.0001f)

        listOf(0.01f, 0.25f, 0.75f, 1f).forEach { smileAlpha ->
            val smileLayers = petFaceEyeLayerAlpha(smileAlpha)

            assertEquals(0f, smileLayers.regularEyeAlpha, 0.0001f)
            assertEquals(smileAlpha, smileLayers.smileEyeAlpha, 0.0001f)
        }
    }

    @Test
    fun eyeLayerAlphaIsClamped() {
        assertEquals(
            PetFaceEyeLayerAlpha(regularEyeAlpha = 1f, smileEyeAlpha = 0f),
            petFaceEyeLayerAlpha(smileAlpha = -1f),
        )
        assertEquals(
            PetFaceEyeLayerAlpha(regularEyeAlpha = 0f, smileEyeAlpha = 1f),
            petFaceEyeLayerAlpha(smileAlpha = 2f),
        )
    }

    @Test
    fun progressIsClamped() {
        assertEquals(
            petFaceArrivalVisual(progress = 0f),
            petFaceArrivalVisual(progress = -1f),
        )
        assertEquals(
            petFaceArrivalVisual(progress = 1f),
            petFaceArrivalVisual(progress = 2f),
        )
    }
}
