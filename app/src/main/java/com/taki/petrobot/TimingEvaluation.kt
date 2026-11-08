package com.taki.petrobot

enum class TimingRating {
    TOO_EARLY,
    JUST_RIGHT,
    TOO_LATE,
}

data class TimingEvaluationRecord(
    val rating: TimingRating,
    val pressedAtNanos: Long,
    val referenceBreakState: String,
    val millisSinceBreakStateChange: Long?,
)

fun createTimingEvaluationRecord(
    rating: TimingRating,
    pressedAtNanos: Long,
    referenceBreakState: String,
    breakStateChangedAtNanos: Long,
): TimingEvaluationRecord {
    require(pressedAtNanos >= 0L)
    require(breakStateChangedAtNanos >= 0L)

    val offsetMillis = if (
        breakStateChangedAtNanos > 0L &&
        pressedAtNanos >= breakStateChangedAtNanos
    ) {
        (pressedAtNanos - breakStateChangedAtNanos) / NANOS_PER_MILLISECOND
    } else {
        null
    }

    return TimingEvaluationRecord(
        rating = rating,
        pressedAtNanos = pressedAtNanos,
        referenceBreakState = referenceBreakState,
        millisSinceBreakStateChange = offsetMillis,
    )
}

private const val NANOS_PER_MILLISECOND = 1_000_000L
