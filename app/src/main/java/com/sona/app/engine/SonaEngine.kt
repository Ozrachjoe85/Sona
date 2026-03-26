package com.sona.app.engine

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.ui.graphics.toArgb
import com.sona.app.SonaLine
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sign

data class SonaNote(val frequency: Double, val durationMs: Long, val waveType: String, val pan: Float = 0.5f)

class SonaEngine {
    fun analyzeBitmap(bitmap: Bitmap): List<SonaNote> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, false)
        return (0 until 8).map { i ->
            val p = scaled.getPixel(i, i)
            SonaNote(150.0 + (AndroidColor.red(p) * 1.5), 300, "sawtooth", i / 8f)
        }
    }

    fun convertLineToNote(line: SonaLine): SonaNote {
        val freq = 800.0 - (line.yOffset / 2.5).coerceIn(0.0, 700.0)
        val colorInt = line.color.toArgb()
        val wave = when(colorInt) {
            -16711681 -> "sine"      // Cyan
            -65281 -> "square"      // Magenta
            -256 -> "sawtooth"      // Yellow
            else -> "sine"
        }
        return SonaNote(freq, (line.thickness * 10).toLong().coerceIn(300, 1500), wave)
    }
}

class SonaAudioPlayer {
    private val sampleRate = 44100
    private var isPlayingInstant = false // Prevention gate for crashes

    fun playInstantTone(freq: Double, waveType: String) {
        if (isPlayingInstant) return // Skip if a tone is already triggering to save CPU
        isPlayingInstant = true
        thread {
            generateAndPlay(SonaNote(freq, 120, waveType))
            isPlayingInstant = false
        }
    }

    fun playProcedural(notes: List<SonaNote>) {
        thread { notes.forEach { generateAndPlay(it) } }
    }

    private fun generateAndPlay(note: SonaNote) {
        val numSamples = (note.durationMs * sampleRate / 1000).toInt()
        val buffer = ShortArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val angle = 2.0 * PI * note.frequency * t
            
            // Envelope: Gradually fade out to prevent "weird" clicking sounds
            val fadeOut = (numSamples - i).toDouble() / numSamples

            val sample = when(note.waveType) {
                "sine" -> (sin(angle) + 0.4 * sin(2 * angle)) 
                "square" -> sign(sin(angle)) * 0.3
                "sawtooth" -> (2.0 * (t * note.frequency - Math.floor(0.5 + t * note.frequency)))
                else -> sin(angle)
            }
            
            // Apply envelope and volume
            val finalSample = (sample * fadeOut * Short.MAX_VALUE * 0.5).toInt().toShort()
            
            buffer[i * 2] = finalSample     // Left
            buffer[i * 2 + 1] = finalSample // Right
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(note.durationMs)
            track.release()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
