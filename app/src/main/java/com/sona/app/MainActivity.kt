package com.sona.app

import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sona.app.engine.SonaAudioPlayer
import com.sona.app.engine.SonaEngine

class MainActivity : ComponentActivity() {
    private val engine = SonaEngine()
    private val player = SonaAudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SonaApp(engine, player)
        }
    }
}

@Composable
fun SonaApp(engine: SonaEngine, player: SonaAudioPlayer) {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    // We use this to "render" the paths to a bitmap for the engine
    val drawModifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .background(Color.White)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                },
                onDrag = { change, _ ->
                    currentPath?.lineTo(change.position.x, change.position.y)
                    val p = currentPath
                    currentPath = null 
                    currentPath = p
                },
                onDragEnd = {
                    currentPath?.let { paths.add(it) }
                    currentPath = null
                }
            )
        }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("SONA", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(16.dp))
        
        Box(modifier = drawModifier) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                paths.forEach { drawPath(it, Color.Black, style = Stroke(width = 8f)) }
                currentPath?.let { drawPath(it, Color.Black, style = Stroke(width = 8f)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { paths.clear() }) { Text("Clear") }
            
            Button(onClick = {
                // Phase 1: Simple Analysis
                // In a full build, we'd capture the actual Canvas pixels.
                // For now, we simulate the interpretation of the path count/complexity.
                val fakeBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val composition = engine.interpretImage(fakeBitmap)
                player.playComposition(composition)
            }) {
                Text("Hear")
            }
        }
    }
}
