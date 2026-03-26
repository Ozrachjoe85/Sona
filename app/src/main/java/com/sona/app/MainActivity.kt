package com.sona.app

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sona.app.engine.*

class MainActivity : ComponentActivity() {
    private lateinit var engine: SonaEngine
    private lateinit var player: SonaAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = SonaEngine()
        player = SonaAudioPlayer(this)
        setContent { SonaApp(engine, player) }
    }
}

data class Line(val path: Path, val color: Color, val strokeWidth: Float)

@Composable
fun SonaApp(engine: SonaEngine, player: SonaAudioPlayer) {
    val context = LocalContext.current
    val lines = remember { mutableStateListOf<Line>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Red) }
    var currentThickness by remember { mutableStateOf(10f) }
    
    // This holds the uploaded image for display
    var uploadedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            uploadedBitmap = bitmap // Set it to show on screen
            player.play(engine.analyzeBitmap(bitmap))
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Color.Red, Color.Cyan, Color.Yellow, Color.White).forEach { color ->
                Box(Modifier.size(40.dp).padding(4.dp).clip(CircleShape).background(color).clickable { currentColor = color })
            }
            Slider(value = currentThickness, onValueChange = { currentThickness = it }, valueRange = 4f..60f, modifier = Modifier.weight(1f))
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray)) {
            // LAYER 1: The Image
            uploadedBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // LAYER 2: The Drawing
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentPath = Path().apply { moveTo(it.x, it.y) } },
                    onDrag = { change, _ -> 
                        currentPath?.lineTo(change.position.x, change.position.y)
                        val p = currentPath; currentPath = null; currentPath = p 
                    },
                    onDragEnd = { currentPath?.let { lines.add(Line(it, currentColor, currentThickness)) }; currentPath = null }
                )
            }) {
                lines.forEach { drawPath(it.path, it.color, style = Stroke(it.strokeWidth)) }
                currentPath?.let { drawPath(it, currentColor, style = Stroke(currentThickness)) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(20.dp), Arrangement.SpaceEvenly) {
            Button(onClick = { lines.clear(); uploadedBitmap = null }) { Text("Scrub") }
            Button(onClick = { launcher.launch("image/*") }) { Text("Scan") }
            Button(onClick = { player.play(engine.generateFromPaths(lines.size)) }) { Text("Hear") }
        }
    }
}
