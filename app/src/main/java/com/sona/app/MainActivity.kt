package com.sona.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sona.app.engine.*

class MainActivity : ComponentActivity() {
    private val engine = SonaEngine()
    private val player = SonaAudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val paths = remember { mutableStateListOf<Path>() }
            var currentPath by remember { mutableStateOf<Path?>(null) }

            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                Text("SONA", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(16.dp))
                
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> currentPath = Path().apply { moveTo(offset.x, offset.y) } },
                            onDrag = { change, _ -> 
                                currentPath?.lineTo(change.position.x, change.position.y)
                                val p = currentPath; currentPath = null; currentPath = p 
                            },
                            onDragEnd = { currentPath?.let { paths.add(it) }; currentPath = null }
                        )
                    }) {
                        paths.forEach { drawPath(it, Color.Black, style = Stroke(8f)) }
                        currentPath?.let { drawPath(it, Color.Black, style = Stroke(8f)) }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { paths.clear() }) { Text("Clear") }
                    Button(onClick = { 
                        val comp = engine.generateFromPaths(paths.size)
                        player.play(comp) 
                    }) { Text("Hear") }
                }
            }
        }
    }
}
