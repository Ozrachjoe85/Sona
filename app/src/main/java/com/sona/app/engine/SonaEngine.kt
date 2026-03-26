package com.sona.app.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import kotlin.concurrent.thread
import kotlin.math.sin
import kotlin.math.PI

data class SonaNote(val frequency: Double, val durationMs: Long, val waveType: String)

class SonaEngine {
    // Maps colors to base frequencies
    fun getFrequencyFromColor(color: Int): Double {
        val r = AndroidColor.red(color)
        val b = AndroidColor.blue(color)
        return if (r > b) 440.0 + r else 220.0 + b
    }

    fun analyzeBitmap(bitmap: Bitmap): List<SonaNote> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 10, 10, false)
        val notes = mutableListOf<SonaNote>()
        for (i in 0 until 10) {
            val pixel = scaled.getPixel(i, i)
            val freq = getFrequencyFromColor(pixel)
            notes.add(SonaNote(freq, 300, "sine"))
        }
        return notes
    }
}

class SonaAudioPlayer {
    private val sampleRate = 44100

    fun playProcedural(notes: List<SonaNote>) {
        thread {
            notes.forEach { note ->
                val numSamples = (note.durationMs * sampleRate / 1000).toInt()
                val samples = DoubleArray(numSamples)
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    // Rule-based Math: Calculating the actual wave
                    samples[i] = sin(2.0 * PI * i / (sampleRate / note.frequency))
                    buffer[i] = (samples[i] * Short.MAX_VALUE).toInt().toShort()
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
                Thread.sleep(note.durationMs)
                audioTrack.release()
            }
        }
    }
}
