package com.taki.petrobot

data class GazeTarget(
    val horizontal: Float,
    val vertical: Float,
)

fun normalizedGazeTarget(
    tapX: Float,
    tapY: Float,
    width: Float,
    height: Float,
): GazeTarget {
    if (width <= 0f || height <= 0f) {
        return GazeTarget(horizontal = 0f, vertical = 0f)
    }

    return GazeTarget(
        horizontal = ((tapX / width) * 2f - 1f).coerceIn(-1f, 1f),
        vertical = ((tapY / height) * 2f - 1f).coerceIn(-1f, 1f),
    )
}
