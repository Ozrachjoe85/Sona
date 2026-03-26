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

data class SonaNote(
    val frequency: Double, 
    val durationMs: Long, 
    val waveType: String,
    val pan: Float = 0.5f // 0.0 (Left) to 1.0 (Right)
)

class SonaEngine {
    fun analyzeBitmap(bitmap: Bitmap): List<SonaNote> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 12, 12, false)
        return (0 until 12).map { i ->
            val p = scaled.getPixel(i, i)
            val freq = 150.0 + (AndroidColor.red(p) * 2.0)
            SonaNote(freq, 200, "sawtooth", i / 12f)
        }
    }

    fun convertLineToNote(line: SonaLine): SonaNote {
        // RULE 1: Y-Position determines Pitch
        val freq = 900.0 - (line.yOffset / 2.2).coerceIn(0.0, 750.0)
        
        // RULE 2: Color determines Waveform (using toArgb() for compatibility)
        val colorInt = line.color.toArgb()
        val wave = when(colorInt) {
            android.graphics.Color.CYAN -> "sine"
            android.graphics.Color.MAGENTA -> "square"
            android.graphics.Color.YELLOW -> "sawtooth"
            else -> "sine"
        }
        
        // RULE 3: Thickness determines Duration
        val duration = (line.thickness * 12).toLong().coerceIn(200, 2000)
        
        return SonaNote(freq, duration, wave)
    }
}

class SonaAudioPlayer {
    private val sampleRate = 44100

    fun playInstantTone(freq: Double, waveType: String, pan: Float = 0.5f) {
        thread {
            generateAndPlay(SonaNote(freq, 100, waveType, pan))
        }
    }

    fun playProcedural(notes: List<SonaNote>) {
        thread { notes.forEach { generateAndPlay(it) } }
    }

    private fun generateAndPlay(note: SonaNote) {
        val numSamples = (note.durationMs * sampleRate / 1000).toInt()
        // Buffer size is numSamples * 2 for Stereo (Left/Right)
        val buffer = ShortArray(numSamples * 2)

        val leftVol = 1.0f - note.pan
        val rightVol = note.pan

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val angle = 2.0 * PI * note.frequency * t
            
            // SYNTH ENGINE: Multi-harmonic oscillators
            val sample = when(note.waveType) {
                "sine" -> sin(angle) + 0.5 * sin(2 * angle) + 0.25 * sin(3 * angle)
                "square" -> sign(sin(angle)) * 0.5
                "sawtooth" -> (2.0 * (t * note.frequency - Math.floor(0.5 + t * note.frequency)))
                else -> sin(angle)
            }
            
            val finalSample = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.6).toInt().toShort()
            
            // Interleave Stereo Samples: [Left, Right, Left, Right...]
            buffer[i * 2] = (finalSample * leftVol).toInt().toShort()
            buffer[i * 2 + 1] = (finalSample * rightVol).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(note.durationMs)
        track.release()
    }
}
