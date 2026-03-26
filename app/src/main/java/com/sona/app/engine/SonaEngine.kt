package com.sona.app.engine

import android.graphics.Bitmap
import android.graphics.Color

data class SonaComposition(
    val bpm: Int,
    val scale: List<Int>,
    val sequence: List<Int>
)

class SonaEngine {
    // Basic Pentatonic Scale for "always sounds good" feeling
    private val majorPentatonic = listOf(60, 62, 64, 67, 69, 72)

    fun interpretImage(bitmap: Bitmap): SonaComposition {
        val width = bitmap.width
        val height = bitmap.height
        
        // 1. Determine BPM by pixel density (more ink = faster)
        var inkCount = 0
        val sampleSize = 10
        val notes = mutableListOf<Int>()

        for (x in 0 until width step sampleSize) {
            for (y in 0 until height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) > 0) {
                    inkCount++
                    // 2. Map Y-coordinate to Pitch
                    val pitchIndex = ((1.0f - (y.toFloat() / height)) * (majorPentatonic.size - 1)).toInt()
                    notes.add(majorPentatonic[pitchIndex.coerceIn(0, majorPentatonic.size - 1)])
                }
            }
        }

        val calculatedBpm = (60 + (inkCount / 100)).coerceIn(60, 180)
        
        return SonaComposition(
            bpm = calculatedBpm,
            scale = majorPentatonic,
            sequence = notes.take(16) // Limit to a 16-beat loop for Phase 1
        )
    }
}
