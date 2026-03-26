package com.sona.app.engine

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

data class SonaComposition(val bpm: Int, val notes: List<Int>)

class SonaEngine {
    // Different "Moods" based on color
    private val scales = mapOf(
        "bright" to listOf(72, 74, 76, 79, 81), // High C Pentatonic
        "dark" to listOf(48, 51, 53, 55, 58),   // C Minor
        "neutral" to listOf(60, 62, 64, 67, 69) // Middle C Major
    )

    fun analyzeBitmap(bitmap: Bitmap): SonaComposition {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
        var totalRed = 0
        var totalBlue = 0
        
        for (x in 0 until 50) {
            for (y in 0 until 50) {
                val p = scaled.getPixel(x, y)
                totalRed += AndroidColor.red(p)
                totalBlue += AndroidColor.blue(p)
            }
        }

        val mood = when {
            totalRed > totalBlue -> "bright"
            totalBlue > totalRed -> "dark"
            else -> "neutral"
        }

        val notes = mutableListOf<Int>()
        val selectedScale = scales[mood] ?: scales["neutral"]!!
        repeat(16) { notes.add(selectedScale.random()) }
        
        return SonaComposition(bpm = 100, notes = notes)
    }

    fun generateFromPaths(count: Int): SonaComposition {
        val baseScale = scales["neutral"]!!
        val notes = List(8) { baseScale.random() }
        return SonaComposition(bpm = 80 + (count * 4).coerceAtMost(80), notes = notes)
    }
}

class SonaAudioPlayer {
    private val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    
    fun play(comp: SonaComposition) {
        thread {
            val interval = (60000 / comp.bpm).toLong()
            comp.notes.forEach { note ->
                // Map MIDI note to frequency for the ToneGenerator
                val freq = (440 * Math.pow(2.0, (note - 69) / 12.0)).toInt()
                // Use different beep types for Phase 1 "Texture"
                val toneType = if (note > 70) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2
                toneGen.startTone(toneType, 150)
                Thread.sleep(interval)
            }
        }
    }
}
