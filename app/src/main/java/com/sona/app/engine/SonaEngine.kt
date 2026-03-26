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
    private val scales = mapOf(
        "warm" to listOf(60, 64, 67, 71, 72),
        "cold" to listOf(60, 63, 67, 70, 72)
    )

    fun analyzeBitmap(bitmap: Bitmap): SonaComposition {
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, false)
        var red = 0L; var blue = 0L
        for (x in 0 until 32) {
            for (y in 0 until 32) {
                val p = scaled.getPixel(x, y)
                red += AndroidColor.red(p)
                blue += AndroidColor.blue(p)
            }
        }
        val type = if (red > blue) "warm" else "cold"
        return SonaComposition(110, List(12) { (scales[type] ?: scales["warm"]!!).random() }, if (type == "warm") 0 else 40)
    }

    fun generateFromPaths(lineCount: Int): SonaComposition {
        return SonaComposition(100 + (lineCount * 2), List(8) { listOf(60, 62, 64, 67, 69).random() })
    }
}

class SonaAudioPlayer(context: Context) {
    private var midiReceiver: MidiReceiver? = null

    init {
        val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
        val devices = midiManager.devices
        if (devices.isNotEmpty()) {
            midiManager.openDevice(devices[0], { device ->
                midiReceiver = device.openInputPort(0)
                // Initialize with a Piano sound (0)
                midiReceiver?.send(byteArrayOf(0xC0.toByte(), 0x00), 0, 2)
            }, Handler(Looper.getMainLooper()))
        }
    }

    fun play(comp: SonaComposition) {
        val receiver = midiReceiver ?: return
        thread {
            val delay = (60000 / comp.bpm).toLong()
            // Switch instrument
            receiver.send(byteArrayOf(0xC0.toByte(), comp.instrument.toByte()), 0, 2)
            
            comp.notes.forEach { note ->
                receiver.send(byteArrayOf(0x90.toByte(), note.toByte(), 0x64.toByte()), 0, 3)
                Thread.sleep(delay)
                receiver.send(byteArrayOf(0x80.toByte(), note.toByte(), 0x00.toByte()), 0, 3)
            }
        }
    }
}
