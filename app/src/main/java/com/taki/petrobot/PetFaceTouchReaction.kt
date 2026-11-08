package com.taki.petrobot

const val PET_FACE_TOUCH_DURATION_MILLIS = 900L

data class PetFaceTouchVisual(
    val eyeScaleMultiplier: Float,
    val cheekAlpha: Float,
)

fun petFaceTouchVisual(progress: Float): PetFaceTouchVisual {
    val boundedProgress = progress.coerceIn(0f, 1f)
    val eyeScale = when {
        boundedProgress <= TOUCH_GROWTH_END -> {
            lerp(
                start = 1f,
                end = MAX_TOUCH_EYE_SCALE,
                fraction = boundedProgress / TOUCH_GROWTH_END,
            )
        }
        else -> {
            lerp(
                start = MAX_TOUCH_EYE_SCALE,
                end = 1f,
                fraction = (boundedProgress - TOUCH_GROWTH_END) /
                    (1f - TOUCH_GROWTH_END),
            )
        }
    }
    val cheekAlpha = when {
        boundedProgress <= CHEEK_FADE_IN_END ->
            boundedProgress / CHEEK_FADE_IN_END
        boundedProgress <= CHEEK_HOLD_END -> 1f
        else ->
            (1f - boundedProgress) / (1f - CHEEK_HOLD_END)
    }

    return PetFaceTouchVisual(
        eyeScaleMultiplier = eyeScale,
        cheekAlpha = cheekAlpha.coerceIn(0f, 1f),
    )
}

private fun lerp(
    start: Float,
    end: Float,
    fraction: Float,
): Float = start + (end - start) * fraction.coerceIn(0f, 1f)

private const val MAX_TOUCH_EYE_SCALE = 1.18f
private const val TOUCH_GROWTH_END = 0.18f
private const val CHEEK_FADE_IN_END = 0.12f
private const val CHEEK_HOLD_END = 0.55f
