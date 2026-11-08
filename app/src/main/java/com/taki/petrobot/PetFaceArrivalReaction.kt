package com.taki.petrobot

const val PET_FACE_ARRIVAL_DURATION_MILLIS = 4_000L

data class PetFaceArrivalVisual(
    val eyeScaleMultiplier: Float,
    val cheekAlpha: Float,
    val glowAlphaBoost: Float,
    val smileAlpha: Float,
)

data class PetFaceEyeLayerAlpha(
    val regularEyeAlpha: Float,
    val smileEyeAlpha: Float,
)

fun petFaceEyeLayerAlpha(smileAlpha: Float): PetFaceEyeLayerAlpha {
    val boundedSmileAlpha = smileAlpha.coerceIn(0f, 1f)
    return if (boundedSmileAlpha > 0f) {
        PetFaceEyeLayerAlpha(
            regularEyeAlpha = 0f,
            smileEyeAlpha = boundedSmileAlpha,
        )
    } else {
        PetFaceEyeLayerAlpha(
            regularEyeAlpha = 1f,
            smileEyeAlpha = 0f,
        )
    }
}

fun shouldTriggerArrivalReaction(
    previousMood: PetFaceMood,
    currentMood: PetFaceMood,
): Boolean = previousMood == PetFaceMood.RESTING &&
    currentMood != PetFaceMood.RESTING

fun petFaceArrivalVisual(progress: Float): PetFaceArrivalVisual {
    val boundedProgress = progress.coerceIn(0f, 1f)
    val pulse = if (boundedProgress <= ARRIVAL_PEAK_PROGRESS) {
        boundedProgress / ARRIVAL_PEAK_PROGRESS
    } else {
        (1f - boundedProgress) / (1f - ARRIVAL_PEAK_PROGRESS)
    }.coerceIn(0f, 1f)

    val smileAlpha = if (boundedProgress > 0f && boundedProgress < 1f) {
        1f
    } else {
        0f
    }
    val cheekAlpha = smileAlpha

    return PetFaceArrivalVisual(
        eyeScaleMultiplier = 1f + MAX_ARRIVAL_EYE_GROWTH * pulse,
        cheekAlpha = cheekAlpha,
        glowAlphaBoost = MAX_GLOW_ALPHA_BOOST * pulse,
        smileAlpha = smileAlpha,
    )
}

private const val ARRIVAL_PEAK_PROGRESS = 0.28f
private const val MAX_ARRIVAL_EYE_GROWTH = 0.14f
private const val MAX_GLOW_ALPHA_BOOST = 0.24f
