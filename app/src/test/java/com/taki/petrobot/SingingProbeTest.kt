package com.taki.petrobot

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingingProbeTest {
    private val messages = mutableListOf<String>()
    private val guard = LocalAudioPlaybackGuard()
    private val probe = SingingProbe(guard, messages::add)
    private val tone = ShortArray(1600) { (8000 * sin(2 * PI * 220 * it / 16000)).toInt().toShort() }
    private val step = 100_000_000L

    @Test fun inactiveProbeDoesNothing() {
        repeat(30) { feed(it + 1) }
        assertTrue(messages.isEmpty())
    }

    @Test fun pauseAndResumeRequireFreshEvidence() {
        probe.setActive(true)
        repeat(20) { feed(it + 1) }
        assertTrue(messages.any { "candidate=true" in it })
        probe.setActive(false)
        messages.clear()
        repeat(30) { feed(it + 21) }
        assertTrue(messages.isEmpty())
        probe.setActive(true)
        feed(51)
        assertFalse(messages.any { "candidate=true" in it })
    }

    @Test fun ownAudioAndWindowsStraddlingItsEndDiscardEvidence() {
        probe.setActive(true)
        repeat(19) { feed(it + 1) }
        guard.suppressFor(20 * step, 12 * step)
        messages.clear()
        for (i in 20..32) feed(i)
        assertTrue(messages.any { "reason=local_playback" in it })
        assertFalse(messages.any { "candidate=true" in it })
        feed(33)
        assertTrue(messages.last().contains("ready=false"))
        for (i in 34..52) feed(i)
        assertTrue(messages.last().contains("candidate=true"))
    }

    @Test fun screenOffAndShortWindowsResetCandidate() {
        probe.setActive(true)
        repeat(20) { feed(it + 1) }
        probe.acceptPcm(tone, tone.size, 21 * step, interactive = false)
        assertTrue(messages.last().contains("reason=screen_off"))
        feed(22)
        assertTrue(messages.last().contains("ready=false"))
        probe.acceptPcm(tone, 100, 23 * step, interactive = true)
        assertTrue(messages.last().contains("reason=short_window"))
        feed(24)
        assertTrue(messages.last().contains("voicedWindows=1/1"))
    }

    private fun feed(index: Int) = probe.acceptPcm(tone, tone.size, index * step, interactive = true)
}
