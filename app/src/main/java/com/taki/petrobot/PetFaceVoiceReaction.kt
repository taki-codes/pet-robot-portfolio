package com.taki.petrobot

const val PET_FACE_VOICE_REACTION_DURATION_MILLIS = 800L

data class PetFaceVoiceReactionVisual(
    val eyeScaleMultiplier: Float,
    val glowAlphaBoost: Float,
    val attentionMarkAlpha: Float,
)

fun petFaceVoiceReactionVisual(progress: Float): PetFaceVoiceReactionVisual {
    val boundedProgress = progress.coerceIn(0f, 1f)
    val pulse = if (boundedProgress <= VOICE_REACTION_PEAK_PROGRESS) {
        boundedProgress / VOICE_REACTION_PEAK_PROGRESS
    } else {
        (1f - boundedProgress) / (1f - VOICE_REACTION_PEAK_PROGRESS)
    }.coerceIn(0f, 1f)

    val attentionMarkAlpha = if (boundedProgress > 0f && boundedProgress < 1f) {
        1f
    } else {
        0f
    }

    return PetFaceVoiceReactionVisual(
        eyeScaleMultiplier = 1f + MAX_VOICE_EYE_GROWTH * pulse,
        glowAlphaBoost = MAX_VOICE_GLOW_ALPHA_BOOST * pulse,
        attentionMarkAlpha = attentionMarkAlpha,
    )
}

private const val VOICE_REACTION_PEAK_PROGRESS = 0.25f
private const val MAX_VOICE_EYE_GROWTH = 0.16f
private const val MAX_VOICE_GLOW_ALPHA_BOOST = 0.28f
