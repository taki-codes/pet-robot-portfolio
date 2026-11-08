package com.taki.petrobot

import kotlin.random.Random

const val CAT_VOICE_MIN_RATE = 0.97f
const val CAT_VOICE_MAX_RATE = 1.03f

/** Small playback-rate changes also change pitch; the WAV itself stays untouched. */
class CatVoiceRateSelector(
    private val random: Random = Random.Default,
) {
    private val rates = floatArrayOf(CAT_VOICE_MIN_RATE, 1f, CAT_VOICE_MAX_RATE)
    private var previousIndex = -1

    @Synchronized
    fun nextRate(): Float {
        val hasPrevious = previousIndex >= 0
        var index = random.nextInt(if (hasPrevious) rates.size - 1 else rates.size)
        if (hasPrevious && index >= previousIndex) index += 1
        previousIndex = index
        return rates[index]
    }
}
