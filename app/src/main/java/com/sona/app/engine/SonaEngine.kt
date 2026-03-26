package com.sona.app.engine

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.sona.app.SonaLine
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

// The data structure for our procedural "rules"
data class SonaNote(val frequency: Double, val durationMs: Long, val waveType: String)

class SonaEngine {

    // RULE 1: Analyze an uploaded image to create a procedural sequence
    fun analyzeBitmap(bitmap: Bitmap): List<SonaNote> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 12, 12, false)
        val notes = mutableListOf<SonaNote>()
        
        for (i in 0 until 12) {
            val pixel = scaled.getPixel(i, i)
            val r = AndroidColor.red(pixel)
            val b = AndroidColor.blue(pixel)
            
            // Logic: More Red = Higher Pitch, More Blue = Lower Pitch
            val freq = 200.0 + (r * 2.0) + (b * 0.5)
            notes.add(SonaNote(freq, 250, "sine"))
        }
        return notes
    }

    // RULE 2: Convert a hand-drawn line into a specific procedural note
    fun convertLineToNote(line: SonaLine): SonaNote {
        // Y-Position determines Pitch (Higher on screen = Higher Frequency)
        // We map the screen Y to a range between 200Hz and 1000Hz
        val freq = 1000.0 - (line.yOffset / 2.0).coerceIn(0.0, 800.0)
        
        // Thickness determines Duration (Thicker = Longer, bolder note)
        val duration = (line.thickness * 15).toLong().coerceIn(150, 2000)
        
        return SonaNote(freq, duration, "sine")
    }
}

class SonaAudioPlayer {
    private val sampleRate = 44100

    fun playProcedural(notes: List<SonaNote>) {
        if (notes.isEmpty()) return
        
        thread {
            notes.forEach { note ->
                val numSamples = (note.durationMs * sampleRate / 1000).toInt()
                val buffer = ShortArray(numSamples)

                // Procedural Math: Generating the actual sound wave bit-by-bit
                for (i in 0 until numSamples) {
                    val angle = 2.0 * PI * i / (sampleRate / note.frequency)
                    val sample = sin(angle)
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                
                // Keep the track alive for the duration of the note
                Thread.sleep(note.durationMs)
                audioTrack.release()
            }
        }
    }
}
