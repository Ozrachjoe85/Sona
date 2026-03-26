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
    private val engine = SonaEngine()
    private val player = SonaAudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SonaApp(engine, player)
        }
    }
}

// Data class to store our "Rule-based" lines
data class SonaLine(
    val path: Path, 
    val color: Color, 
    val thickness: Float,
    val yOffset: Float // Used for pitch calculation
)

@Composable
fun SonaApp(engine: SonaEngine, player: SonaAudioPlayer) {
    val context = LocalContext.current
    val lines = remember { mutableStateListOf<SonaLine>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Cyan) }
    var currentThickness by remember { mutableStateOf(10f) }
    var startY by remember { mutableStateOf(0f) }
    
    // Image state for the Scanner
    var uploadedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            uploadedBitmap = bitmap
            // Rule: Scanner analyzes image and plays unique procedural sequence
            val notes = engine.analyzeBitmap(bitmap)
            player.playProcedural(notes)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        // --- Painter Tools ---
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val palette = listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.White)
            palette.forEach { color ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(if (currentColor == color) color else color.copy(alpha = 0.5f))
                        .clickable { currentColor = color }
                )
            }
            Slider(
                value = currentThickness,
                onValueChange = { currentThickness = it },
                valueRange = 2f..80f,
                modifier = Modifier.weight(1f)
            )
        }

        // --- Canvas Workspace ---
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color.Black)
        ) {
            // Layer 1: Scanned Image
            uploadedBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alpha = 0.6f
                )
            }

            // Layer 2: Drawing Canvas
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        startY = offset.y
                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
                    },
                    onDrag = { change, _ ->
                        currentPath?.lineTo(change.position.x, change.position.y)
                        // Forced UI Refresh
                        val p = currentPath; currentPath = null; currentPath = p
                    },
                    onDragEnd = {
                        currentPath?.let {
                            lines.add(SonaLine(it, currentColor, currentThickness, startY))
                        }
                        currentPath = null
                    }
                )
            }) {
                lines.forEach { line ->
                    drawPath(line.path, line.color, style = Stroke(line.thickness))
                }
                currentPath?.let { path ->
                    drawPath(path, currentColor, style = Stroke(currentThickness))
                }
            }
        }

        // --- Bottom Rule-Based Controls ---
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { 
                lines.clear()
                uploadedBitmap = null 
            }) { Text("Scrub") }

            Button(onClick = { launcher.launch("image/*") }) {
                Text("Scan Image")
            }

            Button(
                onClick = {
                    // RULE: Convert drawing properties to procedural math notes
                    val proceduralNotes = lines.map { line ->
                        engine.convertLineToNote(line)
                    }
                    player.playProcedural(proceduralNotes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black)
            ) {
                Text("Hear Drawing")
            }
        }
    }
}
