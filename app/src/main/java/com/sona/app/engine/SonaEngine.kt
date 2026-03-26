package com.sona.app.engine

import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

data class SonaComposition(val bpm: Int, val notes: List<Int>)

class SonaEngine {
    fun generateFromPaths(count: Int): SonaComposition {
        val notes = mutableListOf<Int>()
        val baseScale = listOf(60, 62, 64, 67, 69) // Pentatonic
        // More paths = more complex sequence
        val length = (8 + count).coerceAtMost(32)
        repeat(length) { notes.add(baseScale.random()) }
        return SonaComposition(bpm = (80 + (count * 2)).coerceAtMost(160), notes = notes)
    }
}

class SonaAudioPlayer {
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    
    fun play(comp: SonaComposition) {
        thread {
            val interval = (60000 / comp.bpm).toLong()
            comp.notes.forEach { _ ->
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                Thread.sleep(interval)
            }
        }
    }
}
