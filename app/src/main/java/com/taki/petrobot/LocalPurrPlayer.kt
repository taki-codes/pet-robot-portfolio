package com.taki.petrobot

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

interface LocalPurrPlayer : AutoCloseable {
    fun play(): Boolean
}

class SoundPoolLocalPurrPlayer(
    context: Context,
    soundResource: Int = R.raw.miina_purr,
) : LocalPurrPlayer {
    private val rateSelector = CatVoiceRateSelector()
    private val closed = AtomicBoolean(false)
    private val loadedSoundId = AtomicInteger(NO_SOUND_ID)
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == LOAD_SUCCESS && !closed.get()) {
                loadedSoundId.set(sampleId)
            }
        }
        soundPool.load(context, soundResource, SOUND_PRIORITY)
    }

    override fun play(): Boolean {
        if (closed.get()) return false

        val soundId = loadedSoundId.get()
        if (soundId == NO_SOUND_ID) return false

        val rate = rateSelector.nextRate()
        val played = soundPool.play(
            soundId,
            FULL_VOLUME,
            FULL_VOLUME,
            SOUND_PRIORITY,
            NO_LOOP,
            rate,
        ) != PLAY_FAILED
        if (played) Log.i("PetRobotPurr", "catVoiceRate=$rate")
        return played
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        loadedSoundId.set(NO_SOUND_ID)
        soundPool.release()
    }

    private companion object {
        const val LOAD_SUCCESS = 0
        const val NO_SOUND_ID = 0
        const val PLAY_FAILED = 0
        const val SOUND_PRIORITY = 1
        const val NO_LOOP = 0
        const val FULL_VOLUME = 1f
    }
}
