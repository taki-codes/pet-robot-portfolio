package com.taki.petrobot

internal object HeartbeatDurationFormatter {
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MINUTES_PER_HOUR = 60L
    private const val HOURS_PER_DAY = 24L

    fun format(elapsedMillis: Long): String {
        val totalMinutes = elapsedMillis.coerceAtLeast(0L) / MILLIS_PER_MINUTE
        val days = totalMinutes / (MINUTES_PER_HOUR * HOURS_PER_DAY)
        val hours = totalMinutes / MINUTES_PER_HOUR % HOURS_PER_DAY
        val minutes = totalMinutes % MINUTES_PER_HOUR
        return "${days}d ${hours.twoDigits()}h ${minutes.twoDigits()}m"
    }

    private fun Long.twoDigits(): String = toString().padStart(2, '0')
}
