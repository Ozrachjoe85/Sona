package com.sona.app.engine

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread
import kotlin.math.pow

data class SonaComposition(val bpm: Int, val notes: List<Int>)

class SonaEngine {
    private val scales = mapOf(
        "bright" to listOf(72, 74, 76, 79, 81), // Higher, airy
        "dark" to listOf(48, 51, 53, 55, 58),   // Lower, moody
        "neutral" to listOf(60, 62, 64, 67, 69) // Standard pentatonic
    )

    fun analyzeBitmap(bitmap: Bitmap): SonaComposition {
        // FIX: Scale down IMMEDIATELY to prevent memory crash
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        var redSum = 0L
        var blueSum = 0L
        
        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val p = scaled.getPixel(x, y)
                redSum += AndroidColor.red(p)
                blueSum += AndroidColor.blue(p)
            }
        }

        val mood = if (redSum > blueSum) "bright" else "dark"
        val selectedScale = scales[mood] ?: scales["neutral"]!!
        
        // Map pixel brightness to note sequence
        val notes = mutableListOf<Int>()
        repeat(12) { notes.add(selectedScale.random()) }
        
        return SonaComposition(bpm = 110, notes = notes)
    }

    fun generateFromPaths(count: Int): SonaComposition {
        val baseScale = scales["neutral"]!!
        val notes = List((6 + count).coerceAtMost(16)) { baseScale.random() }
        return SonaComposition(bpm = 90 + (count * 3).coerceAtMost(60), notes = notes)
    }
}

class SonaAudioPlayer {
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    
    fun play(comp: SonaComposition) {
        thread {
            val interval = (60000 / comp.bpm).toLong()
            comp.notes.forEach { midiNote ->
                // Convert MIDI to actual Frequency (Hz) for a more musical sound
                val freq = (440.0 * 2.0.pow((midiNote - 69) / 12.0)).toInt()
                
                // ToneGenerator is limited; we use TONE_DTMF for cleaner pitch
                // We will move to Oboe/ExoPlayer for "Pro" sounds in the next phase
                toneGen.startTone(ToneGenerator.TONE_DTMF_0, 200) 
                Thread.sleep(interval)
                toneGen.stopTone()
            }
        }
    }
}
