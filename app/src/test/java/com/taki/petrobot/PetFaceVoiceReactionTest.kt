package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Test

class PetFaceVoiceReactionTest {
    @Test
    fun reactionDurationIsShortAndBounded() {
        assertEquals(800L, PET_FACE_VOICE_REACTION_DURATION_MILLIS)
        assertEquals(
            PetFaceVoiceReactionVisual(
                eyeScaleMultiplier = 1f,
                glowAlphaBoost = 0f,
                attentionMarkAlpha = 0f,
            ),
            petFaceVoiceReactionVisual(progress = 0f),
        )
        assertEquals(
            PetFaceVoiceReactionVisual(
                eyeScaleMultiplier = 1f,
                glowAlphaBoost = 0f,
                attentionMarkAlpha = 0f,
            ),
            petFaceVoiceReactionVisual(progress = 1f),
        )
    }

    @Test
    fun peakReactionWidensAndBrightensEyes() {
        val visual = petFaceVoiceReactionVisual(progress = 0.25f)

        assertEquals(1.16f, visual.eyeScaleMultiplier, 0.0001f)
        assertEquals(0.28f, visual.glowAlphaBoost, 0.0001f)
        assertEquals(1f, visual.attentionMarkAlpha, 0.0001f)
    }

    @Test
    fun attentionMarkSwitchesWithoutFading() {
        listOf(0.001f, 0.25f, 0.999f).forEach { progress ->
            assertEquals(
                1f,
                petFaceVoiceReactionVisual(progress).attentionMarkAlpha,
                0.0001f,
            )
        }
        assertEquals(
            0f,
            petFaceVoiceReactionVisual(progress = 0f).attentionMarkAlpha,
            0.0001f,
        )
        assertEquals(
            0f,
            petFaceVoiceReactionVisual(progress = 1f).attentionMarkAlpha,
            0.0001f,
        )
    }

    @Test
    fun progressIsClamped() {
        assertEquals(
            petFaceVoiceReactionVisual(progress = 0f),
            petFaceVoiceReactionVisual(progress = -1f),
        )
        assertEquals(
            petFaceVoiceReactionVisual(progress = 1f),
            petFaceVoiceReactionVisual(progress = 2f),
        )
    }
}
