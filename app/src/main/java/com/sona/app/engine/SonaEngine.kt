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
import kotlin.math.sign

data class SonaNote(val frequency: Double, val durationMs: Long, val waveType: String)

class SonaEngine {
    fun analyzeBitmap(bitmap: Bitmap): List<SonaNote> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 12, 12, false)
        return (0 until 12).map { i ->
            val p = scaled.getPixel(i, i)
            val freq = 150.0 + (AndroidColor.red(p) * 2.0)
            SonaNote(freq, 200, "sawtooth")
        }
    }

    fun convertLineToNote(line: SonaLine): SonaNote {
        val freq = 900.0 - (line.yOffset / 2.2).coerceIn(0.0, 750.0)
        val wave = when(line.color) {
            android.graphics.Color.CYAN -> "sine"
            android.graphics.Color.MAGENTA -> "square"
            android.graphics.Color.YELLOW -> "sawtooth"
            else -> "sine"
        }
        return SonaNote(freq, (line.thickness * 12).toLong().coerceIn(200, 2000), wave)
    }
}

class SonaAudioPlayer {
    private val sampleRate = 44100

    // For Real-time feedback while drawing
    fun playInstantTone(freq: Double, waveType: String) {
        thread {
            generateAndPlay(SonaNote(freq, 80, waveType))
        }
    }

    fun playProcedural(notes: List<SonaNote>) {
        thread { notes.forEach { generateAndPlay(it) } }
    }

    private fun generateAndPlay(note: SonaNote) {
        val numSamples = (note.durationMs * sampleRate / 1000).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val angle = 2.0 * PI * note.frequency * t
            
            // SYNTH ENGINE: Adding Harmonics (Overtones)
            val sample = when(note.waveType) {
                "sine" -> sin(angle) + 0.5 * sin(2 * angle) + 0.25 * sin(3 * angle)
                "square" -> sign(sin(angle)) * 0.6
                "sawtooth" -> (2.0 * (t * note.frequency - Math.floor(0.5 + t * note.frequency)))
                else -> sin(angle)
            }
            
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.7).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(note.durationMs)
        track.release()
    }
}
