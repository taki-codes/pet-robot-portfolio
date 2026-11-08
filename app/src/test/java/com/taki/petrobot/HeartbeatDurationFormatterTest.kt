package com.taki.petrobot

import org.junit.Assert.assertEquals
import org.junit.Test

class HeartbeatDurationFormatterTest {
    @Test
    fun zeroDuration() {
        assertEquals("0d 00h 00m", HeartbeatDurationFormatter.format(0L))
    }

    @Test
    fun dayHourAndMinuteDuration() {
        val elapsedMillis = ((25 * 60L) + 1L) * 60_000L

        assertEquals("1d 01h 01m", HeartbeatDurationFormatter.format(elapsedMillis))
    }

    @Test
    fun negativeDurationIsClampedToZero() {
        assertEquals("0d 00h 00m", HeartbeatDurationFormatter.format(-1L))
    }
}
