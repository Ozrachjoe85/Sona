package com.sona.app.engine

import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

data class SonaComposition(val bpm: Int, val notes: List<Int>)

class SonaEngine {
    fun generateFromPaths(count: Int): SonaComposition {
        val notes = mutableListOf<Int>()
        val baseScale = listOf(60, 62, 64, 67, 69) // Pentatonic
        repeat(8) { notes.add(baseScale.random()) }
        return SonaComposition(bpm = 80 + (count * 5), notes = notes)
    }
}

class SonaAudioPlayer {
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    fun play(comp: SonaComposition) {
        thread {
            comp.notes.forEach {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                Thread.sleep((60000 / comp.bpm).toLong())
            }
        }
    }
}
