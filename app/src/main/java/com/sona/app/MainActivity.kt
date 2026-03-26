package com.sona.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import com.sona.app.engine.SonaEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SonaApp()
        }
    }
}

@Composable
fun SonaApp() {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "SONA", 
            style = MaterialTheme.typography.headlineLarge, 
            modifier = Modifier.padding(16.dp)
        )
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                        },
                        onDrag = { change, _ ->
                            currentPath?.lineTo(change.position.x, change.position.y)
                            val p = currentPath
                            currentPath = null // Force recompose
                            currentPath = p
                        },
                        onDragEnd = {
                            currentPath?.let { paths.add(it) }
                            currentPath = null
                        }
                    )
                }
        ) {
            paths.forEach { drawPath(it, Color.Black, style = Stroke(width = 5f)) }
            currentPath?.let { drawPath(it, Color.Black, style = Stroke(width = 5f)) }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { paths.clear() }) { Text("Clear") }
            Button(onClick = { /* Trigger SonaEngine Logic here */ }) { Text("Hear") }
        }
    }
}
