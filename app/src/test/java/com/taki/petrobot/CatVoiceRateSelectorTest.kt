package com.taki.petrobot

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatVoiceRateSelectorTest {
    @Test
    fun ratesStayInApprovedRangeAndDoNotRepeatConsecutively() {
        val selector = CatVoiceRateSelector(Random(30))
        val rates = List(300) { selector.nextRate() }
        assertEquals(setOf(0.97f, 1f, 1.03f), rates.toSet())
        rates.forEach { assertTrue(it in 0.97f..1.03f) }
        rates.zipWithNext().forEach { (previous, current) ->
            assertNotEquals(previous, current)
        }
    }

    @Test
    fun injectedRandomMakesSelectionReproducible() {
        val first = CatVoiceRateSelector(Random(123))
        val second = CatVoiceRateSelector(Random(123))
        assertEquals(List(30) { first.nextRate() }, List(30) { second.nextRate() })
    }
}
