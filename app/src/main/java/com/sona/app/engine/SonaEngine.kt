package com.sona.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread

data class SonaComposition(val bpm: Int, val notes: List<Int>, val instrument: Int = 0)

class SonaEngine {
    // Scales for different "Scanner" results
    private val palettes = mapOf(
        "warm" to listOf(60, 64, 67, 71, 72), // Major 7th (Bright/Warm)
        "cold" to listOf(60, 63, 67, 70, 72),  // Minor 7th (Moody/Cold)
    )

    fun analyzeBitmap(bitmap: Bitmap): SonaComposition {
        // CRASH FIX: Scale down to a tiny 32x32 grid to save RAM
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, false)
        var red = 0L; var blue = 0L; var brightness = 0L

        for (x in 0 until 32) {
            for (y in 0 until 32) {
                val p = scaled.getPixel(x, y)
                red += AndroidColor.red(p)
                blue += AndroidColor.blue(p)
                brightness += (AndroidColor.red(p) + AndroidColor.green(p) + AndroidColor.blue(p)) / 3
            }
        }

        val type = if (red > blue) "warm" else "cold"
        val scale = palettes[type] ?: palettes["warm"]!!
        
        // Map brightness to BPM: darker = slower/ambient
        val avgBrightness = (brightness / (32 * 32)).toInt()
        val bpm = 60 + (avgBrightness / 4)

        return SonaComposition(bpm, List(16) { scale.random() }, if (type == "warm") 0 else 40)
    }

    fun generateFromPaths(lineCount: Int): SonaComposition {
        val scale = palettes["warm"]!!
        return SonaComposition(90 + (lineCount * 5), List(8) { scale.random() })
    }
}

class SonaAudioPlayer(context: Context) {
    private var midiReceiver: MidiReceiver? = null

    init {
        val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
        // Open the default Internal Synth (Sonivox)
        midiManager.getDevices().firstOrNull()?.let { info ->
            midiManager.openDevice(info, { device ->
                val port = device.openInputPort(0)
                midiReceiver = port
            }, Handler(Looper.getMainLooper()))
        }
    }

    fun play(comp: SonaComposition) {
        val receiver = midiReceiver ?: return
        thread {
            val delay = (60000 / comp.bpm).toLong()
            
            // Set Instrument (Program Change)
            receiver.send(byteArrayOf(0xC0.toByte(), comp.instrument.toByte()), 0, 2)

            comp.notes.forEach { note ->
                val noteOn = byteArrayOf(0x90.toByte(), note.toByte(), 0x60.toByte())
                val noteOff = byteArrayOf(0x80.toByte(), note.toByte(), 0x00.toByte())

                receiver.send(noteOn, 0, 3)
                Thread.sleep(delay)
                receiver.send(noteOff, 0, 3)
            }
        }
    }
}
