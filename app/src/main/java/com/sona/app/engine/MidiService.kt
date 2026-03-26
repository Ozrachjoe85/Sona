package com.sona.app.engine

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import javax.sound.midi.*

class SonaAudioPlayer {
    private var synthesizer: Synthesizer? = null
    private var receiver: Receiver? = null

    init {
        try {
            synthesizer = MidiSystem.getSynthesizer()
            synthesizer?.open()
            receiver = synthesizer?.receiver
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playComposition(composition: SonaComposition) {
        val msg = ShortMessage()
        val delayPerNote = (60000 / composition.bpm).toLong()
        
        Thread {
            composition.sequence.forEach { pitch ->
                // Note On
                msg.setMessage(ShortMessage.NOTE_ON, 0, pitch, 90)
                receiver?.send(msg, -1)
                
                Thread.sleep(delayPerNote)
                
                // Note Off
                msg.setMessage(ShortMessage.NOTE_OFF, 0, pitch, 0)
                receiver?.send(msg, -1)
            }
        }.start()
    }
}
